package com.medfusion.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.NotificationKind
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.LiveVitalsRepository
import com.medfusion.ai.domain.repository.NotificationRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Journey snapshot for the patient home (Phase 4): the next upcoming
 * appointment and whether today's care-plan check-in is still due, so the
 * dashboard reflects where the patient is in their care journey.
 */
data class PatientHomeState(
    val nextAppointment: Appointment? = null,
    val hasCarePlan: Boolean = false,
    val checkInDueToday: Boolean = false,
    /** Live IoT vitals (Phase 7.4) — Loading until Firestore's first snapshot. */
    val liveVitals: LiveVitalsCardState = LiveVitalsCardState.Loading,
)

@HiltViewModel
class PatientDashboardViewModel @Inject constructor(
    authRepository: AuthRepository,
    appointmentRepository: AppointmentRepository,
    careRepository: CareRepository,
    notificationRepository: NotificationRepository,
    liveVitalsRepository: LiveVitalsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PatientHomeState())
    val state: StateFlow<PatientHomeState> = _state.asStateFlow()

    init {
        val pid = authRepository.currentUserId()
        if (pid != null) {
            val today = LocalDate.now().toString()

            // Live vitals (Phase 7.4): independent of the sections below, so a
            // Firestore hiccup on one never blocks the other from updating.
            // Phase 7.5 temporary diagnostic: pid here MUST match the patientId
            // the vitals source (ESP32/backend) uploads under, or this observes
            // an empty/nonexistent document forever.
            Log.d("LiveVitals", "[ViewModel] PatientDashboardViewModel observing patientId=$pid")
            viewModelScope.launch {
                liveVitalsRepository.observeVitalsCardState(pid).collect { cardState ->
                    Log.d("LiveVitals", "[ViewModel] PatientDashboardViewModel updating state with $cardState")
                    _state.update { it.copy(liveVitals = cardState) }
                }
            }

            viewModelScope.launch {
                appointmentRepository.observePatientAppointments(pid).collect { appointments ->
                    val next = appointments
                        .filter {
                            it.date >= today &&
                                it.status != AppointmentStatus.COMPLETED &&
                                it.status != AppointmentStatus.DECLINED
                        }
                        .minByOrNull { it.date }
                    _state.update { it.copy(nextAppointment = next) }
                }
            }

            // Best-effort care-plan nudge; failures just leave the default card text.
            viewModelScope.launch {
                val plan = (careRepository.getCarePlan(pid) as? Resource.Success)?.data
                    ?: return@launch
                val latestLog = (careRepository.getRecentLogs(pid, limit = 1) as? Resource.Success)
                    ?.data?.firstOrNull()
                val dueToday = latestLog?.date != today
                _state.update { it.copy(hasCarePlan = true, checkInDueToday = dueToday) }
                // Notification center (Phase 6.5): one check-in reminder per day.
                if (dueToday) {
                    notificationRepository.post(
                        AppNotification(
                            audience = UserRole.PATIENT,
                            kind = NotificationKind.CHECK_IN_REMINDER,
                            route = Routes.CARE_PLAN,
                            dedupeKey = "check-in-$today",
                        ),
                    )
                }
            }
        }
    }
}
