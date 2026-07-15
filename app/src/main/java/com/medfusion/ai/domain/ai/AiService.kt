package com.medfusion.ai.domain.ai

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.ReportAttachment
import com.medfusion.ai.domain.model.SymptomAnalysis

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
     */
    suspend fun analyzeSymptoms(
        symptoms: String,
        language: String,
        attachments: List<ReportAttachment> = emptyList(),
    ): Resource<SymptomAnalysis>

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
