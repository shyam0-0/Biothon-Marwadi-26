package com.medfusion.ai.domain.model

/**
 * Structured result of an AI symptom consultation (Phase 1). Produced only from
 * a validated JSON response — never from free-text parsing. This is decision
 * support, not a diagnosis; the UI always shows the medical disclaimer.
 */
data class SymptomAnalysis(
    val summary: String,
    val conditions: List<ConditionProbability>,
    val severity: Severity,
    val emergencyMessage: String?,
    val recommendedSpecialists: List<SpecialistRecommendation>,
    val recommendedTests: List<TestRecommendation>,
    val recommendedScans: List<String>,
    val homeCare: List<String>,
    val precautions: List<String>,
    val redFlags: List<String>,
    /** e.g. "A chest X-ray may improve analysis if available." Null if none. */
    val reportRecommendation: String?,
    val consultationRecommended: Boolean,
    /** Why confidence changed vs. earlier analyses/reports (Phase 5). Null on a first pass. */
    val confidenceExplanation: String? = null,
    /** Structured findings from uploaded reports (Phase 5). Null when none uploaded. */
    val reportInsights: ReportInsights? = null,
)

/** A candidate condition with an estimated confidence percentage (0–100). */
data class ConditionProbability(
    val name: String,
    val confidence: Int,
    /** Short explanation of why this condition is suspected (Phase 5). */
    val reason: String = "",
)

/** A specialist chosen from the predicted conditions + symptoms, with the WHY (Phase 5). */
data class SpecialistRecommendation(
    val name: String,
    val reason: String = "",
)

/** A symptom-driven test recommendation with priority and rationale (Phase 5). */
data class TestRecommendation(
    val name: String,
    val priority: TestPriority = TestPriority.RECOMMENDED,
    val reason: String = "",
)

enum class TestPriority(val wireValue: String, val label: String) {
    REQUIRED("required", "Required"),
    RECOMMENDED("recommended", "Recommended"),
    OPTIONAL("optional", "Optional");

    companion object {
        fun fromWire(value: String?): TestPriority =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
                ?: RECOMMENDED
    }
}

/** What the AI extracted from uploaded medical reports (Phase 5). */
data class ReportInsights(
    val summary: String,
    val abnormalValues: List<String> = emptyList(),
    val concerns: List<String> = emptyList(),
    /** How the report affects the assessment — or why it doesn't contribute. */
    val relevance: String? = null,
)

/** AI-estimated urgency of the presentation. */
enum class Severity(val wireValue: String, val label: String) {
    LOW("low", "Low"),
    MODERATE("moderate", "Moderate"),
    HIGH("high", "High"),
    EMERGENCY("emergency", "Emergency");

    /** Maps to the app-wide [UrgencyLevel] used by booking and the doctor queue. */
    fun toUrgency(): UrgencyLevel = when (this) {
        EMERGENCY, HIGH -> UrgencyLevel.RED
        MODERATE -> UrgencyLevel.YELLOW
        LOW -> UrgencyLevel.GREEN
    }

    companion object {
        fun fromWire(value: String?): Severity =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) } ?: MODERATE
    }
}

/** An optional medical file the patient attaches to improve the analysis. */
data class ReportAttachment(
    val uri: android.net.Uri,
    val mimeType: String,
    val displayName: String,
)
