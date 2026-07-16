package com.medfusion.ai.data.ai

import android.content.Context
import android.util.Base64
import com.medfusion.ai.BuildConfig
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.toAppError
import com.medfusion.ai.data.ai.dto.GeminiContent
import com.medfusion.ai.data.ai.dto.GeminiGenerationConfig
import com.medfusion.ai.data.ai.dto.GeminiInlineData
import com.medfusion.ai.data.ai.dto.GeminiPart
import com.medfusion.ai.data.ai.dto.GeminiRequest
import com.medfusion.ai.data.ai.dto.GeminiResponse
import com.medfusion.ai.data.ai.dto.PatientExplanationDto
import com.medfusion.ai.data.ai.dto.ProgressAnalysisDto
import com.medfusion.ai.data.ai.dto.SymptomAnalysisDto
import com.medfusion.ai.data.ai.dto.WellnessPlanDto
import com.medfusion.ai.data.ai.dto.toDomain
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.PatientContext
import com.medfusion.ai.domain.model.PatientExplanation
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.ReportAttachment
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.SymptomLocation
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single point of contact with Gemini. Requests structured JSON only, parses
 * it into typed domain models, and — in demo/mock builds — falls back to an
 * on-device analysis when the key is missing or the network fails, so the flow is
 * always demonstrable.
 */
@Singleton
class GeminiService @Inject constructor(
    private val geminiApi: GeminiApi,
    private val moshi: Moshi,
    private val mockProvider: MockSymptomAnalysisProvider,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AiService {

    override suspend fun analyzeSymptoms(
        symptoms: String,
        language: String,
        attachments: List<ReportAttachment>,
        patientContext: PatientContext?,
        locations: List<SymptomLocation>,
    ): Resource<SymptomAnalysis> = withContext(io) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            return@withContext if (allowMock()) {
                Resource.Success(mockProvider.analyze(symptoms, attachments.isNotEmpty(), patientContext, locations))
            } else {
                Resource.Error(AppError.Validation("AI is not configured. Add your Gemini API key to local.properties."))
            }
        }

        try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(role = "user", parts = buildParts(symptoms, language, attachments, patientContext, locations))),
                generationConfig = GeminiGenerationConfig(),
            )
            val response = geminiApi.generateContent(ENDPOINT, key, request)

            val json = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstNotNullOfOrNull { it.text }
                ?: return@withContext emptyResponse(response, symptoms, attachments, patientContext, locations)

            val dto = moshi.adapter(SymptomAnalysisDto::class.java).fromJson(sanitize(json))
                ?: return@withContext Resource.Error(
                    AppError.Server("The assistant returned an unreadable response. Please try again.")
                )
            Resource.Success(dto.toDomain())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            val error = t.toAppError()
            // Real Gemini is tried first (above). In Demo Mode we fall back on any
            // failure so the flow always works; in mock-fallback (non-demo) builds
            // we only fall back for connectivity-class errors, so real API errors
            // still surface to the developer.
            val recoverable = error is AppError.Network || error is AppError.Timeout || error is AppError.Server
            if (BuildConfig.DEMO_MODE || (BuildConfig.USE_MOCK_AI_FALLBACK && recoverable)) {
                Resource.Success(mockProvider.analyze(symptoms, attachments.isNotEmpty(), patientContext, locations))
            } else {
                Resource.Error(error)
            }
        }
    }

    override suspend fun analyzeProgress(
        logs: List<DailyLog>,
        diagnosis: String?,
        language: String,
    ): Resource<ProgressAnalysis> = withContext(io) {
        if (logs.isEmpty()) {
            return@withContext Resource.Success(
                ProgressAnalysis("Getting started", "Log a few daily check-ins to see your recovery trend.", false)
            )
        }
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            return@withContext if (allowMock()) Resource.Success(mockProvider.progress(logs))
            else Resource.Error(AppError.Validation("AI is not configured."))
        }
        try {
            val response = geminiApi.generateContent(ENDPOINT, key, textRequest(GeminiPrompt.buildProgress(logs, diagnosis, language)))
            val json = extractText(response)
                ?: return@withContext if (allowMock()) Resource.Success(mockProvider.progress(logs))
                else Resource.Error(AppError.Server())
            val dto = moshi.adapter(ProgressAnalysisDto::class.java).fromJson(sanitize(json))
                ?: return@withContext Resource.Error(AppError.Server())
            Resource.Success(dto.toDomain())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            if (shouldFallback(t)) Resource.Success(mockProvider.progress(logs))
            else Resource.Error(t.toAppError())
        }
    }

    override suspend fun generateWellnessPlan(concern: String, language: String): Resource<CarePlan> =
        withContext(io) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank()) {
                return@withContext if (allowMock()) Resource.Success(mockProvider.wellnessPlan())
                else Resource.Error(AppError.Validation("AI is not configured."))
            }
            try {
                val response = geminiApi.generateContent(ENDPOINT, key, textRequest(GeminiPrompt.buildWellness(concern, language)))
                val json = extractText(response)
                    ?: return@withContext if (allowMock()) Resource.Success(mockProvider.wellnessPlan())
                    else Resource.Error(AppError.Server())
                val dto = moshi.adapter(WellnessPlanDto::class.java).fromJson(sanitize(json))
                    ?: return@withContext Resource.Error(AppError.Server())
                Resource.Success(dto.toDomain())
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                if (shouldFallback(t)) Resource.Success(mockProvider.wellnessPlan())
                else Resource.Error(t.toAppError())
            }
        }

    override suspend fun explainForPatient(
        prescription: Prescription,
        carePlan: CarePlan?,
        language: String,
    ): Resource<PatientExplanation> = withContext(io) {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            return@withContext if (allowMock()) Resource.Success(mockProvider.patientExplanation(prescription))
            else Resource.Error(AppError.Validation("AI is not configured."))
        }
        try {
            val response = geminiApi.generateContent(
                ENDPOINT, key, textRequest(GeminiPrompt.buildExplain(prescription, carePlan, language))
            )
            val json = extractText(response)
                ?: return@withContext if (allowMock()) Resource.Success(mockProvider.patientExplanation(prescription))
                else Resource.Error(AppError.Server())
            val dto = moshi.adapter(PatientExplanationDto::class.java).fromJson(sanitize(json))
                ?: return@withContext Resource.Error(AppError.Server())
            Resource.Success(dto.toDomain())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            if (shouldFallback(t)) Resource.Success(mockProvider.patientExplanation(prescription))
            else Resource.Error(t.toAppError())
        }
    }

    private fun textRequest(prompt: String) = GeminiRequest(
        contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))),
        generationConfig = GeminiGenerationConfig(),
    )

    private fun extractText(response: GeminiResponse): String? =
        response.candidates?.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.text }

    private fun shouldFallback(t: Throwable): Boolean {
        val error = t.toAppError()
        val recoverable = error is AppError.Network || error is AppError.Timeout || error is AppError.Server
        return BuildConfig.DEMO_MODE || (BuildConfig.USE_MOCK_AI_FALLBACK && recoverable)
    }

    private fun buildParts(
        symptoms: String,
        language: String,
        attachments: List<ReportAttachment>,
        patientContext: PatientContext?,
        locations: List<SymptomLocation>,
    ): List<GeminiPart> {
        val parts = mutableListOf(
            GeminiPart(text = GeminiPrompt.build(symptoms, language, attachments.isNotEmpty(), patientContext, locations))
        )
        // Attach up to a few reports as inline base64; skip any we can't read.
        attachments.take(MAX_ATTACHMENTS).forEach { attachment ->
            val bytes = runCatching {
                context.contentResolver.openInputStream(attachment.uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null && bytes.size <= MAX_ATTACHMENT_BYTES) {
                parts += GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = attachment.mimeType.ifBlank { "application/octet-stream" },
                        data = Base64.encodeToString(bytes, Base64.NO_WRAP),
                    )
                )
            }
        }
        return parts
    }

    /** Handles blocked prompts and empty candidate lists. */
    private fun emptyResponse(
        response: GeminiResponse,
        symptoms: String,
        attachments: List<ReportAttachment>,
        patientContext: PatientContext?,
        locations: List<SymptomLocation>,
    ): Resource<SymptomAnalysis> {
        val blocked = response.promptFeedback?.blockReason
        return when {
            blocked != null -> Resource.Error(
                AppError.Validation("Your request couldn't be processed (reason: $blocked). Please rephrase your symptoms.")
            )
            allowMock() -> Resource.Success(mockProvider.analyze(symptoms, attachments.isNotEmpty(), patientContext, locations))
            else -> Resource.Error(AppError.Server("The assistant didn't return a result. Please try again."))
        }
    }

    /** Strips accidental markdown code fences the model might wrap JSON in. */
    private fun sanitize(raw: String): String {
        val trimmed = raw.trim()
        return trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun allowMock(): Boolean = BuildConfig.DEMO_MODE || BuildConfig.USE_MOCK_AI_FALLBACK

    private companion object {
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        const val MAX_ATTACHMENTS = 4
        const val MAX_ATTACHMENT_BYTES = 8 * 1024 * 1024 // 8 MB per file
    }
}
