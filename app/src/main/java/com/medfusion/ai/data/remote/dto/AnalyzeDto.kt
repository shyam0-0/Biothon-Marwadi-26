package com.medfusion.ai.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for POST /analyze. */
@JsonClass(generateAdapter = true)
data class AnalyzeRequest(
    @Json(name = "caseId") val caseId: String,
    @Json(name = "xrayUrl") val xrayUrl: String?,
    @Json(name = "labReportUrl") val labReportUrl: String?,
    @Json(name = "symptomsText") val symptomsText: String,
    @Json(name = "language") val language: String = "en",
)

/**
 * Response body for POST /analyze. [confidenceLevel] is the friendly band; some
 * backends also return a numeric [confidenceScore] — both are supported.
 */
@JsonClass(generateAdapter = true)
data class AnalyzeResponse(
    @Json(name = "findings") val findings: String,
    @Json(name = "confidenceLevel") val confidenceLevel: String?,
    @Json(name = "confidenceScore") val confidenceScore: Double?,
    @Json(name = "riskScore") val riskScore: Double,
)
