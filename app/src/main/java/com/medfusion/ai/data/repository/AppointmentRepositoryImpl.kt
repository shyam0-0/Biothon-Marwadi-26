package com.medfusion.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
import com.medfusion.ai.data.firebase.FirestoreSchema.Doctors
import com.medfusion.ai.data.firebase.FirestoreSchema.Users
import com.medfusion.ai.data.remote.MedFusionApi
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.domain.model.Doctor
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

                slots
            }
        }

    override suspend fun getDoctorsBySpecialty(specialty: String): Resource<List<Doctor>> =
        withContext(io) {
            resourceOf {
                val exact = firestore.collection(Doctors.COLLECTION)
                    .whereEqualTo(Doctors.SPECIALTY, specialty)
                    .get()
                    .await()
                    .documents
                    .toDoctors()

                if (exact.isNotEmpty()) {
                    return@resourceOf exact
                }

                // The AI's recommended specialist is free text (e.g. "ENT
                // Specialist", "General Practitioner") and often won't match the
                // directory's specialty field byte-for-byte. Scan the full
                // directory (a handful of documents) and compare loosely instead
                // of returning nothing when only the wording differs.
                firestore.collection(Doctors.COLLECTION)
                    .get()
                    .await()
                    .documents
                    .toDoctors()
                    .filter { specialtyMatches(it.specialty, specialty) }
            }
        }

    override suspend fun getDoctorAvailability(doctorId: String, date: String): Resource<List<AvailabilitySlot>> =
        withContext(io) {
            resourceOf {
                val snapshot = firestore.collection(DoctorAvailability.COLLECTION)
                    .whereEqualTo(DoctorAvailability.DOCTOR_ID, doctorId)
                    .whereEqualTo(DoctorAvailability.DATE, date)
                    .get()
                    .await()
                val slots = snapshot.documents.flatMap { doc ->
                    val doctorName = doc.getString(Appointments.DOCTOR_NAME) ?: "Doctor"
                    @Suppress("UNCHECKED_CAST")
                    val times = doc.get(DoctorAvailability.SLOTS) as? List<String> ?: emptyList()
                    times.map { AvailabilitySlot(doctorId, doctorName, date, it) }
                }
                if (slots.isEmpty() && BuildConfig.USE_MOCK_AI_FALLBACK) {
                    DEFAULT_SLOTS.map { AvailabilitySlot(doctorId, "Doctor", date, it) }
                } else {
                    slots
                }
            }
        }

    override suspend fun getAppointment(appointmentId: String): Resource<Appointment> =
        withContext(io) {
            resourceOf {
                val snap = appointments().document(appointmentId).get().await()
                snap.toAppointment() ?: fail(AppError.NotFound("Appointment not found."))
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
        specialty: String?,
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
                Appointments.SPECIALTY to specialty,
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
                specialty = specialty,
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
                // Older appointments may still carry a dead Daily.co URL — regenerate those.
                if (!existing.isNullOrBlank() && existing.startsWith(JITSI_ROOM_BASE)) {
                    return@resourceOf existing
                }

                // Jitsi Meet rooms exist automatically when opened — no server-side
                // room creation needed. Deterministic per appointment, so the patient
                // and the doctor always land in the same room.
                val url = "$JITSI_ROOM_BASE/MedFusion-$appointmentId"
                docRef.update(Appointments.ROOM_URL, url).await()
                url
            }
        }

    private fun List<DocumentSnapshot>.toDoctors(): List<Doctor> = mapNotNull { doc ->
        val name = doc.getString(Doctors.NAME) ?: return@mapNotNull null
        Doctor(
            id = doc.id,
            name = name,
            specialty = doc.getString(Doctors.SPECIALTY).orEmpty(),
            yearsExperience = (doc.getLong(Doctors.YEARS_EXPERIENCE) ?: 0).toInt(),
            rating = doc.getDouble(Doctors.RATING) ?: 0.0,
            qualification = doc.getString(Doctors.QUALIFICATION).orEmpty(),
        )
    }

    /**
     * Loose specialty comparison so a free-text AI recommendation (e.g. "ENT
     * Specialist", "General Practitioner", "Orthopaedic") still matches the
     * directory's canonical specialty categories. Only normalizes specialty
     * wording — never a doctor identity.
     */
    private fun specialtyMatches(directoryValue: String, requested: String): Boolean {
        fun normalize(value: String) = value.trim().lowercase()
            .removeSuffix("specialist").removeSuffix("doctor").removeSuffix("consultant").trim()

        // "-ology" / "-ologist" cover most specialty pairs an AI might phrase
        // either way (Cardiology/Cardiologist, Neurology/Neurologist, ...).
        fun suffixForms(value: String): Set<String> = buildSet {
            add(value)
            if (value.endsWith("ologist")) add(value.removeSuffix("ologist") + "ology")
            if (value.endsWith("ology")) add(value.removeSuffix("ology") + "ologist")
        }

        val a = normalize(directoryValue)
        val b = normalize(requested)
        if (a.isEmpty() || b.isEmpty()) return false
        if (suffixForms(a).intersect(suffixForms(b)).isNotEmpty()) return true

        val synonyms = mapOf(
            "general physician" to setOf("general practitioner", "family medicine", "family physician", "gp"),
            "orthopedic" to setOf("orthopedist", "orthopaedic", "orthopaedics", "orthopedics"),
            "ent" to setOf("otolaryngologist", "otolaryngology"),
        )
        return synonyms[a]?.contains(b) == true || synonyms[b]?.contains(a) == true
    }

    private companion object {
        // Jitsi Meet: free, no backend, rooms auto-created on first join. Swap the
        // VideoProvider binding (and this base) to move to another provider later.
        const val JITSI_ROOM_BASE = "https://meet.jit.si"
        val DEFAULT_SLOTS = listOf("09:00 AM", "10:30 AM", "12:00 PM", "03:00 PM", "04:30 PM")
    }
}

