package com.medfusion.ai.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for POST /generate-pdf. */
@JsonClass(generateAdapter = true)
data class GeneratePdfRequest(
    @Json(name = "caseId") val caseId: String,
)

/** Response body for POST /generate-pdf — a downloadable Firebase Storage URL. */
@JsonClass(generateAdapter = true)
data class GeneratePdfResponse(
    @Json(name = "pdfUrl") val pdfUrl: String,
)
