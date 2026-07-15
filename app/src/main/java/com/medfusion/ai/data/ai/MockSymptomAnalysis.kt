package com.medfusion.ai.data.ai

import com.medfusion.ai.data.remote.MockAiEngine
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CarePlanSource
import com.medfusion.ai.domain.model.ConditionProbability
import com.medfusion.ai.domain.model.DailyLog
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Mood
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.medfusion.ai.domain.model.Severity
import com.medfusion.ai.domain.model.SymptomAnalysis
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic offline stand-in for [GeminiService]. Used only when the Gemini
 * key is absent or the network fails in a demo/mock build, so the full symptom
 * flow stays demonstrable. Reuses [MockAiEngine]'s keyword heuristics.
 */
@Singleton
class MockSymptomAnalysisProvider @Inject constructor(
    private val mockAiEngine: MockAiEngine,
) {
    fun analyze(symptoms: String, hasAttachments: Boolean): SymptomAnalysis {
        val triage = mockAiEngine.triage(symptoms, caseId = "mock")
        val severity = when (triage.urgencyLevel) {
            "red" -> Severity.HIGH
            "yellow" -> Severity.MODERATE
            else -> Severity.LOW
        }
        val text = symptoms.lowercase()
        val conditions = when {
            text.contains("cough") || text.contains("breath") -> listOf(
                ConditionProbability("Acute bronchitis", 54),
                ConditionProbability("Common cold", 26),
                ConditionProbability("Pneumonia", 14),
                ConditionProbability("Other", 6),
            )
            text.contains("fever") || text.contains("headache") -> listOf(
                ConditionProbability("Viral infection", 58),
                ConditionProbability("Influenza", 24),
                ConditionProbability("Other", 18),
            )
            else -> listOf(
                ConditionProbability("Non-specific viral illness", 60),
                ConditionProbability("Other", 40),
            )
        }
        val confidenceBoost = if (hasAttachments) " Uploaded reports were reviewed and factored in." else ""
        return SymptomAnalysis(
            summary = "Based on your description, your symptoms are most consistent with a common, " +
                "usually self-limiting condition.$confidenceBoost This is preliminary guidance, not a diagnosis.",
            conditions = conditions,
            severity = severity,
            emergencyMessage = null,
            recommendedSpecialists = listOf("General Physician") +
                if (text.contains("cough") || text.contains("breath")) listOf("Pulmonologist") else emptyList(),
            recommendedTests = listOf("Complete Blood Count (CBC)"),
            recommendedScans = if (text.contains("cough") || text.contains("chest"))
                listOf("Chest X-ray") else emptyList(),
            homeCare = listOf("Rest and stay hydrated", "Monitor your temperature"),
            precautions = listOf("Avoid strenuous activity", "Isolate if you have a fever"),
            redFlags = listOf("Difficulty breathing", "Chest pain", "Persistent high fever"),
            reportRecommendation = if (!hasAttachments && (text.contains("cough") || text.contains("chest")))
                "A chest X-ray may improve the analysis if available." else null,
            consultationRecommended = severity != Severity.LOW,
        )
    }

    /** Heuristic recovery-progress summary from recent check-ins. */
    fun progress(logs: List<DailyLog>): ProgressAnalysis {
        val recent = logs.sortedByDescending { it.date }
        val latest = recent.first()
        val previous = recent.getOrNull(1)
        val compliance = recent.take(3).count { it.medicationTaken }
        val complianceLow = recent.size >= 2 && compliance <= recent.take(3).size / 2

        return when {
            previous != null && (latest.painLevel > previous.painLevel + 1 ||
                (latest.mood == Mood.POOR && previous.mood != Mood.POOR)) ->
                ProgressAnalysis(
                    "Symptoms worsening",
                    "Your recent check-ins show increasing discomfort. A follow-up with your doctor is recommended.",
                    followUpRecommended = true,
                )
            complianceLow ->
                ProgressAnalysis(
                    "Medication compliance decreasing",
                    "You've missed some doses recently. Staying consistent will help your recovery.",
                    followUpRecommended = false,
                )
            previous != null && (latest.painLevel < previous.painLevel ||
                latest.sleepHours >= previous.sleepHours) ->
                ProgressAnalysis(
                    "Recovery improving",
                    "Your check-ins show a positive trend. Keep following your care plan.",
                    followUpRecommended = false,
                )
            else ->
                ProgressAnalysis(
                    "Recovery stable",
                    "Your condition looks steady. Continue your care plan and daily check-ins.",
                    followUpRecommended = false,
                )
        }
    }

    /** Generic AI wellness plan for a minor concern (patientId filled by caller). */
    fun wellnessPlan(): CarePlan = CarePlan(
        patientId = "",
        medications = listOf(
            Medication("Paracetamol", "500 mg", "If fever/pain, after food"),
        ),
        activityGoals = listOf("Light 20-minute walk", "Balanced meals"),
        recoveryGoals = listOf("Feel better within 5–7 days", "No worsening symptoms"),
        lifestyle = listOf("Rest adequately", "Avoid smoking and alcohol"),
        hydration = "Drink 8–10 glasses of water daily",
        exercise = "Gentle stretching; avoid strenuous activity",
        sleep = "Aim for 7–8 hours of sleep",
        note = "This is a self-care wellness plan for minor symptoms. See a doctor if symptoms persist or worsen.",
        source = CarePlanSource.AI_WELLNESS,
    )
}
