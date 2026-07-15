package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.repository.CareRepository
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory care plan + daily logs for Demo Mode. */
@Singleton
class FakeCareRepository @Inject constructor() : CareRepository {

    private val logs = CopyOnWriteArrayList<DailyLog>()

    // No plan until a doctor approves one or the patient accepts a wellness plan.
    @Volatile private var storedPlan: CarePlan? = null

    override suspend fun getCarePlan(patientId: String): Resource<CarePlan?> =
        Resource.Success(storedPlan)

    override suspend fun saveCarePlan(plan: CarePlan): Resource<Unit> {
        storedPlan = plan
        return Resource.Success(Unit)
    }

    override suspend fun getRecentLogs(patientId: String, limit: Int): Resource<List<DailyLog>> =
        Resource.Success(logs.sortedByDescending { it.date }.take(limit))

    override suspend fun submitDailyLog(patientId: String, log: DailyLog): Resource<Unit> {
        logs.removeAll { it.date == log.date }
        logs.add(log)
        return Resource.Success(Unit)
    }

    override suspend fun submitPendingApproval(
        patientId: String,
        suggestion: CareSuggestion,
    ): Resource<Unit> = Resource.Success(Unit)
}
