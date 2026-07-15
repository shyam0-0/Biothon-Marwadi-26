package com.medfusion.ai.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.CarePlans
import com.medfusion.ai.data.firebase.FirestoreSchema.PendingApprovals
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CarePlanSource
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

    // A care plan exists only after doctor approval or an accepted AI wellness
    // plan (Phase 3) — so a missing document returns null, not a default.
    override suspend fun getCarePlan(patientId: String): Resource<CarePlan?> = withContext(io) {
        resourceOf {
            val snap = planDoc(patientId).get().await()
            if (!snap.exists()) {
                null
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
                @Suppress("UNCHECKED_CAST")
                val recovery = (snap.get(CarePlans.RECOVERY_GOALS) as? List<String>).orEmpty()
                @Suppress("UNCHECKED_CAST")
                val lifestyle = (snap.get(CarePlans.LIFESTYLE) as? List<String>).orEmpty()
                CarePlan(
                    patientId = patientId,
                    medications = meds,
                    activityGoals = goals,
                    note = snap.getString(CarePlans.NOTE),
                    diagnosis = snap.getString(CarePlans.DIAGNOSIS),
                    doctorName = snap.getString(CarePlans.DOCTOR_NAME),
                    recoveryGoals = recovery,
                    lifestyle = lifestyle,
                    hydration = snap.getString(CarePlans.HYDRATION),
                    exercise = snap.getString(CarePlans.EXERCISE),
                    sleep = snap.getString(CarePlans.SLEEP),
                    followUpDate = snap.getString(CarePlans.FOLLOW_UP_DATE),
                    source = CarePlanSource.fromWire(snap.getString(CarePlans.SOURCE)),
                )
            }
        }
    }

    override suspend fun saveCarePlan(plan: CarePlan): Resource<Unit> = withContext(io) {
        resourceOf {
            val medications = plan.medications.map {
                mapOf("name" to it.name, "dosage" to it.dosage, "timing" to it.timing)
            }
            val data = mapOf(
                CarePlans.PATIENT_ID to plan.patientId,
                CarePlans.MEDICATIONS to medications,
                CarePlans.ACTIVITY_GOALS to plan.activityGoals,
                CarePlans.NOTE to plan.note,
                CarePlans.DIAGNOSIS to plan.diagnosis,
                CarePlans.DOCTOR_NAME to plan.doctorName,
                CarePlans.RECOVERY_GOALS to plan.recoveryGoals,
                CarePlans.LIFESTYLE to plan.lifestyle,
                CarePlans.HYDRATION to plan.hydration,
                CarePlans.EXERCISE to plan.exercise,
                CarePlans.SLEEP to plan.sleep,
                CarePlans.FOLLOW_UP_DATE to plan.followUpDate,
                CarePlans.SOURCE to plan.source.wireValue,
            )
            planDoc(plan.patientId).set(data).await()
            Unit
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
                        painLevel = (doc.getLong(CarePlans.LOG_PAIN_LEVEL) ?: 0).toInt(),
                        currentSymptoms = doc.getString(CarePlans.LOG_CURRENT_SYMPTOMS).orEmpty(),
                        medicationTaken = doc.getBoolean(CarePlans.LOG_MEDICATION_TAKEN) ?: false,
                        temperature = doc.getDouble(CarePlans.LOG_TEMPERATURE),
                        notes = doc.getString(CarePlans.LOG_NOTES).orEmpty(),
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
                    CarePlans.LOG_PAIN_LEVEL to log.painLevel,
                    CarePlans.LOG_CURRENT_SYMPTOMS to log.currentSymptoms.trim(),
                    CarePlans.LOG_MEDICATION_TAKEN to log.medicationTaken,
                    CarePlans.LOG_TEMPERATURE to log.temperature,
                    CarePlans.LOG_NOTES to log.notes.trim(),
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
}
