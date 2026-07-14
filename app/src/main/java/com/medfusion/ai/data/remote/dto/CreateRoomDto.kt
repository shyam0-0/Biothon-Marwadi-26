package com.medfusion.ai.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for POST /create-room. */
@JsonClass(generateAdapter = true)
data class CreateRoomRequest(
    @Json(name = "appointmentId") val appointmentId: String,
)

/** Response body for POST /create-room — a Daily.co (or Twilio) room URL. */
@JsonClass(generateAdapter = true)
data class CreateRoomResponse(
    @Json(name = "roomUrl") val roomUrl: String,
)
