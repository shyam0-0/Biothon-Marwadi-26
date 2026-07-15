package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.repository.ConsultationRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Loads the prescription for an appointment (patient view, Phase 2). */
@HiltViewModel
class PrescriptionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val consultationRepository: ConsultationRepository,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.Args.APPOINTMENT_ID]) {
        "PrescriptionViewModel requires an appointmentId"
    }

    private val _state = MutableStateFlow<UiState<Prescription>>(UiState.Loading)
    val state: StateFlow<UiState<Prescription>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = consultationRepository.getPrescriptionForAppointment(appointmentId)) {
                is Resource.Success -> result.data?.let { UiState.Success(it) } ?: UiState.Empty
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }
}
