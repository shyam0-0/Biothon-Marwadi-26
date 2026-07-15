package com.medfusion.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medfusion.ai.BuildConfig
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.fail
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.core.util.toAppError
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.data.firebase.FirestoreSchema.Cases
import com.medfusion.ai.data.remote.MedFusionApi
import com.medfusion.ai.data.remote.MockAiEngine
import com.medfusion.ai.data.remote.dto.AnalyzeRequest
import com.medfusion.ai.data.remote.dto.AnalyzeResponse
import com.medfusion.ai.data.remote.dto.TriageRequest
import com.medfusion.ai.data.remote.dto.TriageResponse
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.CaseStatus
import com.medfusion.ai.domain.model.ConfidenceLevel
import com.medfusion.ai.domain.model.FusionResult
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.repository.CaseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaseRepositoryImpl @Inject constructor(
    private val api: MedFusionApi,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val mockAi: MockAiEngine,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CaseRepository {

    private fun casesCollection() = firestore.collection(Cases.COLLECTION)

    override suspend fun runTriage(symptomsText: String, language: String): Resource<Case> =
        withContext(io) {
            resourceOf {
                val userId = auth.currentUser?.uid ?: fail(AppError.Unauthorized())

                // Pre-generate the document id so it is the caseId even when the
                // backend doesn't echo one back (or the mock fallback is used).
                val docRef = casesCollection().document()
                val caseId = docRef.id

                // Resolve the app's current language so AI text comes back localized.
                val lang = language.ifBlank { LocaleManager.current() }
                val response = triageWithFallback(symptomsText, caseId, lang)
                val urgency = UrgencyLevel.fromWire(response.urgencyLevel)

                val data = mapOf(
                    Cases.CASE_ID to caseId,
                    Cases.USER_ID to userId,
                    Cases.SYMPTOMS_TEXT to symptomsText.trim(),
                    Cases.RECOMMENDED_TEST to response.recommendedTest,
                    Cases.URGENCY_LEVEL to urgency.wireValue,
                    Cases.STATUS to CaseStatus.AWAITING_TEST.wireValue,
                    Cases.CREATED_AT to FieldValue.serverTimestamp(),
                    Cases.UPDATED_AT to FieldValue.serverTimestamp(),
                )
                docRef.set(data).await()

                Case(
                    caseId = caseId,
                    userId = userId,
                    symptomsText = symptomsText.trim(),
                    recommendedTest = response.recommendedTest,
                    urgencyLevel = urgency,
                    status = CaseStatus.AWAITING_TEST,
                )
            }
        }

    override suspend fun getCase(caseId: String): Resource<Case> = withContext(io) {
        resourceOf {
            val snap = casesCollection().document(caseId).get().await()
            snap.toCase() ?: fail(AppError.NotFound("This case could not be found."))
        }
    }

    override suspend fun attachReports(
        caseId: String,
        xrayUrl: String?,
        labReportUrl: String?,
    ): Resource<Case> = withContext(io) {
        resourceOf {
            if (xrayUrl == null && labReportUrl == null) {
                fail(AppError.Validation("Please add at least one report before continuing."))
            }
            val docRef = casesCollection().document(caseId)
            // Build a partial update so we only touch the fields we have.
            val updates = buildMap<String, Any> {
                xrayUrl?.let { put(Cases.XRAY_URL, it) }
                labReportUrl?.let { put(Cases.LAB_REPORT_URL, it) }
                put(Cases.STATUS, CaseStatus.READY_FOR_ANALYSIS.wireValue)
                put(Cases.UPDATED_AT, FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()

            val snap = docRef.get().await()
            snap.toCase() ?: fail(AppError.NotFound("This case could not be found."))
        }
    }

    override suspend fun runAnalysis(caseId: String, language: String): Resource<Case> =
        withContext(io) {
            resourceOf {
                val docRef = casesCollection().document(caseId)
                val case = docRef.get().await().toCase()
                    ?: fail(AppError.NotFound("This case could not be found."))

                val lang = language.ifBlank { LocaleManager.current() }
                val response = analyzeWithFallback(case, lang)
                // Prefer an explicit score; otherwise derive one from the level.
                val level = ConfidenceLevel.fromWire(response.confidenceLevel)
                val score = response.confidenceScore ?: when (level) {
                    ConfidenceLevel.HIGH -> 0.85
                    ConfidenceLevel.MODERATE -> 0.6
                    ConfidenceLevel.LOW -> 0.35
                }
                val fusion = FusionResult(
                    findings = response.findings,
                    confidenceScore = score,
                    riskScore = response.riskScore,
                    confidenceLevel = level,
                )

                docRef.update(
                    mapOf(
                        Cases.FUSION_RESULT to fusion.toMap(),
                        Cases.STATUS to CaseStatus.ANALYZED.wireValue,
                        Cases.UPDATED_AT to FieldValue.serverTimestamp(),
                    )
                ).await()

                case.copy(fusionResult = fusion, status = CaseStatus.ANALYZED)
            }
        }

    override suspend fun createCaseFromAnalysis(
        symptoms: String,
        analysis: SymptomAnalysis,
    ): Resource<Case> = withContext(io) {
        resourceOf {
            val userId = auth.currentUser?.uid ?: fail(AppError.Unauthorized())
            val docRef = casesCollection().document()
            val caseId = docRef.id
            val urgency = analysis.severity.toUrgency()
            val recommendedTest = analysis.recommendedScans.firstOrNull()
                ?: analysis.recommendedTests.firstOrNull()
                ?: "Doctor consultation"
            val topConfidence = analysis.conditions.maxOfOrNull { it.confidence } ?: 0
            val score = topConfidence / 100.0
            val fusion = FusionResult(
                findings = analysis.summary,
                confidenceScore = score,
                riskScore = when (urgency) {
                    UrgencyLevel.RED -> 0.8
                    UrgencyLevel.YELLOW -> 0.5
                    UrgencyLevel.GREEN -> 0.2
                },
                confidenceLevel = ConfidenceLevel.fromScore(score),
            )
            val data = mapOf(
                Cases.CASE_ID to caseId,
                Cases.USER_ID to userId,
                Cases.SYMPTOMS_TEXT to symptoms.trim(),
                Cases.RECOMMENDED_TEST to recommendedTest,
                Cases.URGENCY_LEVEL to urgency.wireValue,
                Cases.STATUS to CaseStatus.ANALYZED.wireValue,
                Cases.FUSION_RESULT to fusion.toMap(),
                Cases.CREATED_AT to FieldValue.serverTimestamp(),
                Cases.UPDATED_AT to FieldValue.serverTimestamp(),
            )
            docRef.set(data).await()

            Case(
                caseId = caseId,
                userId = userId,
                symptomsText = symptoms.trim(),
                recommendedTest = recommendedTest,
                urgencyLevel = urgency,
                status = CaseStatus.ANALYZED,
                fusionResult = fusion,
            )
        }
    }

    /**
     * Calls the /triage endpoint; on a connectivity-class failure in a build with
     * [BuildConfig.USE_MOCK_AI_FALLBACK] enabled, falls back to the on-device
     * mock so the journey stays demoable. Non-connectivity errors always propagate.
     */
    private suspend fun triageWithFallback(
        symptomsText: String,
        caseId: String,
        language: String,
    ): TriageResponse = try {
        api.triage(TriageRequest(symptomsText = symptomsText.trim(), language = language))
    } catch (t: Throwable) {
        val error = t.toAppError()
        val recoverable = error is AppError.Network || error is AppError.Timeout || error is AppError.Server
        if (BuildConfig.USE_MOCK_AI_FALLBACK && recoverable) {
            mockAi.triage(symptomsText, caseId)
        } else {
            throw t
        }
    }

    /** /analyze with the same debug mock fallback policy as [triageWithFallback]. */
    private suspend fun analyzeWithFallback(case: Case, language: String): AnalyzeResponse = try {
        api.analyze(
            AnalyzeRequest(
                caseId = case.caseId,
                xrayUrl = case.xrayUrl,
                labReportUrl = case.labReportUrl,
                symptomsText = case.symptomsText,
                language = language,
            )
        )
    } catch (t: Throwable) {
        val error = t.toAppError()
        val recoverable = error is AppError.Network || error is AppError.Timeout || error is AppError.Server
        if (BuildConfig.USE_MOCK_AI_FALLBACK && recoverable) {
            val mock = mockAi.analyze(
                symptomsText = case.symptomsText,
                hasXray = case.xrayUrl != null,
                hasLabReport = case.labReportUrl != null,
            )
            AnalyzeResponse(
                findings = mock.findings,
                confidenceLevel = ConfidenceLevel.fromScore(mock.confidenceScore).wireValue,
                confidenceScore = mock.confidenceScore,
                riskScore = mock.riskScore,
            )
        } else {
            throw t
        }
    }
}
