package com.medfusion.ai.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.Doctors
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.DoctorProfile
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed doctor professional profile (Phase 7.1). Reuses the same
 * "doctors" directory collection that the booking flow already reads
 * ([AppointmentRepositoryImpl.getDoctorsBySpecialty]), keyed by doctorId (the
 * doctor's auth uid), so a profile save also keeps the patient-facing booking
 * directory current — one document, one source of truth.
 */
@Singleton
class DoctorProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DoctorProfileRepository {

    private fun doc(doctorId: String) =
        firestore.collection(Doctors.COLLECTION).document(doctorId)

    override fun observeProfile(doctorId: String): Flow<DoctorProfile?> = callbackFlow {
        val registration = doc(doctorId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Surface "no profile" rather than crashing the collector.
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toDoctorProfile(doctorId))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getProfile(doctorId: String): Resource<DoctorProfile?> =
        withContext(io) {
            resourceOf { doc(doctorId).get().await().toDoctorProfile(doctorId) }
        }

    override suspend fun saveProfile(profile: DoctorProfile): Resource<Unit> =
        withContext(io) {
            resourceOf {
                val data = mapOf(
                    Doctors.NAME to profile.fullName,
                    Doctors.SPECIALTY to profile.specialty,
                    Doctors.YEARS_EXPERIENCE to profile.yearsExperience,
                    Doctors.QUALIFICATION to profile.qualifications,
                    Doctors.PHOTO_URL to profile.photoUri,
                    Doctors.HOSPITAL to profile.hospital,
                    Doctors.LANGUAGES_SPOKEN to profile.languagesSpoken,
                    Doctors.BIOGRAPHY to profile.biography,
                    Doctors.AVAILABILITY_TEXT to profile.availability,
                    Doctors.LICENSE_NUMBER to profile.licenseNumber,
                )
                // Merge: preserves fields this profile doesn't own (e.g. RATING) and
                // any directory doc that already existed for the booking list.
                doc(profile.doctorId).set(data, SetOptions.merge()).await()
                Unit
            }
        }
}

private fun DocumentSnapshot.toDoctorProfile(doctorId: String): DoctorProfile? {
    if (!exists()) return null
    val name = getString(Doctors.NAME) ?: return null
    return DoctorProfile(
        doctorId = doctorId,
        fullName = name,
        photoUri = getString(Doctors.PHOTO_URL).orEmpty(),
        specialty = getString(Doctors.SPECIALTY).orEmpty(),
        qualifications = getString(Doctors.QUALIFICATION).orEmpty(),
        yearsExperience = (getLong(Doctors.YEARS_EXPERIENCE) ?: 0).toInt(),
        hospital = getString(Doctors.HOSPITAL).orEmpty(),
        languagesSpoken = getString(Doctors.LANGUAGES_SPOKEN).orEmpty(),
        biography = getString(Doctors.BIOGRAPHY).orEmpty(),
        availability = getString(Doctors.AVAILABILITY_TEXT).orEmpty(),
        licenseNumber = getString(Doctors.LICENSE_NUMBER).orEmpty(),
    )
}
