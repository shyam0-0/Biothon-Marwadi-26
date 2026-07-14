package com.medfusion.ai.domain.model

/**
 * A doctor-set care plan (Phase 10): medication schedule + activity goals, stored
 * in the "care_plans" collection keyed by patientId.
 */
data class CarePlan(
    val patientId: String,
    val medications: List<Medication>,
    val activityGoals: List<String>,
    val note: String? = null,
)

data class Medication(
    val name: String,
    val dosage: String,
    val timing: String,
)

/** A patient's daily self-report, stored in the "daily_logs" subcollection. */
data class DailyLog(
    val date: String,            // ISO yyyy-MM-dd (also the document id)
    val sleepHours: Double,
    val activityLevel: ActivityLevel,
    val mood: Mood,
)

enum class ActivityLevel(val wireValue: String, val label: String) {
    LOW("low", "Low"),
    MODERATE("moderate", "Moderate"),
    HIGH("high", "High");

    companion object {
        fun fromWire(value: String?): ActivityLevel =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) } ?: MODERATE
    }
}

enum class Mood(val wireValue: String, val label: String) {
    GOOD("good", "Good"),
    OKAY("okay", "Okay"),
    POOR("poor", "Poor");

    companion object {
        fun fromWire(value: String?): Mood =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) } ?: OKAY
    }
}

/**
 * An adaptive suggestion produced by the rule engine. Lifestyle suggestions are
 * applied immediately; medication-related ones require doctor approval and are
 * held until then.
 */
data class CareSuggestion(
    val message: String,
    val requiresDoctorApproval: Boolean,
    val pending: Boolean = false,
)
