package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.UiState
import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.domain.model.Doctor
import com.medfusion.ai.domain.model.NotificationKind
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.model.TimelineEventType
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.NotificationRepository
import com.medfusion.ai.domain.repository.PassportRepository
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
    val specialty: String,
    val doctors: UiState<List<Doctor>> = UiState.Loading,
    val selectedDoctor: Doctor? = null,
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
    private val authRepository: AuthRepository,
    private val passportRepository: PassportRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val caseId: String? = savedStateHandle.get<String>(Routes.Args.CASE_ID)
        ?.takeUnless { it == Routes.FOLLOW_UP_CASE }
    private val urgency: UrgencyLevel =
        UrgencyLevel.fromWire(savedStateHandle[Routes.Args.URGENCY])
    private val specialty: String =
        savedStateHandle.get<String>(Routes.Args.SPECIALTY)?.ifBlank { null } ?: "General Physician"

    private val _uiState = MutableStateFlow(BookingUiState(specialty = specialty))
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Appointment>(extraBufferCapacity = 1)
    val bookedEvents: SharedFlow<Appointment> = _events.asSharedFlow()

    init {
        loadDoctors()
    }

    private fun loadDoctors() {
        _uiState.update { it.copy(doctors = UiState.Loading) }
        viewModelScope.launch {
            val state = when (val result = appointmentRepository.getDoctorsBySpecialty(specialty)) {
                is Resource.Success ->
                    if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
            _uiState.update { it.copy(doctors = state) }
        }
    }

    fun selectDoctor(doctor: Doctor) = _uiState.update {
        it.copy(
            selectedDoctor = doctor,
            date = null,
            availability = UiState.Idle,
            selectedSlot = null,
            error = null,
        )
    }

    fun changeDoctor() = _uiState.update { it.copy(selectedDoctor = null) }

    fun onDateSelected(epochMillis: Long) {
        val iso = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val doctorId = _uiState.value.selectedDoctor?.id ?: return
        _uiState.update { it.copy(date = iso, selectedSlot = null, availability = UiState.Loading) }
        viewModelScope.launch {
            val state = when (val result = appointmentRepository.getDoctorAvailability(doctorId, iso)) {
                is Resource.Success ->
                    if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                is Resource.Error -> UiState.Error(result.error)
            }
            _uiState.update { it.copy(availability = state) }
        }
    }

    fun onSlotSelected(slot: AvailabilitySlot) =
        _uiState.update { it.copy(selectedSlot = slot, error = null) }

    fun onMessageChange(text: String) = _uiState.update { it.copy(message = text) }

    fun book() {
        val state = _uiState.value
        val doctor = state.selectedDoctor
        val slot = state.selectedSlot
        val date = state.date
        if (doctor == null || date == null || slot == null) {
            _uiState.update { it.copy(error = AppError.Validation("Please pick a doctor, date and time slot.")) }
            return
        }
        _uiState.update { it.copy(isBooking = true, error = null) }
        viewModelScope.launch {
            when (val result = appointmentRepository.bookAppointment(
                caseId = caseId,
                doctorId = doctor.id,
                doctorName = doctor.name,
                date = date,
                timeSlot = slot.timeSlot,
                message = state.message,
                urgency = urgency,
                specialty = specialty,
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isBooking = false) }
                    // Passport timeline (Phase 5): the booking becomes part of the
                    // patient's medical journey automatically.
                    authRepository.currentUserId()?.let { pid ->
                        passportRepository.addTimelineEvent(
                            pid,
                            TimelineEvent(
                                type = TimelineEventType.APPOINTMENT_BOOKED,
                                title = "Appointment Booked",
                                detail = "${doctor.name} ($specialty) • $date at ${slot.timeSlot}",
                                dateMillis = System.currentTimeMillis(),
                            ),
                        )
                    }
                    // Notification center (Phase 6.5): confirm to the patient,
                    // alert the doctor portal of the new request.
                    val detail = "${doctor.name} • $date ${slot.timeSlot}"
                    notificationRepository.post(
                        AppNotification(
                            audience = UserRole.PATIENT,
                            kind = NotificationKind.APPOINTMENT_BOOKED,
                            detail = detail,
                            route = Routes.PATIENT_APPOINTMENTS,
                        ),
                    )
                    notificationRepository.post(
                        AppNotification(
                            audience = UserRole.DOCTOR,
                            kind = if (caseId == null) NotificationKind.FOLLOW_UP_BOOKED
                            else NotificationKind.NEW_APPOINTMENT_REQUEST,
                            detail = "$date ${slot.timeSlot}",
                            route = Routes.DOCTOR_DASHBOARD,
                        ),
                    )
                    _events.emit(result.data)
                }
                is Resource.Error ->
                    _uiState.update { it.copy(isBooking = false, error = result.error) }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }

    fun todayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
