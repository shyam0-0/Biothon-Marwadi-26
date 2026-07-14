package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Streams the signed-in patient's appointments for the appointments list. */
@HiltViewModel
class PatientAppointmentsViewModel @Inject constructor(
    authRepository: AuthRepository,
    appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val patientId: String? = authRepository.currentUserId()

    val appointments: StateFlow<List<Appointment>> =
        (patientId?.let { appointmentRepository.observePatientAppointments(it) } ?: emptyFlow())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
