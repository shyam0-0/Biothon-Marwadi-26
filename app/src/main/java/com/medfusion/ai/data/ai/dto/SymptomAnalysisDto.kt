package com.medfusion.ai.data.ai.dto

import com.medfusion.ai.domain.model.ConditionProbability
import com.medfusion.ai.domain.model.ReportInsights
import com.medfusion.ai.domain.model.Severity
import com.medfusion.ai.domain.model.SpecialistRecommendation
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.TestPriority
import com.medfusion.ai.domain.model.TestRecommendation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The exact JSON shape we require the model to return (enforced via
 * responseMimeType=application/json + an explicit contract in the prompt). We map
 * this to the domain [SymptomAnalysis]; the app never sees raw model text.
 */
@JsonClass(generateAdapter = true)
data class SymptomAnalysisDto(
    @Json(name = "summary") val summary: String?,
    @Json(name = "conditions") val conditions: List<ConditionDto>?,
    @Json(name = "severity") val severity: String?,
    @Json(name = "emergencyMessage") val emergencyMessage: String?,
    @Json(name = "recommendedSpecialists") val recommendedSpecialists: List<SpecialistDto>?,
    @Json(name = "recommendedTests") val recommendedTests: List<TestDto>?,
    @Json(name = "recommendedScans") val recommendedScans: List<String>?,
    @Json(name = "homeCare") val homeCare: List<String>?,
    @Json(name = "precautions") val precautions: List<String>?,
    @Json(name = "redFlags") val redFlags: List<String>?,
    @Json(name = "reportRecommendation") val reportRecommendation: String?,
    @Json(name = "consultationRecommended") val consultationRecommended: Boolean?,
    @Json(name = "confidenceExplanation") val confidenceExplanation: String?,
    @Json(name = "reportInsights") val reportInsights: ReportInsightsDto?,
)

@JsonClass(generateAdapter = true)
data class ConditionDto(
    @Json(name = "name") val name: String?,
    @Json(name = "confidence") val confidence: Double?,
    @Json(name = "reason") val reason: String?,
)

@JsonClass(generateAdapter = true)
data class SpecialistDto(
    @Json(name = "name") val name: String?,
    @Json(name = "reason") val reason: String?,
)

@JsonClass(generateAdapter = true)
data class TestDto(
    @Json(name = "name") val name: String?,
    @Json(name = "priority") val priority: String?,
    @Json(name = "reason") val reason: String?,
)

@JsonClass(generateAdapter = true)
data class ReportInsightsDto(
    @Json(name = "summary") val summary: String?,
    @Json(name = "abnormalValues") val abnormalValues: List<String>?,
    @Json(name = "concerns") val concerns: List<String>?,
    @Json(name = "relevance") val relevance: String?,
)

fun SymptomAnalysisDto.toDomain(): SymptomAnalysis = SymptomAnalysis(
    summary = summary?.trim().orEmpty(),
    conditions = conditions.orEmpty()
        .filter { !it.name.isNullOrBlank() }
        .map {
            ConditionProbability(
                name = it.name!!.trim(),
                confidence = (it.confidence ?: 0.0).toInt().coerceIn(0, 100),
                reason = it.reason?.trim().orEmpty(),
            )
        },
    severity = Severity.fromWire(severity),
    emergencyMessage = emergencyMessage?.trim()?.takeIf { it.isNotEmpty() },
    recommendedSpecialists = recommendedSpecialists.orEmpty()
        .filter { !it.name.isNullOrBlank() }
        .map { SpecialistRecommendation(it.name!!.trim(), it.reason?.trim().orEmpty()) },
    recommendedTests = recommendedTests.orEmpty()
        .filter { !it.name.isNullOrBlank() }
        .map {
            TestRecommendation(
                name = it.name!!.trim(),
                priority = TestPriority.fromWire(it.priority),
                reason = it.reason?.trim().orEmpty(),
            )
        },
    recommendedScans = recommendedScans.orEmpty().cleaned(),
    homeCare = homeCare.orEmpty().cleaned(),
    precautions = precautions.orEmpty().cleaned(),
    redFlags = redFlags.orEmpty().cleaned(),
    reportRecommendation = reportRecommendation?.trim()?.takeIf { it.isNotEmpty() },
    consultationRecommended = consultationRecommended ?: true,
    confidenceExplanation = confidenceExplanation?.trim()?.takeIf { it.isNotEmpty() },
    reportInsights = reportInsights?.let { dto ->
        dto.summary?.trim()?.takeIf { it.isNotEmpty() }?.let { summaryText ->
            ReportInsights(
                summary = summaryText,
                abnormalValues = dto.abnormalValues.orEmpty().cleaned(),
                concerns = dto.concerns.orEmpty().cleaned(),
                relevance = dto.relevance?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    },
)

private fun List<String>.cleaned(): List<String> =
    map { it.trim() }.filter { it.isNotEmpty() }
