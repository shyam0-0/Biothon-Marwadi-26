package com.medfusion.ai.domain.model

/**
 * Clinical urgency returned by triage (Phase 2) and reused to sort the doctor's
 * queue (Phase 7/8). Ordinal ordering (RED highest) drives the smart queue sort.
 */
enum class UrgencyLevel(val wireValue: String, val priority: Int, val label: String) {
    RED("red", 3, "Urgent"),
    YELLOW("yellow", 2, "Moderate"),
    GREEN("green", 1, "Routine");

    companion object {
        /** Lenient parse from backend/Firestore string; defaults to YELLOW. */
        fun fromWire(value: String?): UrgencyLevel =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
                ?: YELLOW
    }
}
