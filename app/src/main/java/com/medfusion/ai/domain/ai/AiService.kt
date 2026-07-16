package com.medfusion.ai.domain.ai

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.PatientContext
import com.medfusion.ai.domain.model.PatientExplanation
import com.medfusion.ai.domain.model.Prescription
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.ReportAttachment
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.SymptomLocation

/**
 * Abstraction over the app's AI provider. The rest of the app depends only on
 * this interface — it never talks to Gemini (or any model) directly. Every method
 * returns a strongly-typed, validated result (never raw text).
 */
interface AiService {

    /**
     * Analyzes the patient's symptoms and, optionally, any attached medical
     * reports/images to refine the assessment.
     *
     * @param attachments optional; when present the model summarizes their
     *   findings and updates condition confidence. Never required.
     * @param patientContext the Smart AI Context (Phase 5): passport, previous AI
     *   consultations, care plan and recent check-ins, so the AI understands the
     *   patient's history instead of treating every request as a new patient.
     */
    suspend fun analyzeSymptoms(
        symptoms: String,
        language: String,
        attachments: List<ReportAttachment> = emptyList(),
        patientContext: PatientContext? = null,
        locations: List<SymptomLocation> = emptyList(),
    ): Resource<SymptomAnalysis>

    /**
     * Translates the doctor's clinical outcome (diagnosis, prescription, care
     * plan) into a simple, accurate patient-friendly explanation (Phase 5.6).
     * The clinical record itself is never modified.
     */
    suspend fun explainForPatient(
        prescription: Prescription,
        carePlan: CarePlan?,
        language: String,
    ): Resource<PatientExplanation>

    /** Analyzes historical daily check-ins into a recovery-progress summary (Phase 3). */
    suspend fun analyzeProgress(
        logs: List<DailyLog>,
        diagnosis: String?,
        language: String,
    ): Resource<ProgressAnalysis>

    /**
     * Generates an AI wellness plan for a minor concern that the patient may
     * accept. The returned plan has a blank patientId for the caller to fill.
     */
    suspend fun generateWellnessPlan(concern: String, language: String): Resource<CarePlan>
}
