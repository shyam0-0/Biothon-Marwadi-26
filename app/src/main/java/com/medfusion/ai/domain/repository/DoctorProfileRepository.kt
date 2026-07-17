package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.DoctorProfile
import kotlinx.coroutines.flow.Flow

/** Professional doctor profile storage (Phase 6.5) — editable by the doctor, viewable by patients. */
interface DoctorProfileRepository {

    /** Live profile for a doctor, or null when none has been created yet. */
    fun observeProfile(doctorId: String): Flow<DoctorProfile?>

    suspend fun getProfile(doctorId: String): Resource<DoctorProfile?>

    suspend fun saveProfile(profile: DoctorProfile): Resource<Unit>
}
