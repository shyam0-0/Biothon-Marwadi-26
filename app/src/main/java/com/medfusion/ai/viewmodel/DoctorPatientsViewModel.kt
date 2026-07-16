package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One row in the doctor's patient directory (Phase 6). */
data class DirectoryPatient(
    val patientId: String,
    val patientName: String,
    val lastVisitDate: String,
    val visitCount: Int,
    val highPriority: Boolean,
)

/** Patient directory: the doctor's patients derived from their appointments. */
@HiltViewModel
class DoctorPatientsViewModel @Inject constructor(
    authRepository: AuthRepository,
    appointmentRepository: AppointmentRepository,
) : ViewModel() {

    private val doctorId: String? = authRepository.currentUserId()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val patients: StateFlow<List<DirectoryPatient>> =
        (doctorId?.let { appointmentRepository.observeDoctorQueue(it) } ?: emptyFlow())
            .combine(_query) { appointments, query ->
                appointments
                    .filter { it.status != AppointmentStatus.DECLINED }
                    .groupBy { it.patientId }
                    .map { (patientId, visits) ->
                        DirectoryPatient(
                            patientId = patientId,
                            patientName = visits.firstOrNull { it.patientName.isNotBlank() }
                                ?.patientName ?: "Patient",
                            lastVisitDate = visits.maxOf { it.date },
                            visitCount = visits.size,
                            highPriority = visits.any {
                                it.urgencyLevel == UrgencyLevel.RED &&
                                    it.status != AppointmentStatus.COMPLETED
                            },
                        )
                    }
                    .filter { query.isBlank() || it.patientName.contains(query.trim(), ignoreCase = true) }
                    .sortedWith(compareByDescending<DirectoryPatient> { it.highPriority }
                        .thenByDescending { it.lastVisitDate })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(text: String) {
        _query.value = text
    }
}
