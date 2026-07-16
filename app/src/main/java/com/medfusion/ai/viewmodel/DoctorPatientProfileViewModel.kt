package com.medfusion.ai.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.locale.LocaleManager
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.ai.AiService
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.PassportRepository
import com.medfusion.ai.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only patient record for the doctor (Phase 6). Never editable. */
data class DoctorPatientProfileState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val patientName: String = "",
    val passport: PatientPassport? = null,
    val aiHistory: List<AiConsultationRecord> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val carePlan: CarePlan? = null,
    val recentLogs: List<DailyLog> = emptyList(),
    val progress: ProgressAnalysis? = null,
    /** Completed consultations with this doctor (diagnosis on record). */
    val consultations: List<Appointment> = emptyList(),
)

/**
 * The doctor's review view of one patient: passport, medical history, AI
 * analyses, consultations, care plan and recovery progress — before treatment.
 */
@HiltViewModel
class DoctorPatientProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    private val passportRepository: PassportRepository,
    private val careRepository: CareRepository,
    private val appointmentRepository: AppointmentRepository,
    private val aiService: AiService,
) : ViewModel() {

    private val patientId: String = checkNotNull(savedStateHandle[Routes.Args.PATIENT_ID]) {
        "DoctorPatientProfileViewModel requires a patientId"
    }
    private val patientName: String = savedStateHandle.get<String>(Routes.Args.PATIENT_NAME) ?: "Patient"
    private val doctorId: String? = authRepository.currentUserId()

    private val _uiState = MutableStateFlow(DoctorPatientProfileState(patientName = patientName))
    val uiState: StateFlow<DoctorPatientProfileState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val passport = passportRepository.getPassport(patientId)) {
                is Resource.Success -> _uiState.update { it.copy(passport = passport.data) }
                is Resource.Error -> {
                    _uiState.update { it.copy(loading = false, error = passport.error) }
                    return@launch
                }
            }
            // Best-effort sections: a failure leaves that section empty.
            (passportRepository.getAiHistory(patientId) as? Resource.Success)?.let { r ->
                _uiState.update { it.copy(aiHistory = r.data) }
            }
            (passportRepository.getTimeline(patientId) as? Resource.Success)?.let { r ->
                _uiState.update { it.copy(timeline = r.data) }
            }
            val plan = (careRepository.getCarePlan(patientId) as? Resource.Success)?.data
            _uiState.update { it.copy(carePlan = plan) }

            val logs = (careRepository.getRecentLogs(patientId) as? Resource.Success)?.data.orEmpty()
            _uiState.update { it.copy(recentLogs = logs) }
            if (logs.isNotEmpty()) {
                (aiService.analyzeProgress(logs, plan?.diagnosis, LocaleManager.current())
                    as? Resource.Success)?.let { p ->
                    _uiState.update { it.copy(progress = p.data) }
                }
            }

            doctorId?.let { id ->
                val consultations = appointmentRepository.observeDoctorQueue(id)
                    .firstOrNull().orEmpty()
                    .filter { it.patientId == patientId && it.status == AppointmentStatus.COMPLETED }
                    .sortedByDescending { it.date }
                _uiState.update { it.copy(consultations = consultations) }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }
}
