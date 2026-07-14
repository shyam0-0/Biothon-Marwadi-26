package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.data.location.LocationProvider
import com.medfusion.ai.domain.model.EmergencyAction
import com.medfusion.ai.domain.model.EmergencyOutcome
import com.medfusion.ai.domain.repository.EmergencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import javax.inject.Inject

/** Phases of the vitals monitor + emergency flow. */
sealed interface VitalsPhase {
    data object Monitoring : VitalsPhase
    data class Alert(val secondsLeft: Int) : VitalsPhase
    data object Escalating : VitalsPhase
    data class Escalated(val outcome: EmergencyOutcome) : VitalsPhase
    data object Dismissed : VitalsPhase
}

data class VitalsUiState(
    val currentHr: Int = 72,
    val history: List<Int> = emptyList(),
    val phase: VitalsPhase = VitalsPhase.Monitoring,
    val simulateAbnormal: Boolean = false,
    val error: AppError? = null,
)

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val emergencyRepository: EmergencyRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        startMonitoring()
    }

    /** Emits a new (simulated) heart-rate reading roughly every 1.2s. */
    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                delay(1_200)
                val state = _uiState.value
                val target = if (state.simulateAbnormal) 150 else 74
                // Random walk toward the target so the trace looks organic.
                val next = (state.currentHr + ((target - state.currentHr) / 3) +
                    Random.nextInt(-4, 5)).coerceIn(30, 200)
                val history = (state.history + next).takeLast(WINDOW)
                _uiState.update { it.copy(currentHr = next, history = history) }

                if (state.phase is VitalsPhase.Monitoring) evaluate(history)
            }
        }
    }

    private fun evaluate(history: List<Int>) {
        val recent = history.takeLast(SUSTAINED_READINGS)
        val sustainedAnomaly = recent.size >= SUSTAINED_READINGS &&
            recent.all { it < NORMAL_MIN || it > NORMAL_MAX }
        if (sustainedAnomaly) triggerAlert()
    }

    private fun triggerAlert() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var seconds = COUNTDOWN_SECONDS
            _uiState.update { it.copy(phase = VitalsPhase.Alert(seconds)) }
            while (seconds > 0) {
                delay(1_000)
                seconds--
                // Only keep counting if the user hasn't already responded.
                if (_uiState.value.phase !is VitalsPhase.Alert) return@launch
                _uiState.update { it.copy(phase = VitalsPhase.Alert(seconds)) }
            }
            // No response in time → auto-escalate.
            escalate(EmergencyAction.AUTO_ESCALATED)
        }
    }

    fun confirmEmergency() {
        countdownJob?.cancel()
        escalate(EmergencyAction.CONFIRMED)
    }

    fun imFine() {
        countdownJob?.cancel()
        viewModelScope.launch {
            // Log the dismissal for the audit trail, then resume monitoring.
            emergencyRepository.reportAnomaly(
                heartRate = _uiState.value.currentHr,
                action = EmergencyAction.DISMISSED,
                location = null,
            )
            _uiState.update { it.copy(phase = VitalsPhase.Dismissed, simulateAbnormal = false) }
        }
    }

    private fun escalate(action: EmergencyAction) {
        _uiState.update { it.copy(phase = VitalsPhase.Escalating) }
        viewModelScope.launch {
            val location = locationProvider.lastKnown()
            when (val result = emergencyRepository.reportAnomaly(
                heartRate = _uiState.value.currentHr,
                action = action,
                location = location,
            )) {
                is Resource.Success ->
                    _uiState.update { it.copy(phase = VitalsPhase.Escalated(result.data)) }
                is Resource.Error ->
                    _uiState.update { it.copy(phase = VitalsPhase.Monitoring, error = result.error) }
            }
        }
    }

    /** Demo control: pushes readings into the abnormal range to exercise the flow. */
    fun setSimulateAbnormal(enabled: Boolean) =
        _uiState.update { it.copy(simulateAbnormal = enabled) }

    /** Return to normal monitoring after an alert/escalation is acknowledged. */
    fun resumeMonitoring() =
        _uiState.update { it.copy(phase = VitalsPhase.Monitoring, simulateAbnormal = false, error = null) }

    private companion object {
        const val NORMAL_MIN = 50
        const val NORMAL_MAX = 120
        const val SUSTAINED_READINGS = 3
        const val COUNTDOWN_SECONDS = 15
        const val WINDOW = 30
    }
}
