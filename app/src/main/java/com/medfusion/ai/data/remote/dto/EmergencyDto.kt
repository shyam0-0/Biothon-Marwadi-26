package com.medfusion.ai.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for POST /emergency-alert. The backend wraps Twilio SMS to the
 * emergency contact and notifies the nearest hospital with the patient's location.
 */
@JsonClass(generateAdapter = true)
data class EmergencyAlertRequest(
    @Json(name = "patientId") val patientId: String,
    @Json(name = "heartRate") val heartRate: Int,
    @Json(name = "emergencyContact") val emergencyContact: String?,
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
)

@JsonClass(generateAdapter = true)
data class EmergencyAlertResponse(
    @Json(name = "hospitalName") val hospitalName: String?,
    @Json(name = "hospitalPhone") val hospitalPhone: String?,
    @Json(name = "contactNotified") val contactNotified: Boolean,
)
