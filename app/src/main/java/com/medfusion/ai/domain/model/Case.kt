package com.medfusion.ai.domain.model

/**
 * The central aggregate of a patient's care episode. Created at triage (Phase 2),
 * enriched with uploaded reports (Phase 3) and the fusion result (Phase 4), and
 * read by the result, booking and doctor screens. Mirrors the Firestore "cases"
 * document.
 */
data class Case(
    val caseId: String,
    val userId: String,
    val symptomsText: String,
    val recommendedTest: String,
    val urgencyLevel: UrgencyLevel,
    val status: CaseStatus,
    val xrayUrl: String? = null,
    val labReportUrl: String? = null,
    val fusionResult: FusionResult? = null,
    val createdAtMillis: Long = 0L,
    /** Body-map symptom localization (Phase 5.6), shown to the doctor pre-read. */
    val symptomLocations: List<SymptomLocation> = emptyList(),
)

/** Lifecycle of a [Case] as it moves through the patient journey. */
enum class CaseStatus(val wireValue: String) {
    AWAITING_TEST("awaiting_test"),
    READY_FOR_ANALYSIS("ready_for_analysis"),
    ANALYZED("analyzed");

    companion object {
        fun fromWire(value: String?): CaseStatus =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
                ?: AWAITING_TEST
    }
}
