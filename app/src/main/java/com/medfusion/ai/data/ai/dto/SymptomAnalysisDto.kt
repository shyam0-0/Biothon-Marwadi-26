package com.medfusion.ai.data.ai.dto

import com.medfusion.ai.domain.model.ConditionProbability
import com.medfusion.ai.domain.model.Severity
import com.medfusion.ai.domain.model.SymptomAnalysis
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
    @Json(name = "recommendedSpecialists") val recommendedSpecialists: List<String>?,
    @Json(name = "recommendedTests") val recommendedTests: List<String>?,
    @Json(name = "recommendedScans") val recommendedScans: List<String>?,
    @Json(name = "homeCare") val homeCare: List<String>?,
    @Json(name = "precautions") val precautions: List<String>?,
    @Json(name = "redFlags") val redFlags: List<String>?,
    @Json(name = "reportRecommendation") val reportRecommendation: String?,
    @Json(name = "consultationRecommended") val consultationRecommended: Boolean?,
)

@JsonClass(generateAdapter = true)
data class ConditionDto(
    @Json(name = "name") val name: String?,
    @Json(name = "confidence") val confidence: Double?,
)

fun SymptomAnalysisDto.toDomain(): SymptomAnalysis = SymptomAnalysis(
    summary = summary?.trim().orEmpty(),
    conditions = conditions.orEmpty()
        .filter { !it.name.isNullOrBlank() }
        .map { ConditionProbability(it.name!!.trim(), (it.confidence ?: 0.0).toInt().coerceIn(0, 100)) },
    severity = Severity.fromWire(severity),
    emergencyMessage = emergencyMessage?.trim()?.takeIf { it.isNotEmpty() },
    recommendedSpecialists = recommendedSpecialists.orEmpty().cleaned(),
    recommendedTests = recommendedTests.orEmpty().cleaned(),
    recommendedScans = recommendedScans.orEmpty().cleaned(),
    homeCare = homeCare.orEmpty().cleaned(),
    precautions = precautions.orEmpty().cleaned(),
    redFlags = redFlags.orEmpty().cleaned(),
    reportRecommendation = reportRecommendation?.trim()?.takeIf { it.isNotEmpty() },
    consultationRecommended = consultationRecommended ?: true,
)

private fun List<String>.cleaned(): List<String> =
    map { it.trim() }.filter { it.isNotEmpty() }
