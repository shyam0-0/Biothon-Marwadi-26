package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class BookingUiState(
    val date: String? = null,                                   // ISO yyyy-MM-dd
    val availability: UiState<List<AvailabilitySlot>> = UiState.Idle,
    val selectedSlot: AvailabilitySlot? = null,
    val message: String = "",
    val isBooking: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class BookAppointmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val caseId: String? = savedStateHandle[Routes.Args.CASE_ID]
    private val urgency: UrgencyLevel =
        UrgencyLevel.fromWire(savedStateHandle[Routes.Args.URGENCY])

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Appointment>(extraBufferCapacity = 1)
    val bookedEvents: SharedFlow<Appointment> = _events.asSharedFlow()

    fun onDateSelected(epochMillis: Long) {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val iso = date.toString()
        _uiState.update { it.copy(date = iso, selectedSlot = null, availability = UiState.Loading) }
        loadAvailability(iso)
    }

    private fun loadAvailability(date: String) {
        viewModelScope.launch {
            val newState = when (val result = appointmentRepository.getAvailability(date)) {
                is Resource.Success ->
                    if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
            _uiState.update { it.copy(availability = newState) }
        }
    }

    fun onSlotSelected(slot: AvailabilitySlot) =
        _uiState.update { it.copy(selectedSlot = slot, error = null) }

    fun onMessageChange(text: String) = _uiState.update { it.copy(message = text) }

    fun book() {
        val state = _uiState.value
        val slot = state.selectedSlot
        val date = state.date
        if (date == null || slot == null) {
            _uiState.update { it.copy(error = AppError.Validation("Please pick a date and a time slot.")) }
            return
        }
        _uiState.update { it.copy(isBooking = true, error = null) }
        viewModelScope.launch {
            when (val result = appointmentRepository.bookAppointment(
                caseId = caseId,
                doctorId = slot.doctorId,
                doctorName = slot.doctorName,
                date = date,
                timeSlot = slot.timeSlot,
                message = state.message,
                urgency = urgency,
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isBooking = false) }
                    _events.emit(result.data)
                }
                is Resource.Error ->
                    _uiState.update { it.copy(isBooking = false, error = result.error) }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    /** Default date offered by the picker (today, in millis) for convenience. */
    fun todayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
