package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.AiConsultationRecord
import com.medfusion.ai.domain.model.PatientPassport
import com.medfusion.ai.domain.model.TimelineEvent

/**
 * Owns the Patient Passport (Phase 5): the patient's permanent medical identity,
 * their stored AI consultation history, and the automatic medical timeline that
 * every module contributes to.
 */
interface PassportRepository {

    /** The patient's passport; a blank passport (never an error) when unset. */
    suspend fun getPassport(patientId: String): Resource<PatientPassport>

    /** Persists profile / medical-history edits made by the patient. */
    suspend fun savePassport(passport: PatientPassport): Resource<Unit>

    /** Stored AI consultations, newest first. */
    suspend fun getAiHistory(patientId: String, limit: Int = 10): Resource<List<AiConsultationRecord>>

    /** Appends a completed AI symptom consultation to the passport. */
    suspend fun addAiConsultation(patientId: String, record: AiConsultationRecord): Resource<Unit>

    /** The medical timeline, newest first. */
    suspend fun getTimeline(patientId: String, limit: Int = 50): Resource<List<TimelineEvent>>

    /** Appends an automatic timeline event (modules call this; never the user). */
    suspend fun addTimelineEvent(patientId: String, event: TimelineEvent): Resource<Unit>
}
