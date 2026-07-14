package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resolves (or creates) the video room URL for an appointment. */
@HiltViewModel
class VideoCallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val appointmentId: String = checkNotNull(savedStateHandle[Routes.Args.APPOINTMENT_ID]) {
        "VideoCallViewModel requires an appointmentId argument"
    }

    private val _roomUrl = MutableStateFlow<UiState<String>>(UiState.Loading)
    val roomUrl: StateFlow<UiState<String>> = _roomUrl.asStateFlow()

    init {
        joinRoom()
    }

    fun joinRoom() {
        _roomUrl.value = UiState.Loading
        viewModelScope.launch {
            _roomUrl.value = when (val result = appointmentRepository.getOrCreateRoom(appointmentId)) {
                is Resource.Success -> UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
        }
    }
}
