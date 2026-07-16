package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.ConsultationRepository
import com.medfusion.ai.domain.repository.PassportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Everything the digital health record shows (Phase 5). */
data class PassportUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val passport: PatientPassport? = null,
    val aiHistory: List<AiConsultationRecord> = emptyList(),
    val timeline: List<TimelineEvent> = emptyList(),
    val carePlan: CarePlan? = null,
    val nextAppointment: Appointment? = null,
    val latestPrescription: Prescription? = null,
    val saving: Boolean = false,
    val editing: Boolean = false,
)

/**
 * The Patient Passport dashboard: profile + medical summary, risk factors, the
 * current care plan, upcoming appointment, stored AI sessions, latest
 * prescription and the automatic medical timeline — one digital health record.
 */
@HiltViewModel
class PatientPassportViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val passportRepository: PassportRepository,
    private val careRepository: CareRepository,
    private val appointmentRepository: AppointmentRepository,
    private val consultationRepository: ConsultationRepository,
) : ViewModel() {

    private val patientId: String? = authRepository.currentUserId()

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        val pid = patientId ?: run {
            _uiState.update { it.copy(loading = false, error = AppError.Unauthorized()) }
            return
        }
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val passport = passportRepository.getPassport(pid)) {
                is Resource.Success -> _uiState.update { it.copy(passport = passport.data) }
                is Resource.Error -> {
                    _uiState.update { it.copy(loading = false, error = passport.error) }
                    return@launch
                }
            }
            // The rest is best-effort: a failed section just stays empty.
            (passportRepository.getAiHistory(pid) as? Resource.Success)?.let { r ->
                _uiState.update { it.copy(aiHistory = r.data) }
            }
            (passportRepository.getTimeline(pid) as? Resource.Success)?.let { r ->
                _uiState.update { it.copy(timeline = r.data) }
            }
            (careRepository.getCarePlan(pid) as? Resource.Success)?.let { r ->
                _uiState.update { it.copy(carePlan = r.data) }
            }
            val appointments = appointmentRepository.observePatientAppointments(pid)
                .firstOrNull().orEmpty()
            val today = LocalDate.now().toString()
            val upcoming = appointments
                .filter {
                    it.date >= today &&
                        it.status != AppointmentStatus.COMPLETED &&
                        it.status != AppointmentStatus.DECLINED
                }
                .minByOrNull { it.date }
            _uiState.update { it.copy(nextAppointment = upcoming) }

            // Latest prescription comes from the most recent completed visit.
            val lastCompleted = appointments
                .filter { it.status == AppointmentStatus.COMPLETED && it.prescriptionId != null }
                .maxByOrNull { it.date }
            if (lastCompleted != null) {
                (consultationRepository.getPrescriptionForAppointment(lastCompleted.id)
                    as? Resource.Success)?.let { p ->
                    _uiState.update { it.copy(latestPrescription = p.data) }
                }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun startEditing() = _uiState.update { it.copy(editing = true) }
    fun cancelEditing() = _uiState.update { it.copy(editing = false) }

    /** Persists profile/medical-history edits, then reloads the record. */
    fun savePassport(updated: PatientPassport) {
        val pid = patientId ?: return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val result = passportRepository.savePassport(updated.copy(patientId = pid))) {
                is Resource.Success -> {
                    _uiState.update { it.copy(saving = false, editing = false, passport = updated.copy(patientId = pid)) }
                }
                is Resource.Error ->
                    _uiState.update { it.copy(saving = false, error = result.error) }
            }
        }
    }

    fun consumeError() = _uiState.update { it.copy(error = null) }
}
