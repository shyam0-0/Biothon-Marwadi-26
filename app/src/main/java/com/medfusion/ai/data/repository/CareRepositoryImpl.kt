package com.medfusion.ai.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.medfusion.ai.BuildConfig
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.CarePlans
import com.medfusion.ai.data.firebase.FirestoreSchema.PendingApprovals
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CareSuggestion
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.domain.repository.CareRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CareRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CareRepository {

    private fun planDoc(patientId: String) =
        firestore.collection(CarePlans.COLLECTION).document(patientId)

    private fun logsCollection(patientId: String) =
        planDoc(patientId).collection(CarePlans.DAILY_LOGS)

    override suspend fun getCarePlan(patientId: String): Resource<CarePlan?> = withContext(io) {
        resourceOf {
            val snap = planDoc(patientId).get().await()
            if (!snap.exists()) {
                if (BuildConfig.USE_MOCK_AI_FALLBACK) demoCarePlan(patientId) else null
            } else {
                @Suppress("UNCHECKED_CAST")
                val meds = (snap.get(CarePlans.MEDICATIONS) as? List<Map<String, Any?>>).orEmpty()
                    .map {
                        Medication(
                            name = it["name"] as? String ?: "",
                            dosage = it["dosage"] as? String ?: "",
                            timing = it["timing"] as? String ?: "",
                        )
                    }
                @Suppress("UNCHECKED_CAST")
                val goals = (snap.get(CarePlans.ACTIVITY_GOALS) as? List<String>).orEmpty()
                CarePlan(
                    patientId = patientId,
                    medications = meds,
                    activityGoals = goals,
                    note = snap.getString(CarePlans.NOTE),
                )
            }
        }
    }

    override suspend fun getRecentLogs(patientId: String, limit: Int): Resource<List<DailyLog>> =
        withContext(io) {
            resourceOf {
                val snap = logsCollection(patientId)
                    .orderBy(CarePlans.LOG_DATE, Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                snap.documents.mapNotNull { doc ->
                    val date = doc.getString(CarePlans.LOG_DATE) ?: return@mapNotNull null
                    DailyLog(
                        date = date,
                        sleepHours = doc.getDouble(CarePlans.LOG_SLEEP_HOURS) ?: 0.0,
                        activityLevel = ActivityLevel.fromWire(doc.getString(CarePlans.LOG_ACTIVITY_LEVEL)),
                        mood = Mood.fromWire(doc.getString(CarePlans.LOG_MOOD)),
                    )
                }
            }
        }

    override suspend fun submitDailyLog(patientId: String, log: DailyLog): Resource<Unit> =
        withContext(io) {
            resourceOf {
                val data = mapOf(
                    CarePlans.LOG_DATE to log.date,
                    CarePlans.LOG_SLEEP_HOURS to log.sleepHours,
                    CarePlans.LOG_ACTIVITY_LEVEL to log.activityLevel.wireValue,
                    CarePlans.LOG_MOOD to log.mood.wireValue,
                    CarePlans.LOG_CREATED_AT to FieldValue.serverTimestamp(),
                )
                // Document id = date so re-submitting the same day overwrites it.
                logsCollection(patientId).document(log.date).set(data).await()
                Unit
            }
        }

    override suspend fun submitPendingApproval(
        patientId: String,
        suggestion: CareSuggestion,
    ): Resource<Unit> = withContext(io) {
        resourceOf {
            val data = mapOf(
                PendingApprovals.PATIENT_ID to patientId,
                PendingApprovals.MESSAGE to suggestion.message,
                PendingApprovals.TYPE to "medication",
                PendingApprovals.STATUS to "pending",
                PendingApprovals.CREATED_AT to FieldValue.serverTimestamp(),
            )
            firestore.collection(PendingApprovals.COLLECTION).add(data).await()
            Unit
        }
    }

    private fun demoCarePlan(patientId: String) = CarePlan(
        patientId = patientId,
        medications = listOf(
            Medication("Amoxicillin", "500 mg", "After breakfast"),
            Medication("Vitamin D", "1 tablet", "With lunch"),
        ),
        activityGoals = listOf("30-minute walk", "8 glasses of water", "Sleep by 11 PM"),
        note = "Follow up in two weeks. Keep hydrated and monitor your temperature.",
    )
}
