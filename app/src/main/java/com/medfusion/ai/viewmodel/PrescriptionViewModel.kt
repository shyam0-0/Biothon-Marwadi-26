package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.model.PatientExplanation
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.repository.CareRepository
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
    private val careRepository: CareRepository,
    private val aiService: AiService,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.Args.APPOINTMENT_ID]) {
        "PrescriptionViewModel requires an appointmentId"
    }

    private val _state = MutableStateFlow<UiState<Prescription>>(UiState.Loading)
    val state: StateFlow<UiState<Prescription>> = _state.asStateFlow()

    /** Patient-friendly translation of the clinical outcome (Phase 5.6). */
    private val _explanation = MutableStateFlow<UiState<PatientExplanation>>(UiState.Idle)
    val explanation: StateFlow<UiState<PatientExplanation>> = _explanation.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = consultationRepository.getPrescriptionForAppointment(appointmentId)) {
                is Resource.Success -> result.data?.let {
                    // Automatically generate the patient-friendly explanation —
                    // the stored clinical record is never modified.
                    generateExplanation(it)
                    UiState.Success(it)
                } ?: UiState.Empty
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }

    fun retryExplanation() {
        (_state.value as? UiState.Success)?.data?.let { generateExplanation(it) }
    }

    private fun generateExplanation(prescription: Prescription) {
        _explanation.value = UiState.Loading
        viewModelScope.launch {
            val plan = (careRepository.getCarePlan(prescription.patientId) as? Resource.Success)?.data
            _explanation.value = when (
                val result = aiService.explainForPatient(prescription, plan, LocaleManager.current())
            ) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }
}
