package com.medfusion.ai.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.Passports
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.ConditionProbability
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.domain.model.Severity
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.model.TimelineEventType
import com.medfusion.ai.domain.repository.PassportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Firestore-backed Patient Passport, AI history and timeline (Phase 5). */
@Singleton
class PassportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PassportRepository {

    private fun passportDoc(patientId: String) =
        firestore.collection(Passports.COLLECTION).document(patientId)

    override suspend fun getPassport(patientId: String): Resource<PatientPassport> =
        withContext(io) {
            resourceOf {
                val snap = passportDoc(patientId).get().await()
                if (!snap.exists()) {
                    PatientPassport(patientId = patientId)
                } else {
                    PatientPassport(
                        patientId = patientId,
                        fullName = snap.getString(Passports.FULL_NAME).orEmpty(),
                        age = snap.getLong(Passports.AGE)?.toInt(),
                        gender = snap.getString(Passports.GENDER).orEmpty(),
                        bloodGroup = snap.getString(Passports.BLOOD_GROUP).orEmpty(),
                        heightCm = snap.getDouble(Passports.HEIGHT_CM),
                        weightKg = snap.getDouble(Passports.WEIGHT_KG),
                        photoUrl = snap.getString(Passports.PHOTO_URL),
                        contactNumber = snap.getString(Passports.CONTACT_NUMBER).orEmpty(),
                        emergencyContact = snap.getString(Passports.EMERGENCY_CONTACT).orEmpty(),
                        allergies = snap.stringList(Passports.ALLERGIES),
                        chronicDiseases = snap.stringList(Passports.CHRONIC_DISEASES),
                        currentMedications = snap.stringList(Passports.CURRENT_MEDICATIONS),
                        previousDiagnoses = snap.stringList(Passports.PREVIOUS_DIAGNOSES),
                        previousSurgeries = snap.stringList(Passports.PREVIOUS_SURGERIES),
                        vaccinations = snap.stringList(Passports.VACCINATIONS),
                        smoker = snap.getBoolean(Passports.SMOKER) ?: false,
                        alcohol = snap.getBoolean(Passports.ALCOHOL) ?: false,
                        pregnant = snap.getBoolean(Passports.PREGNANT) ?: false,
                    )
                }
            }
        }

    override suspend fun savePassport(passport: PatientPassport): Resource<Unit> = withContext(io) {
        resourceOf {
            val data = mapOf(
                Passports.FULL_NAME to passport.fullName,
                Passports.AGE to passport.age,
                Passports.GENDER to passport.gender,
                Passports.BLOOD_GROUP to passport.bloodGroup,
                Passports.HEIGHT_CM to passport.heightCm,
                Passports.WEIGHT_KG to passport.weightKg,
                Passports.PHOTO_URL to passport.photoUrl,
                Passports.CONTACT_NUMBER to passport.contactNumber,
                Passports.EMERGENCY_CONTACT to passport.emergencyContact,
                Passports.ALLERGIES to passport.allergies,
                Passports.CHRONIC_DISEASES to passport.chronicDiseases,
                Passports.CURRENT_MEDICATIONS to passport.currentMedications,
                Passports.PREVIOUS_DIAGNOSES to passport.previousDiagnoses,
                Passports.PREVIOUS_SURGERIES to passport.previousSurgeries,
                Passports.VACCINATIONS to passport.vaccinations,
                Passports.SMOKER to passport.smoker,
                Passports.ALCOHOL to passport.alcohol,
                Passports.PREGNANT to passport.pregnant,
                Passports.UPDATED_AT to FieldValue.serverTimestamp(),
            )
            passportDoc(passport.patientId).set(data).await()
            Unit
        }
    }

    override suspend fun getAiHistory(
        patientId: String,
        limit: Int,
    ): Resource<List<AiConsultationRecord>> = withContext(io) {
        resourceOf {
            val snap = passportDoc(patientId).collection(Passports.AI_HISTORY)
                .orderBy(Passports.AI_DATE, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snap.documents.map { doc ->
                @Suppress("UNCHECKED_CAST")
                val conditions = (doc.get(Passports.AI_CONDITIONS) as? List<Map<String, Any?>>)
                    .orEmpty()
                    .map {
                        ConditionProbability(
                            name = it["name"] as? String ?: "",
                            confidence = (it["confidence"] as? Number)?.toInt() ?: 0,
                            reason = it["reason"] as? String ?: "",
                        )
                    }
                @Suppress("UNCHECKED_CAST")
                val tests = (doc.get(Passports.AI_TESTS) as? List<String>).orEmpty()
                AiConsultationRecord(
                    id = doc.id,
                    dateMillis = doc.getLong(Passports.AI_DATE) ?: 0L,
                    symptoms = doc.getString(Passports.AI_SYMPTOMS).orEmpty(),
                    summary = doc.getString(Passports.AI_SUMMARY).orEmpty(),
                    conditions = conditions,
                    severity = Severity.fromWire(doc.getString(Passports.AI_SEVERITY)),
                    recommendedTests = tests,
                    recommendedSpecialist = doc.getString(Passports.AI_SPECIALIST),
                    locations = doc.stringList(Passports.AI_LOCATIONS),
                )
            }
        }
    }

    override suspend fun addAiConsultation(
        patientId: String,
        record: AiConsultationRecord,
    ): Resource<Unit> = withContext(io) {
        resourceOf {
            val data = mapOf(
                Passports.AI_DATE to record.dateMillis,
                Passports.AI_SYMPTOMS to record.symptoms,
                Passports.AI_SUMMARY to record.summary,
                Passports.AI_CONDITIONS to record.conditions.map {
                    mapOf("name" to it.name, "confidence" to it.confidence, "reason" to it.reason)
                },
                Passports.AI_SEVERITY to record.severity.wireValue,
                Passports.AI_TESTS to record.recommendedTests,
                Passports.AI_SPECIALIST to record.recommendedSpecialist,
                Passports.AI_LOCATIONS to record.locations,
            )
            passportDoc(patientId).collection(Passports.AI_HISTORY).add(data).await()
            Unit
        }
    }

    override suspend fun getTimeline(patientId: String, limit: Int): Resource<List<TimelineEvent>> =
        withContext(io) {
            resourceOf {
                val snap = passportDoc(patientId).collection(Passports.TIMELINE)
                    .orderBy(Passports.EVENT_DATE, Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                snap.documents.map { doc ->
                    TimelineEvent(
                        id = doc.id,
                        type = TimelineEventType.fromWire(doc.getString(Passports.EVENT_TYPE)),
                        title = doc.getString(Passports.EVENT_TITLE).orEmpty(),
                        detail = doc.getString(Passports.EVENT_DETAIL).orEmpty(),
                        dateMillis = doc.getLong(Passports.EVENT_DATE) ?: 0L,
                    )
                }
            }
        }

    override suspend fun addTimelineEvent(patientId: String, event: TimelineEvent): Resource<Unit> =
        withContext(io) {
            resourceOf {
                val data = mapOf(
                    Passports.EVENT_TYPE to event.type.wireValue,
                    Passports.EVENT_TITLE to event.title,
                    Passports.EVENT_DETAIL to event.detail,
                    Passports.EVENT_DATE to event.dateMillis,
                )
                passportDoc(patientId).collection(Passports.TIMELINE).add(data).await()
                Unit
            }
        }
}

@Suppress("UNCHECKED_CAST")
private fun com.google.firebase.firestore.DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<String>).orEmpty()
