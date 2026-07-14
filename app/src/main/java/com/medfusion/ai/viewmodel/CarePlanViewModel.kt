package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.care.CareRuleEngine
import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CareUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val plan: CarePlan? = null,
    val suggestions: List<CareSuggestion> = emptyList(),
    val submitting: Boolean = false,
    val checkInDone: Boolean = false,
)

@HiltViewModel
class CarePlanViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val careRepository: CareRepository,
    private val ruleEngine: CareRuleEngine,
) : ViewModel() {

    private val patientId: String? = authRepository.currentUserId()

    private val _uiState = MutableStateFlow(CareUiState())
    val uiState: StateFlow<CareUiState> = _uiState.asStateFlow()

    init {
        load(fileApprovals = false)
    }

    private fun load(fileApprovals: Boolean) {
        val pid = patientId ?: run {
            _uiState.update { it.copy(loading = false, error = AppError.Unauthorized()) }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val planResult = careRepository.getCarePlan(pid)
            val logsResult = careRepository.getRecentLogs(pid)

            if (planResult is Resource.Error) {
                _uiState.update { it.copy(loading = false, error = planResult.error) }
                return@launch
            }
            val plan = (planResult as Resource.Success).data
            val logs = (logsResult as? Resource.Success)?.data.orEmpty()

            val raw = ruleEngine.evaluate(plan, logs)
            val suggestions = raw.map { suggestion ->
                if (suggestion.requiresDoctorApproval) {
                    // Medication-related: file for approval (once, after a check-in)
                    // and surface it as pending rather than applying it.
                    if (fileApprovals) careRepository.submitPendingApproval(pid, suggestion)
                    suggestion.copy(pending = true)
                } else {
                    suggestion
                }
            }
            _uiState.update { it.copy(loading = false, plan = plan, suggestions = suggestions) }
        }
    }

    fun submitCheckIn(sleepHours: Double?, activity: ActivityLevel?, mood: Mood?) {
        val pid = patientId ?: return
        if (sleepHours == null || activity == null || mood == null) {
            _uiState.update { it.copy(error = AppError.Validation("Please complete all check-in fields.")) }
            return
        }
        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val log = DailyLog(
                date = LocalDate.now().toString(),
                sleepHours = sleepHours,
                activityLevel = activity,
                mood = mood,
            )
            when (val result = careRepository.submitDailyLog(pid, log)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(submitting = false, checkInDone = true) }
                    load(fileApprovals = true)
                }
                is Resource.Error ->
                    _uiState.update { it.copy(submitting = false, error = result.error) }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }
}
