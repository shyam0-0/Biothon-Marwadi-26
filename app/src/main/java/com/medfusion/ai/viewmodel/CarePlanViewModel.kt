package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.care.CareRuleEngine
import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.domain.model.ProgressAnalysis
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
    val progress: ProgressAnalysis? = null,
    val submitting: Boolean = false,
    val checkInDone: Boolean = false,
    val generatingPlan: Boolean = false,
)

@HiltViewModel
class CarePlanViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val careRepository: CareRepository,
    private val ruleEngine: CareRuleEngine,
    private val aiService: AiService,
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

            val suggestions = ruleEngine.evaluate(plan, logs).map { suggestion ->
                if (suggestion.requiresDoctorApproval) {
                    if (fileApprovals) careRepository.submitPendingApproval(pid, suggestion)
                    suggestion.copy(pending = true)
                } else suggestion
            }
            _uiState.update {
                it.copy(loading = false, plan = plan, suggestions = suggestions)
            }

            // Recovery progress analysis once there's a plan + some check-ins.
            if (plan != null && logs.isNotEmpty()) {
                (aiService.analyzeProgress(logs, plan.diagnosis, LocaleManager.current()) as? Resource.Success)
                    ?.let { r -> _uiState.update { s -> s.copy(progress = r.data) } }
            }
        }
    }

    fun submitCheckIn(
        sleepHours: Double,
        activity: ActivityLevel?,
        mood: Mood?,
        painLevel: Int,
        currentSymptoms: String,
        medicationTaken: Boolean,
        temperature: Double?,
        notes: String,
    ) {
        val pid = patientId ?: return
        if (activity == null || mood == null) {
            _uiState.update { it.copy(error = AppError.Validation("Please select your activity level and mood.")) }
            return
        }
        _uiState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val log = DailyLog(
                date = LocalDate.now().toString(),
                sleepHours = sleepHours,
                activityLevel = activity,
                mood = mood,
                painLevel = painLevel,
                currentSymptoms = currentSymptoms,
                medicationTaken = medicationTaken,
                temperature = temperature,
                notes = notes,
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

    /** Patient accepts an AI wellness plan for a minor condition (Phase 3 gating). */
    fun acceptWellnessPlan() {
        val pid = patientId ?: return
        if (_uiState.value.generatingPlan) return
        _uiState.update { it.copy(generatingPlan = true, error = null) }
        viewModelScope.launch {
            when (val result = aiService.generateWellnessPlan(
                concern = "General minor symptoms self-care",
                language = LocaleManager.current(),
            )) {
                is Resource.Success -> {
                    careRepository.saveCarePlan(result.data.copy(patientId = pid))
                    _uiState.update { it.copy(generatingPlan = false) }
                    load(fileApprovals = false)
                }
                is Resource.Error ->
                    _uiState.update { it.copy(generatingPlan = false, error = result.error) }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }
}
