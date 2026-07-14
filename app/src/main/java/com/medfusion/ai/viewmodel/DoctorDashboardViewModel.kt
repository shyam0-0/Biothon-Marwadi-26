package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Backs the doctor dashboard: the urgency-sorted queue + per-patient AI pre-read. */
@HiltViewModel
class DoctorDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val caseRepository: CaseRepository,
) : ViewModel() {

    private val doctorId: String? = authRepository.currentUserId()

    val queue: StateFlow<List<Appointment>> =
        (doctorId?.let { appointmentRepository.observeDoctorQueue(it) } ?: emptyFlow())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lazily-loaded pre-read cases, keyed by caseId, populated when a row expands. */
    private val _cases = MutableStateFlow<Map<String, UiState<Case>>>(emptyMap())
    val cases: StateFlow<Map<String, UiState<Case>>> = _cases.asStateFlow()

    fun loadCase(caseId: String) {
        if (_cases.value[caseId] is UiState.Success || _cases.value[caseId] is UiState.Loading) return
        _cases.update { it + (caseId to UiState.Loading) }
        viewModelScope.launch {
            val state = when (val result = caseRepository.getCase(caseId)) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
            _cases.update { it + (caseId to state) }
        }
    }

    fun accept(appointmentId: String) = updateStatus(appointmentId, AppointmentStatus.ACCEPTED)

    fun decline(appointmentId: String) = updateStatus(appointmentId, AppointmentStatus.DECLINED)

    fun reschedule(appointmentId: String, epochMillis: Long, timeSlot: String) {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        viewModelScope.launch {
            appointmentRepository.updateStatus(
                appointmentId = appointmentId,
                status = AppointmentStatus.RESCHEDULED,
                newDate = date,
                newTimeSlot = timeSlot,
            )
        }
    }

    private fun updateStatus(appointmentId: String, status: AppointmentStatus) {
        viewModelScope.launch {
            appointmentRepository.updateStatus(appointmentId, status)
        }
    }
}
