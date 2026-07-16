package com.medfusion.ai.domain.passport

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.PatientContext
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.PassportRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles the Smart AI Context (Phase 5) before every Gemini request: passport,
 * previous AI consultations, latest care plan, recent daily check-ins and the
 * doctor's diagnosis. Every part is best-effort — a missing piece never blocks
 * the analysis, it just narrows what the AI knows.
 */
@Singleton
class PatientContextBuilder @Inject constructor(
    private val authRepository: AuthRepository,
    private val passportRepository: PassportRepository,
    private val careRepository: CareRepository,
) {

    suspend fun build(): PatientContext {
        val patientId = authRepository.currentUserId() ?: return PatientContext()

        val passport = (passportRepository.getPassport(patientId) as? Resource.Success)?.data
        val aiHistory = (passportRepository.getAiHistory(patientId, limit = 3) as? Resource.Success)
            ?.data.orEmpty()
        val carePlan = (careRepository.getCarePlan(patientId) as? Resource.Success)?.data
        val recentLogs = (careRepository.getRecentLogs(patientId, limit = 5) as? Resource.Success)
            ?.data.orEmpty()

        return PatientContext(
            passport = passport,
            aiHistory = aiHistory,
            carePlan = carePlan,
            recentLogs = recentLogs,
            latestDiagnosis = carePlan?.diagnosis,
        )
    }
}
