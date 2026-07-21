package com.medfusion.ai.domain.model

/**
 * One reading relayed by an IoT device (ESP32 today) through the FastAPI
 * backend into Firestore (Phase 7.3). Read-only from the Android app.
 */
data class LiveVitals(
    val heartRate: Int,
    val spo2: Int,
    val deviceId: String,
    val timestamp: String,
    val updatedAtMillis: Long?,
)
