package com.medfusion.ai.domain.model

/**
 * Confidence band for the fusion model's result (Phase 4/5). Derived from a
 * numeric confidence score so the UI shows a friendly Low/Moderate/High chip.
 */
enum class ConfidenceLevel(val wireValue: String, val label: String) {
    LOW("low", "Low"),
    MODERATE("moderate", "Moderate"),
    HIGH("high", "High");

    companion object {
        fun fromWire(value: String?): ConfidenceLevel =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
                ?: MODERATE

        /** Map a 0.0–1.0 score to a band when the backend sends a number. */
        fun fromScore(score: Double): ConfidenceLevel = when {
            score >= 0.75 -> HIGH
            score >= 0.5 -> MODERATE
            else -> LOW
        }
    }
}
