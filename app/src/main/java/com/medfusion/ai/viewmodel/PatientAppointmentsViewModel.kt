package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.video.VideoProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    /** Fetches/creates the room and hands off to the modular video provider. */
    fun joinCall(appointmentId: String) {
        _error.value = null
        viewModelScope.launch {
            when (val result = appointmentRepository.getOrCreateRoom(appointmentId)) {
                is Resource.Success -> videoProvider.join(result.data)
                is Resource.Error -> _error.value = result.error
            }
        }
    }
}
