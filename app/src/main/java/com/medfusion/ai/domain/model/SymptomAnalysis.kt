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
    val recommendedSpecialists: List<String>,
    val recommendedTests: List<String>,
    val recommendedScans: List<String>,
    val homeCare: List<String>,
    val precautions: List<String>,
    val redFlags: List<String>,
    /** e.g. "A chest X-ray may improve analysis if available." Null if none. */
    val reportRecommendation: String?,
    val consultationRecommended: Boolean,
)

/** A candidate condition with an estimated confidence percentage (0–100). */
data class ConditionProbability(
    val name: String,
    val confidence: Int,
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
