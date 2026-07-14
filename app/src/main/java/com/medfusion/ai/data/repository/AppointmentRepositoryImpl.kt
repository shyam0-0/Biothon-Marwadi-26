package com.medfusion.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.medfusion.ai.BuildConfig
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.fail
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.core.util.toAppError
import com.medfusion.ai.data.firebase.FirestoreSchema.Appointments
import com.medfusion.ai.data.firebase.FirestoreSchema.DoctorAvailability
import com.medfusion.ai.data.firebase.FirestoreSchema.Users
import com.medfusion.ai.data.remote.MedFusionApi
import com.medfusion.ai.data.remote.dto.CreateRoomRequest
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.repository.AppointmentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val api: MedFusionApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AppointmentRepository {

    private fun appointments() = firestore.collection(Appointments.COLLECTION)

    override suspend fun getAvailability(date: String): Resource<List<AvailabilitySlot>> =
        withContext(io) {
            resourceOf {
                val snapshot = firestore.collection(DoctorAvailability.COLLECTION)
                    .whereEqualTo(DoctorAvailability.DATE, date)
                    .get()
                    .await()

                val slots = snapshot.documents.flatMap { doc ->
                    val doctorId = doc.getString(DoctorAvailability.DOCTOR_ID) ?: return@flatMap emptyList()
                    val doctorName = doc.getString(Appointments.DOCTOR_NAME) ?: "Doctor"
                    @Suppress("UNCHECKED_CAST")
                    val times = doc.get(DoctorAvailability.SLOTS) as? List<String> ?: emptyList()
                    times.map { AvailabilitySlot(doctorId, doctorName, date, it) }
                }

                // Demo builds: if no availability is configured, offer sensible
                // default slots so the booking flow can be shown end-to-end.
                if (slots.isEmpty() && BuildConfig.USE_MOCK_AI_FALLBACK) {
                    defaultDemoSlots(date)
                } else {
                    slots
                }
            }
        }

    override suspend fun bookAppointment(
        caseId: String?,
        doctorId: String,
        doctorName: String,
        date: String,
        timeSlot: String,
        message: String,
        urgency: UrgencyLevel,
    ): Resource<Appointment> = withContext(io) {
        resourceOf {
            val patientId = auth.currentUser?.uid ?: fail(AppError.Unauthorized())
            val patientName = firestore.collection(Users.COLLECTION).document(patientId)
                .get().await().getString(Users.FULL_NAME).orEmpty()

            val docRef = appointments().document()
            val data = mapOf(
                Appointments.PATIENT_ID to patientId,
                Appointments.PATIENT_NAME to patientName,
                Appointments.DOCTOR_ID to doctorId,
                Appointments.DOCTOR_NAME to doctorName,
                Appointments.DATE to date,
                Appointments.TIME_SLOT to timeSlot,
                Appointments.MESSAGE to message.trim(),
                Appointments.URGENCY_LEVEL to urgency.wireValue,
                Appointments.STATUS to AppointmentStatus.PENDING.wireValue,
                Appointments.CASE_ID to caseId,
                Appointments.CREATED_AT to FieldValue.serverTimestamp(),
            )
            docRef.set(data).await()

            Appointment(
                id = docRef.id,
                patientId = patientId,
                patientName = patientName,
                doctorId = doctorId,
                doctorName = doctorName,
                date = date,
                timeSlot = timeSlot,
                message = message.trim(),
                urgencyLevel = urgency,
                status = AppointmentStatus.PENDING,
                caseId = caseId,
            )
        }
    }

    override fun observeDoctorQueue(doctorId: String): Flow<List<Appointment>> = callbackFlow {
        // No server-side orderBy: avoids a composite index and dropped docs with
        // not-yet-resolved server timestamps. Ordering is applied client-side.
        val registration = appointments()
            .whereEqualTo(Appointments.DOCTOR_ID, doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Surface an empty list rather than crashing the collector.
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toAppointment() }
                    ?.sortedForQueue()
                    .orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateStatus(
        appointmentId: String,
        status: AppointmentStatus,
        newDate: String?,
        newTimeSlot: String?,
    ): Resource<Unit> = withContext(io) {
        resourceOf {
            val updates = buildMap<String, Any> {
                put(Appointments.STATUS, status.wireValue)
                newDate?.let { put(Appointments.DATE, it) }
                newTimeSlot?.let { put(Appointments.TIME_SLOT, it) }
            }
            appointments().document(appointmentId).update(updates).await()
            Unit
        }
    }

    override fun observePatientAppointments(patientId: String): Flow<List<Appointment>> = callbackFlow {
        val registration = appointments()
            .whereEqualTo(Appointments.PATIENT_ID, patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toAppointment() }
                    ?.sortedByDescending { it.createdAtMillis }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getOrCreateRoom(appointmentId: String): Resource<String> =
        withContext(io) {
            resourceOf {
                val docRef = appointments().document(appointmentId)
                val existing = docRef.get().await().getString(Appointments.ROOM_URL)
                if (!existing.isNullOrBlank()) return@resourceOf existing

                val url = try {
                    api.createRoom(CreateRoomRequest(appointmentId)).roomUrl
                } catch (t: Throwable) {
                    val error = t.toAppError()
                    val recoverable = error is AppError.Network || error is AppError.Timeout || error is AppError.Server
                    if (BuildConfig.USE_MOCK_AI_FALLBACK && recoverable) {
                        // Demo fallback: a deterministic room name per appointment.
                        "$DEMO_ROOM_BASE/medfusion-${appointmentId.take(8)}"
                    } else throw t
                }
                docRef.update(Appointments.ROOM_URL, url).await()
                url
            }
        }

    private fun defaultDemoSlots(date: String): List<AvailabilitySlot> {
        val demoDoctorId = "demo-doctor"
        val demoDoctorName = "Dr. A. Sharma"
        return listOf("09:00 AM", "10:30 AM", "12:00 PM", "03:00 PM", "04:30 PM")
            .map { AvailabilitySlot(demoDoctorId, demoDoctorName, date, it) }
    }

    private companion object {
        // Replace with your Daily.co subdomain in production; only used as a
        // deterministic demo room when the backend /create-room is unavailable.
        const val DEMO_ROOM_BASE = "https://medfusion.daily.co"
    }
}

