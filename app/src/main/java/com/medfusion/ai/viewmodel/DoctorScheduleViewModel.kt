package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The doctor's schedule grouped for quick review (Phase 6). Informational only. */
data class ScheduleGroups(
    val today: List<Appointment> = emptyList(),
    val upcoming: List<Appointment> = emptyList(),
    val completed: List<Appointment> = emptyList(),
)

@HiltViewModel
class DoctorScheduleViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val doctorProfileRepository: DoctorProfileRepository,
) : ViewModel() {

    // Resolved from doctorAuthUid when a directory profile has been linked to
    // this signed-in doctor; falls back to the existing doctorId == auth-uid
    // behavior unchanged when no match exists.
    private val _doctorId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val uid = authRepository.currentUserId()
            _doctorId.value = uid?.let { u ->
                (doctorProfileRepository.findDoctorIdByAuthUid(u) as? Resource.Success)?.data ?: u
            }
        }
    }

    val groups: StateFlow<ScheduleGroups> = _doctorId
        .flatMapLatest { id -> id?.let { appointmentRepository.observeDoctorQueue(it) } ?: emptyFlow() }
        .map { appointments ->
            val today = LocalDate.now().toString()
            val active = appointments.filter { it.status != AppointmentStatus.DECLINED }
            ScheduleGroups(
                today = active.filter { it.date == today && it.status != AppointmentStatus.COMPLETED },
                upcoming = active
                    .filter { it.date > today && it.status != AppointmentStatus.COMPLETED }
                    .sortedBy { it.date },
                completed = active
                    .filter { it.status == AppointmentStatus.COMPLETED }
                    .sortedByDescending { it.date },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleGroups())
}
