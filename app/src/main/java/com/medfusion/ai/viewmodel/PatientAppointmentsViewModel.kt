package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.video.VideoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Streams the signed-in patient's appointments and joins calls via the provider. */
@HiltViewModel
class PatientAppointmentsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val appointmentRepository: AppointmentRepository,
    private val videoProvider: VideoProvider,
) : ViewModel() {

    private val patientId: String? = authRepository.currentUserId()

    val appointments: StateFlow<List<Appointment>> =
        (patientId?.let { appointmentRepository.observePatientAppointments(it) } ?: emptyFlow())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Fetches/creates the room and hands off to the modular video provider. */
    fun joinCall(appointmentId: String) {
        viewModelScope.launch {
            (appointmentRepository.getOrCreateRoom(appointmentId) as? Resource.Success)
                ?.let { videoProvider.join(it.data) }
        }
    }
}
