package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.DoctorProfile
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctorProfileUiState(
    val profile: DoctorProfile? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
)

/** Edit state for the signed-in doctor's own professional profile (Phase 6.5). */
@HiltViewModel
class DoctorProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val profileRepository: DoctorProfileRepository,
) : ViewModel() {

    private val doctorId: String? = authRepository.currentUserId()

    private val _uiState = MutableStateFlow(DoctorProfileUiState())
    val uiState: StateFlow<DoctorProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val id = doctorId
            if (id == null) {
                _uiState.update { it.copy(loading = false, error = AppError.Unauthorized()) }
            } else {
                val existing = (profileRepository.getProfile(id) as? Resource.Success)?.data
                // Prefill the name from the signed-in account when no profile exists yet.
                val initial = existing ?: DoctorProfile(
                    doctorId = id,
                    fullName = authRepository.currentUser.firstOrNull()?.fullName.orEmpty(),
                )
                _uiState.update { it.copy(profile = initial, loading = false) }
            }
        }
    }

    fun update(transform: (DoctorProfile) -> DoctorProfile) {
        _uiState.update { state ->
            state.profile?.let { state.copy(profile = transform(it), saved = false) } ?: state
        }
    }

    fun save(validationMessage: String) {
        val profile = _uiState.value.profile ?: return
        if (!profile.isComplete) {
            _uiState.update { it.copy(error = AppError.Validation(validationMessage)) }
            return
        }
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val result = profileRepository.saveProfile(profile)) {
                is Resource.Success -> _uiState.update { it.copy(saving = false, saved = true) }
                is Resource.Error -> _uiState.update { it.copy(saving = false, error = result.error) }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }
}
