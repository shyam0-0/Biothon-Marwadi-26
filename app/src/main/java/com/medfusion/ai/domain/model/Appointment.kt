package com.medfusion.ai.domain.model

/**
 * A booking between a patient and a doctor. [urgencyLevel] is carried over from
 * the triage case so the doctor's queue can be prioritized (Phase 7/8).
 */
data class Appointment(
    val id: String,
    val patientId: String,
    val patientName: String,
    val doctorId: String,
    val doctorName: String,
    val date: String,        // ISO yyyy-MM-dd
    val timeSlot: String,    // e.g. "10:00 AM"
    val message: String,
    val urgencyLevel: UrgencyLevel,
    val status: AppointmentStatus,
    val caseId: String? = null,
    val roomUrl: String? = null,
    val createdAtMillis: Long = 0L,
)

enum class AppointmentStatus(val wireValue: String, val label: String) {
    PENDING("pending", "Pending"),
    ACCEPTED("accepted", "Accepted"),
    RESCHEDULED("rescheduled", "Rescheduled"),
    DECLINED("declined", "Declined"),
    COMPLETED("completed", "Completed");

    companion object {
        fun fromWire(value: String?): AppointmentStatus =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) } ?: PENDING
    }
}

/** A single bookable slot offered by a doctor on a given date. */
data class AvailabilitySlot(
    val doctorId: String,
    val doctorName: String,
    val date: String,
    val timeSlot: String,
)
