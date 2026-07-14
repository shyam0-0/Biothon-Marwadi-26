package com.medfusion.ai.domain.model

/** What the patient (or the countdown) decided about a heart-rate anomaly. */
enum class EmergencyAction(val wireValue: String) {
    CONFIRMED("confirmed"),
    DISMISSED("dismissed"),
    AUTO_ESCALATED("auto_escalated"),
}

/** A hospital that can be alerted, with a contact number. */
data class Hospital(
    val name: String,
    val phone: String,
    val distanceKm: Double? = null,
)

/** Result of an escalation: which hospital was alerted and whether SMS was sent. */
data class EmergencyOutcome(
    val hospital: Hospital?,
    val contactNotified: Boolean,
)
