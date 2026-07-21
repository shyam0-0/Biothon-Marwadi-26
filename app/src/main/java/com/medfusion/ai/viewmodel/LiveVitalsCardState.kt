package com.medfusion.ai.viewmodel

import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.LiveVitals
import com.medfusion.ai.domain.repository.LiveVitalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Live Vitals card state (Phase 7.4). Shared by [PatientDashboardViewModel]
 * and [DoctorPatientProfileViewModel] so the same repository read maps to the
 * same four states in one place instead of being duplicated per screen.
 */
sealed interface LiveVitalsCardState {
    data object Loading : LiveVitalsCardState
    data object Empty : LiveVitalsCardState
    data class Data(val vitals: LiveVitals) : LiveVitalsCardState
    data class Error(val error: AppError) : LiveVitalsCardState
}

/** Maps the repository's [Resource] stream to the card's UI states, starting
 * with [LiveVitalsCardState.Loading] until the first snapshot arrives. */
fun LiveVitalsRepository.observeVitalsCardState(patientId: String): Flow<LiveVitalsCardState> =
    observeLatestVitals(patientId)
        .map { resource ->
            when (resource) {
                is Resource.Success ->
                    resource.data?.let { LiveVitalsCardState.Data(it) } ?: LiveVitalsCardState.Empty
                is Resource.Error -> LiveVitalsCardState.Error(resource.error)
            }
        }
        .onStart { emit(LiveVitalsCardState.Loading) }
