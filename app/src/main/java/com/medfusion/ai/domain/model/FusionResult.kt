package com.medfusion.ai.domain.model

/**
 * Output of the multimodal fusion model (Phase 4), rendered on the explainable
 * result screen (Phase 5). [confidenceScore] is 0.0–1.0; [confidenceLevel] is the
 * friendly band shown to the patient.
 */
data class FusionResult(
    val findings: String,
    val confidenceScore: Double,
    val riskScore: Double,
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.fromScore(confidenceScore),
)
