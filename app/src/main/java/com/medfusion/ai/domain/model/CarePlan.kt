package com.medfusion.ai.domain.model

/**
 * A dynamic care plan (Phase 3), created only after doctor approval or when a
 * patient accepts an AI wellness plan. Stored in "care_plans" keyed by patientId.
 * [source] records how it came to exist.
 */
data class CarePlan(
    val patientId: String,
    val medications: List<Medication>,
    val activityGoals: List<String>,
    val note: String? = null,
    val diagnosis: String? = null,
    val doctorName: String? = null,
    val recoveryGoals: List<String> = emptyList(),
    val lifestyle: List<String> = emptyList(),
    val hydration: String? = null,
    val exercise: String? = null,
    val sleep: String? = null,
    val followUpDate: String? = null,
    val source: CarePlanSource = CarePlanSource.DOCTOR,
) {
    /** Medicine reminders derived from each medication's timing. */
    val medicineReminders: List<String>
        get() = medications.map { "${it.name} — ${it.dosage} • ${it.timing}" }
}

enum class CarePlanSource(val wireValue: String) {
    DOCTOR("doctor"),
    AI_WELLNESS("ai_wellness");

    companion object {
        fun fromWire(value: String?): CarePlanSource =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) } ?: DOCTOR
    }
}

/** Gemini's assessment of recovery from historical check-ins (Phase 3). */
data class ProgressAnalysis(
    val status: String,          // e.g. "Recovery improving"
    val summary: String,
    val followUpRecommended: Boolean,
    /** Why a follow-up is (or isn't) recommended — Smart Follow-up (Phase 5). */
    val followUpReason: String? = null,
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
    val painLevel: Int = 0,          // 0–10
    val currentSymptoms: String = "",
    val medicationTaken: Boolean = false,
    val temperature: Double? = null, // °F
    val notes: String = "",
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
