package com.medfusion.ai.domain.model

/**
 * A digital prescription issued by a doctor after a consultation (Phase 2).
 * Reuses [Medication]. Stored in the "prescriptions" collection and shown to the
 * patient; also seeds the generated care plan (Phase 3).
 */
data class Prescription(
    val id: String,
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val doctorName: String,
    val diagnosis: String,
    val medications: List<Medication>,
    val advice: String,
    val followUpDate: String? = null,   // ISO yyyy-MM-dd
    val createdAtMillis: Long = 0L,
)
