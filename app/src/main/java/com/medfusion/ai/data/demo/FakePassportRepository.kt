package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.domain.model.TimelineEvent
import com.medfusion.ai.domain.model.TimelineEventType
import com.medfusion.ai.domain.repository.PassportRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory Patient Passport, AI history and timeline for Demo Mode. */
@Singleton
class FakePassportRepository @Inject constructor() : PassportRepository {

    private val passports = ConcurrentHashMap<String, PatientPassport>()
    private val aiHistory = ConcurrentHashMap<String, CopyOnWriteArrayList<AiConsultationRecord>>()
    private val timelines = ConcurrentHashMap<String, CopyOnWriteArrayList<TimelineEvent>>()

    init {
        // Seed a filled-in passport + a timeline start so the Phase 5 screens are
        // demonstrable on first launch.
        passports[DemoData.PATIENT_ID] = PatientPassport(
            patientId = DemoData.PATIENT_ID,
            fullName = DemoData.PATIENT_NAME,
            age = 32,
            gender = "Male",
            bloodGroup = "O+",
            heightCm = 175.0,
            weightKg = 72.0,
            contactNumber = "+1-202-555-0117",
            emergencyContact = "+1-202-555-0100",
            allergies = listOf("Penicillin"),
            chronicDiseases = emptyList(),
            currentMedications = emptyList(),
            previousDiagnoses = listOf("Seasonal allergic rhinitis (2024)"),
            previousSurgeries = emptyList(),
            vaccinations = listOf("COVID-19 (full)", "Tetanus (2023)"),
        )
        timelines.getOrPut(DemoData.PATIENT_ID) { CopyOnWriteArrayList() }.add(
            TimelineEvent(
                id = UUID.randomUUID().toString(),
                type = TimelineEventType.AI_ANALYSIS,
                title = "AI Symptom Analysis",
                detail = "Persistent cough for the last week, worse at night.",
                dateMillis = System.currentTimeMillis() - 24 * 60 * 60 * 1000L,
            )
        )
    }

    override suspend fun getPassport(patientId: String): Resource<PatientPassport> =
        Resource.Success(passports[patientId] ?: PatientPassport(patientId = patientId))

    override suspend fun savePassport(passport: PatientPassport): Resource<Unit> {
        passports[passport.patientId] = passport
        return Resource.Success(Unit)
    }

    override suspend fun getAiHistory(
        patientId: String,
        limit: Int,
    ): Resource<List<AiConsultationRecord>> = Resource.Success(
        aiHistory[patientId].orEmpty().sortedByDescending { it.dateMillis }.take(limit)
    )

    override suspend fun addAiConsultation(
        patientId: String,
        record: AiConsultationRecord,
    ): Resource<Unit> {
        aiHistory.getOrPut(patientId) { CopyOnWriteArrayList() }
            .add(record.copy(id = record.id.ifBlank { UUID.randomUUID().toString() }))
        return Resource.Success(Unit)
    }

    override suspend fun getTimeline(patientId: String, limit: Int): Resource<List<TimelineEvent>> =
        Resource.Success(
            timelines[patientId].orEmpty().sortedByDescending { it.dateMillis }.take(limit)
        )

    override suspend fun addTimelineEvent(patientId: String, event: TimelineEvent): Resource<Unit> {
        timelines.getOrPut(patientId) { CopyOnWriteArrayList() }
            .add(event.copy(id = event.id.ifBlank { UUID.randomUUID().toString() }))
        return Resource.Success(Unit)
    }
}
