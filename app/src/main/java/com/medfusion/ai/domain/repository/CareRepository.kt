package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.DailyLog

/** Care plan, daily logs, and doctor-approval routing for the care companion (Phase 10). */
interface CareRepository {

    /** The patient's doctor-set care plan (or a demo default in debug when unset). */
    suspend fun getCarePlan(patientId: String): Resource<CarePlan?>

    /** Persists a care plan (generated on doctor approval, Phase 2). */
    suspend fun saveCarePlan(plan: CarePlan): Resource<Unit>

    /** Recent daily logs, newest first, up to [limit]. */
    suspend fun getRecentLogs(patientId: String, limit: Int = 7): Resource<List<DailyLog>>

    /** Saves (or overwrites) today's daily log. */
    suspend fun submitDailyLog(patientId: String, log: DailyLog): Resource<Unit>

    /** Files a medication-related suggestion for doctor approval. */
    suspend fun submitPendingApproval(patientId: String, suggestion: CareSuggestion): Resource<Unit>
}
