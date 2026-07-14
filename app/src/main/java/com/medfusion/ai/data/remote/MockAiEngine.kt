package com.medfusion.ai.data.remote

import com.medfusion.ai.data.remote.dto.TriageResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Deterministic, on-device stand-in for the FastAPI AI endpoints. Used only as a
 * fallback in debug builds (see [com.medfusion.ai.BuildConfig.USE_MOCK_AI_FALLBACK])
 * so the entire patient journey can be demonstrated before the backend is live.
 *
 * The heuristics are intentionally simple keyword rules — not clinical logic —
 * and never ship as the source of truth in a release build.
 */
@Singleton
class MockAiEngine @Inject constructor() {

    private data class Rule(
        val keywords: List<String>,
        val test: String,
        val urgency: String,
    )

    // Ordered by descending urgency so the first match wins for red-flag symptoms.
    private val rules = listOf(
        Rule(listOf("chest pain", "shortness of breath", "breathing", "faint", "collapse"),
            "Chest X-ray + ECG", "red"),
        Rule(listOf("blood", "severe", "unbearable", "high fever", "confusion"),
            "Urgent blood panel", "red"),
        Rule(listOf("persistent cough", "cough", "wheeze", "phlegm"),
            "Chest X-ray", "yellow"),
        Rule(listOf("fracture", "swelling", "injury", "sprain", "bone"),
            "X-ray of affected area", "yellow"),
        Rule(listOf("fatigue", "tired", "headache", "dizzy", "nausea"),
            "Complete Blood Count (CBC)", "yellow"),
    )

    fun triage(symptomsText: String, caseId: String): TriageResponse {
        val text = symptomsText.lowercase()
        val match = rules.firstOrNull { rule -> rule.keywords.any { text.contains(it) } }
        return if (match != null) {
            TriageResponse(caseId = caseId, recommendedTest = match.test, urgencyLevel = match.urgency)
        } else {
            TriageResponse(
                caseId = caseId,
                recommendedTest = "General health check-up",
                urgencyLevel = "green",
            )
        }
    }

    /**
     * Mock fusion analysis (Phase 4). Produces a plausible findings summary and
     * confidence/risk scores derived from the symptoms and the presence of reports.
     */
    fun analyze(symptomsText: String, hasXray: Boolean, hasLabReport: Boolean): MockFusion {
        val urgency = triage(symptomsText, caseId = "tmp").urgencyLevel
        val base = when (urgency) {
            "red" -> 0.82 to 0.78
            "yellow" -> 0.68 to 0.45
            else -> 0.6 to 0.2
        }
        // More modalities → higher confidence.
        val modalities = (if (hasXray) 1 else 0) + (if (hasLabReport) 1 else 0)
        val confidence = min(0.95, base.first + modalities * 0.06)
        val risk = base.second

        val findings = buildString {
            append("Based on the reported symptoms")
            if (hasXray) append(", the uploaded imaging")
            if (hasLabReport) append(" and the lab report")
            append(", the model highlights patterns that warrant ")
            append(
                when (urgency) {
                    "red" -> "prompt clinical review. Key indicators appear outside typical ranges."
                    "yellow" -> "a follow-up consultation. Some indicators are mildly atypical."
                    else -> "routine monitoring. Indicators largely fall within expected ranges."
                }
            )
        }
        return MockFusion(findings = findings, confidenceScore = confidence, riskScore = risk)
    }

    data class MockFusion(val findings: String, val confidenceScore: Double, val riskScore: Double)
}
