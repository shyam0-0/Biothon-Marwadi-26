package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.NotificationKind
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import com.medfusion.ai.domain.repository.NotificationRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Backs the doctor dashboard: the urgency-sorted queue + per-patient AI pre-read. */
@HiltViewModel
class DoctorDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val caseRepository: CaseRepository,
    private val notificationRepository: NotificationRepository,
    private val doctorProfileRepository: DoctorProfileRepository,
) : ViewModel() {

    // Resolved from doctorAuthUid when a directory profile has been linked to
    // this signed-in doctor; falls back to the existing doctorId == auth-uid
    // behavior unchanged when no match exists.
    private val _doctorId = MutableStateFlow<String?>(null)

    val queue: StateFlow<List<Appointment>> = _doctorId
        .flatMapLatest { id -> id?.let { appointmentRepository.observeDoctorQueue(it) } ?: emptyFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val uid = authRepository.currentUserId()
            _doctorId.value = uid?.let { u ->
                (doctorProfileRepository.findDoctorIdByAuthUid(u) as? Resource.Success)?.data ?: u
            }
        }
    }

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
            val result = appointmentRepository.updateStatus(
                appointmentId = appointmentId,
                status = AppointmentStatus.RESCHEDULED,
                newDate = date,
                newTimeSlot = timeSlot,
            )
            if (result is Resource.Success) {
                notifyPatient(NotificationKind.APPOINTMENT_RESCHEDULED, "$date $timeSlot")
            }
        }
    }

    private fun updateStatus(appointmentId: String, status: AppointmentStatus) {
        viewModelScope.launch {
            val result = appointmentRepository.updateStatus(appointmentId, status)
            if (result is Resource.Success) {
                when (status) {
                    AppointmentStatus.ACCEPTED ->
                        notifyPatient(NotificationKind.APPOINTMENT_ACCEPTED, null)
                    AppointmentStatus.DECLINED ->
                        notifyPatient(NotificationKind.APPOINTMENT_CANCELLED, null)
                    else -> Unit
                }
            }
        }
    }

    /** Notification center (Phase 6.5): keep the patient informed of queue decisions. */
    private suspend fun notifyPatient(kind: NotificationKind, detail: String?) {
        notificationRepository.post(
            AppNotification(
                audience = UserRole.PATIENT,
                kind = kind,
                detail = detail,
                route = Routes.PATIENT_APPOINTMENTS,
            ),
        )
    }
}
