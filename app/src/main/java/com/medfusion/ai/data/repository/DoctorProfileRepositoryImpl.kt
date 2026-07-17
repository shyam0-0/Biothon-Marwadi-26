package com.medfusion.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.DoctorProfile
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed doctor profile store. Deliberately dependency-free
 * (no Room / DataStore) per Phase 6.5's "no unnecessary dependencies" rule; a
 * Firestore-backed implementation can replace this behind the same interface
 * when backend integration begins. Works identically in Demo Mode.
 */
@Singleton
class DoctorProfileRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : DoctorProfileRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("doctor_profiles", Context.MODE_PRIVATE)

    /** Bumped on every save so observers re-read the prefs. */
    private val version = MutableStateFlow(0)

    override fun observeProfile(doctorId: String): Flow<DoctorProfile?> =
        version.map { read(doctorId) }

    override suspend fun getProfile(doctorId: String): Resource<DoctorProfile?> =
        Resource.Success(read(doctorId))

    override suspend fun saveProfile(profile: DoctorProfile): Resource<Unit> {
        val id = profile.doctorId
        prefs.edit()
            .putString("$id.fullName", profile.fullName)
            .putString("$id.photoUri", profile.photoUri)
            .putString("$id.specialty", profile.specialty)
            .putString("$id.qualifications", profile.qualifications)
            .putInt("$id.yearsExperience", profile.yearsExperience)
            .putString("$id.hospital", profile.hospital)
            .putString("$id.languagesSpoken", profile.languagesSpoken)
            .putString("$id.biography", profile.biography)
            .putString("$id.availability", profile.availability)
            .putString("$id.licenseNumber", profile.licenseNumber)
            .apply()
        version.value++
        return Resource.Success(Unit)
    }

    private fun read(doctorId: String): DoctorProfile? {
        val name = prefs.getString("$doctorId.fullName", null) ?: return null
        return DoctorProfile(
            doctorId = doctorId,
            fullName = name,
            photoUri = prefs.getString("$doctorId.photoUri", "") ?: "",
            specialty = prefs.getString("$doctorId.specialty", "") ?: "",
            qualifications = prefs.getString("$doctorId.qualifications", "") ?: "",
            yearsExperience = prefs.getInt("$doctorId.yearsExperience", 0),
            hospital = prefs.getString("$doctorId.hospital", "") ?: "",
            languagesSpoken = prefs.getString("$doctorId.languagesSpoken", "") ?: "",
            biography = prefs.getString("$doctorId.biography", "") ?: "",
            availability = prefs.getString("$doctorId.availability", "") ?: "",
            licenseNumber = prefs.getString("$doctorId.licenseNumber", "") ?: "",
        )
    }
}
