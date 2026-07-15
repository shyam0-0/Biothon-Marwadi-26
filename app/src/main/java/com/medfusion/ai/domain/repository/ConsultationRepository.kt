package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Prescription

/**
 * Doctor-side consultation outcomes (Phase 2): notes, a digital prescription, and
 * approval — which completes the appointment and generates the patient's care plan.
 */
interface ConsultationRepository {

    /**
     * Records the consultation result in one step: writes the prescription, marks
     * the appointment COMPLETED with the notes/diagnosis, and generates + saves a
     * care plan for the patient.
     */
    suspend fun completeConsultation(
        appointmentId: String,
        patientId: String,
        doctorId: String,
        doctorName: String,
        notes: String,
        diagnosis: String,
        medications: List<Medication>,
        advice: String,
        followUpDate: String?,
    ): Resource<Prescription>

    /** The prescription for an appointment, if one was issued. */
    suspend fun getPrescriptionForAppointment(appointmentId: String): Resource<Prescription?>
}
