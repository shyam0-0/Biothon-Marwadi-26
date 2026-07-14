package com.medfusion.ai.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for POST /triage. */
@JsonClass(generateAdapter = true)
data class TriageRequest(
    @Json(name = "symptomsText") val symptomsText: String,
    @Json(name = "language") val language: String = "en",
)

/** Response body for POST /triage. */
@JsonClass(generateAdapter = true)
data class TriageResponse(
    @Json(name = "caseId") val caseId: String?,
    @Json(name = "recommendedTest") val recommendedTest: String,
    @Json(name = "urgencyLevel") val urgencyLevel: String,
)
