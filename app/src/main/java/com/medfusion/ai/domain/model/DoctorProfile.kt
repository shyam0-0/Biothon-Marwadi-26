package com.medfusion.ai.domain.model

/**
 * Lightweight professional doctor profile (Phase 6.5). NOT authentication —
 * purely the public professional card a patient can review before booking.
 * Only professional information; no personal data.
 */
data class DoctorProfile(
    val doctorId: String,
    val fullName: String = "",
    val photoUri: String = "",
    val specialty: String = "",
    val qualifications: String = "",
    val yearsExperience: Int = 0,
    val hospital: String = "",
    val languagesSpoken: String = "",
    val biography: String = "",
    val availability: String = "",
    val licenseNumber: String = "",   // optional
) {
    val isComplete: Boolean
        get() = fullName.isNotBlank() && specialty.isNotBlank()
}
