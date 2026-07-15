package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.SymptomAnalysis

/**
 * Owns the "cases" aggregate across the patient journey. Grows across phases:
 * triage (Phase 2), report attachment (Phase 3), fusion analysis (Phase 4),
 * and reads for the result/booking/doctor screens.
 */
interface CaseRepository {

    /**
     * Runs symptom triage for the signed-in patient: calls the backend (with a
     * debug mock fallback), then creates the Firestore "cases" document and
     * returns the resulting [Case] with status = AWAITING_TEST.
     */
    suspend fun runTriage(symptomsText: String, language: String = ""): Resource<Case>

    /** Loads a single case by id. */
    suspend fun getCase(caseId: String): Resource<Case>

    /**
     * Attaches uploaded report URLs to an existing case and moves it to
     * READY_FOR_ANALYSIS. Updates the existing document — never creates a new one.
     */
    suspend fun attachReports(
        caseId: String,
        xrayUrl: String?,
        labReportUrl: String?,
    ): Resource<Case>

    /**
     * Runs fusion analysis for a case: pulls the stored symptoms + report URLs,
     * calls the backend (with debug mock fallback), stores the result under
     * "fusionResult", moves status to ANALYZED, and returns the updated case.
     */
    suspend fun runAnalysis(caseId: String, language: String = ""): Resource<Case>

    /**
     * Persists a completed AI symptom analysis (Phase 1) as a case so that a
     * chosen consultation carries the symptoms, urgency and AI summary into the
     * booking flow and the doctor's pre-read. Called only when the patient
     * decides to consult — never automatically.
     */
    suspend fun createCaseFromAnalysis(symptoms: String, analysis: SymptomAnalysis): Resource<Case>
}
