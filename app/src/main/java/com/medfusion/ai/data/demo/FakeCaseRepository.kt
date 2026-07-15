package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.data.remote.MockAiEngine
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.CaseStatus
import com.medfusion.ai.domain.model.ConfidenceLevel
import com.medfusion.ai.domain.model.FusionResult
import com.medfusion.ai.domain.model.SymptomAnalysis
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CaseRepository
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory cases for Demo Mode, powered by the existing [MockAiEngine]. */
@Singleton
class FakeCaseRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val mockAi: MockAiEngine,
) : CaseRepository {

    private val cases = ConcurrentHashMap<String, Case>()

    init {
        // Seed a case so the doctor queue has a pre-read on first launch.
        val seed = Case(
            caseId = "demo-case-seed",
            userId = DemoData.PATIENT_ID,
            symptomsText = "Persistent cough for the last week, worse at night.",
            recommendedTest = "Chest X-ray",
            urgencyLevel = UrgencyLevel.YELLOW,
            status = CaseStatus.ANALYZED,
            xrayUrl = "https://images.unsplash.com/photo-1530026405186-ed1f139313f8?w=400",
            fusionResult = FusionResult(
                findings = "Imaging and symptoms suggest a mild lower-respiratory infection. " +
                    "Indicators are slightly atypical; a follow-up consultation is advised.",
                confidenceScore = 0.71,
                riskScore = 0.42,
                confidenceLevel = ConfidenceLevel.MODERATE,
            ),
        )
        cases[seed.caseId] = seed
    }

    override suspend fun runTriage(symptomsText: String, language: String): Resource<Case> {
        delay(600)
        val caseId = UUID.randomUUID().toString()
        val response = mockAi.triage(symptomsText, caseId)
        val case = Case(
            caseId = caseId,
            userId = authRepository.currentUserId() ?: DemoData.PATIENT_ID,
            symptomsText = symptomsText.trim(),
            recommendedTest = response.recommendedTest,
            urgencyLevel = UrgencyLevel.fromWire(response.urgencyLevel),
            status = CaseStatus.AWAITING_TEST,
        )
        cases[caseId] = case
        return Resource.Success(case)
    }

    override suspend fun getCase(caseId: String): Resource<Case> {
        val case = cases[caseId] ?: return Resource.Error(AppError.NotFound())
        return Resource.Success(case)
    }

    override suspend fun attachReports(
        caseId: String,
        xrayUrl: String?,
        labReportUrl: String?,
    ): Resource<Case> {
        delay(400)
        val case = cases[caseId] ?: return Resource.Error(AppError.NotFound())
        val updated = case.copy(
            xrayUrl = xrayUrl ?: case.xrayUrl,
            labReportUrl = labReportUrl ?: case.labReportUrl,
            status = CaseStatus.READY_FOR_ANALYSIS,
        )
        cases[caseId] = updated
        return Resource.Success(updated)
    }

    override suspend fun runAnalysis(caseId: String, language: String): Resource<Case> {
        delay(900)
        val case = cases[caseId] ?: return Resource.Error(AppError.NotFound())
        val mock = mockAi.analyze(
            symptomsText = case.symptomsText,
            hasXray = case.xrayUrl != null,
            hasLabReport = case.labReportUrl != null,
        )
        val updated = case.copy(
            status = CaseStatus.ANALYZED,
            fusionResult = FusionResult(
                findings = mock.findings,
                confidenceScore = mock.confidenceScore,
                riskScore = mock.riskScore,
                confidenceLevel = ConfidenceLevel.fromScore(mock.confidenceScore),
            ),
        )
        cases[caseId] = updated
        return Resource.Success(updated)
    }

    override suspend fun createCaseFromAnalysis(
        symptoms: String,
        analysis: SymptomAnalysis,
    ): Resource<Case> {
        val caseId = UUID.randomUUID().toString()
        val urgency = analysis.severity.toUrgency()
        val topConfidence = analysis.conditions.maxOfOrNull { it.confidence } ?: 0
        val case = Case(
            caseId = caseId,
            userId = authRepository.currentUserId() ?: DemoData.PATIENT_ID,
            symptomsText = symptoms.trim(),
            recommendedTest = analysis.recommendedScans.firstOrNull()
                ?: analysis.recommendedTests.firstOrNull()
                ?: "Doctor consultation",
            urgencyLevel = urgency,
            status = CaseStatus.ANALYZED,
            fusionResult = FusionResult(
                findings = analysis.summary,
                confidenceScore = topConfidence / 100.0,
                riskScore = when (urgency) {
                    UrgencyLevel.RED -> 0.8
                    UrgencyLevel.YELLOW -> 0.5
                    UrgencyLevel.GREEN -> 0.2
                },
                confidenceLevel = ConfidenceLevel.fromScore(topConfidence / 100.0),
            ),
        )
        cases[caseId] = case
        return Resource.Success(case)
    }
}
