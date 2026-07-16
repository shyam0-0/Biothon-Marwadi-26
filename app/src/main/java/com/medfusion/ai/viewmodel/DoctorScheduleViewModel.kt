package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    authRepository: AuthRepository,
    appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val doctorId: String? = authRepository.currentUserId()

    val groups: StateFlow<ScheduleGroups> =
        (doctorId?.let { appointmentRepository.observeDoctorQueue(it) } ?: emptyFlow())
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
