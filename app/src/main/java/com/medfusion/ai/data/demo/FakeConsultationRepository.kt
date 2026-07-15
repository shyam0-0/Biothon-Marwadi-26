package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.data.repository.doctorCarePlan
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.ConsultationRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory consultation outcomes for Demo Mode. */
@Singleton
class FakeConsultationRepository @Inject constructor(
    private val appointments: FakeAppointmentRepository,
    private val careRepository: CareRepository,
) : ConsultationRepository {

    private val byAppointment = ConcurrentHashMap<String, Prescription>()

    override suspend fun completeConsultation(
        appointmentId: String,
        patientId: String,
        doctorId: String,
        doctorName: String,
        notes: String,
        diagnosis: String,
        medications: List<Medication>,
        advice: String,
        followUpDate: String?,
    ): Resource<Prescription> {
        val prescription = Prescription(
            id = UUID.randomUUID().toString(),
            appointmentId = appointmentId,
            patientId = patientId,
            doctorId = doctorId,
            doctorName = doctorName,
            diagnosis = diagnosis.trim(),
            medications = medications,
            advice = advice.trim(),
            followUpDate = followUpDate,
            createdAtMillis = System.currentTimeMillis(),
        )
        byAppointment[appointmentId] = prescription

        appointments.updateAppointment(appointmentId) {
            it.copy(
                status = AppointmentStatus.COMPLETED,
                doctorNotes = notes.trim(),
                diagnosis = diagnosis.trim(),
                prescriptionId = prescription.id,
            )
        }

        careRepository.saveCarePlan(
            doctorCarePlan(patientId, doctorName, diagnosis, medications, advice, followUpDate)
        )
        return Resource.Success(prescription)
    }

    override suspend fun getPrescriptionForAppointment(appointmentId: String): Resource<Prescription?> =
        Resource.Success(byAppointment[appointmentId])
}
