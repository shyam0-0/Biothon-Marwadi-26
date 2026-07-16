package com.medfusion.ai.domain.model

/**
 * The patient's permanent medical identity inside MedFusion AI (Phase 5). Every
 * module reads from and contributes to this passport: symptom analysis stores AI
 * history, consultations add diagnoses, and the timeline records the journey.
 * Stored in the "patient_passports" collection keyed by [patientId].
 */
data class PatientPassport(
    val patientId: String,
    // Personal information
    val fullName: String = "",
    val age: Int? = null,
    val gender: String = "",
    val bloodGroup: String = "",
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val photoUrl: String? = null,
    val contactNumber: String = "",
    val emergencyContact: String = "",
    // Medical profile
    val allergies: List<String> = emptyList(),
    val chronicDiseases: List<String> = emptyList(),
    val currentMedications: List<String> = emptyList(),
    val previousDiagnoses: List<String> = emptyList(),
    val previousSurgeries: List<String> = emptyList(),
    val vaccinations: List<String> = emptyList(),
    // Lifestyle factors
    val smoker: Boolean = false,
    val alcohol: Boolean = false,
    val pregnant: Boolean = false,
) {
    /** BMI computed from height/weight when both are present. */
    val bmi: Double?
        get() {
            val h = heightCm ?: return null
            val w = weightKg ?: return null
            if (h <= 0.0 || w <= 0.0) return null
            val meters = h / 100.0
            return (w / (meters * meters) * 10).toInt() / 10.0
        }

    /** Risk factors surfaced on the passport dashboard. */
    val riskFactors: List<String>
        get() = buildList {
            addAll(chronicDiseases)
            allergies.takeIf { it.isNotEmpty() }?.let { add("Allergies: ${it.joinToString()}") }
            if (smoker) add("Smoking")
            if (alcohol) add("Alcohol use")
            if (pregnant) add("Pregnancy")
            bmi?.let { if (it >= 30.0) add("High BMI ($it)") }
        }

    /** True when the patient hasn't filled in any profile details yet. */
    val isEmpty: Boolean
        get() = age == null && gender.isBlank() && bloodGroup.isBlank() &&
            heightCm == null && weightKg == null && allergies.isEmpty() &&
            chronicDiseases.isEmpty() && currentMedications.isEmpty() &&
            previousDiagnoses.isEmpty() && previousSurgeries.isEmpty() &&
            vaccinations.isEmpty() && !smoker && !alcohol && !pregnant
}

/**
 * A completed AI symptom consultation, stored permanently in the passport's AI
 * history so later analyses (and the doctor) can see how the assessment evolved.
 */
data class AiConsultationRecord(
    val id: String = "",
    val dateMillis: Long = 0L,
    val symptoms: String,
    val summary: String,
    val conditions: List<ConditionProbability>,
    val severity: Severity,
    val recommendedTests: List<String>,
    val recommendedSpecialist: String?,
    /** Body-map localization summaries (Phase 5.6), e.g. "Chest: Pain, severity 7/10…". */
    val locations: List<String> = emptyList(),
)

/** The kind of event shown on the patient's medical timeline. */
enum class TimelineEventType(val wireValue: String, val label: String) {
    AI_ANALYSIS("ai_analysis", "AI Symptom Analysis"),
    REPORT_UPLOADED("report_uploaded", "Report Uploaded"),
    APPOINTMENT_BOOKED("appointment_booked", "Appointment Booked"),
    DOCTOR_CONSULTATION("doctor_consultation", "Doctor Consultation"),
    PRESCRIPTION("prescription", "Prescription Generated"),
    CARE_PLAN_STARTED("care_plan_started", "Care Plan Started"),
    DAILY_CHECK_IN("daily_check_in", "Daily Check-in"),
    FOLLOW_UP("follow_up", "Follow-up");

    companion object {
        fun fromWire(value: String?): TimelineEventType =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
                ?: AI_ANALYSIS
    }
}

/**
 * One entry on the patient's medical timeline. Events are appended automatically
 * by each module (never entered manually) so the timeline is the medical journey.
 */
data class TimelineEvent(
    val id: String = "",
    val type: TimelineEventType,
    val title: String,
    val detail: String = "",
    val dateMillis: Long,
)

/**
 * Everything the AI should know about the patient before a Gemini request
 * (Phase 5 Smart AI Context). Assembled best-effort — any part may be absent.
 */
data class PatientContext(
    val passport: PatientPassport? = null,
    val aiHistory: List<AiConsultationRecord> = emptyList(),
    val carePlan: CarePlan? = null,
    val recentLogs: List<DailyLog> = emptyList(),
    /** Latest doctor diagnosis, when one exists. */
    val latestDiagnosis: String? = null,
) {
    val isEmpty: Boolean
        get() = (passport == null || passport.isEmpty) && aiHistory.isEmpty() &&
            carePlan == null && recentLogs.isEmpty() && latestDiagnosis == null
}
