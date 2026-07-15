package com.medfusion.ai.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.Appointments
import com.medfusion.ai.data.firebase.FirestoreSchema.Prescriptions
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.ConsultationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsultationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val careRepository: CareRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ConsultationRepository {

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
    ): Resource<Prescription> = withContext(io) {
        resourceOf {
            // 1) Write the prescription.
            val prescriptionRef = firestore.collection(Prescriptions.COLLECTION).document()
            val medsData = medications.map {
                mapOf("name" to it.name, "dosage" to it.dosage, "timing" to it.timing)
            }
            prescriptionRef.set(
                mapOf(
                    Prescriptions.APPOINTMENT_ID to appointmentId,
                    Prescriptions.PATIENT_ID to patientId,
                    Prescriptions.DOCTOR_ID to doctorId,
                    Prescriptions.DOCTOR_NAME to doctorName,
                    Prescriptions.DIAGNOSIS to diagnosis,
                    Prescriptions.MEDICATIONS to medsData,
                    Prescriptions.ADVICE to advice.trim(),
                    Prescriptions.FOLLOW_UP_DATE to followUpDate,
                    Prescriptions.CREATED_AT to FieldValue.serverTimestamp(),
                )
            ).await()

            // 2) Complete the appointment with the outcome.
            firestore.collection(Appointments.COLLECTION).document(appointmentId).update(
                mapOf(
                    Appointments.STATUS to AppointmentStatus.COMPLETED.wireValue,
                    Appointments.DOCTOR_NOTES to notes.trim(),
                    Appointments.DIAGNOSIS to diagnosis.trim(),
                    Appointments.PRESCRIPTION_ID to prescriptionRef.id,
                )
            ).await()

            // 3) Generate + save the care plan (Phase 3 renders it).
            careRepository.saveCarePlan(
                doctorCarePlan(patientId, doctorName, diagnosis, medications, advice, followUpDate)
            )

            Prescription(
                id = prescriptionRef.id,
                appointmentId = appointmentId,
                patientId = patientId,
                doctorId = doctorId,
                doctorName = doctorName,
                diagnosis = diagnosis.trim(),
                medications = medications,
                advice = advice.trim(),
                followUpDate = followUpDate,
            )
        }
    }

    override suspend fun getPrescriptionForAppointment(appointmentId: String): Resource<Prescription?> =
        withContext(io) {
            resourceOf {
                firestore.collection(Prescriptions.COLLECTION)
                    .whereEqualTo(Prescriptions.APPOINTMENT_ID, appointmentId)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.toPrescription()
            }
        }
}
