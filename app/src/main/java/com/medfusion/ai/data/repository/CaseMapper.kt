package com.medfusion.ai.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.medfusion.ai.data.firebase.FirestoreSchema.Cases
import com.medfusion.ai.domain.model.BodyRegion
import com.medfusion.ai.domain.model.Case
import com.medfusion.ai.domain.model.CaseStatus
import com.medfusion.ai.domain.model.ConfidenceLevel
import com.medfusion.ai.domain.model.FusionResult
import com.medfusion.ai.domain.model.SymptomLocation
import com.medfusion.ai.domain.model.UrgencyLevel

/**
 * Firestore <-> domain mapping for cases, in one place so every phase reads and
 * writes the "cases" document consistently.
 */

/** Keys nested inside the "fusionResult" map field. */
object FusionKeys {
    const val FINDINGS = "findings"
    const val CONFIDENCE_SCORE = "confidenceScore"
    const val CONFIDENCE_LEVEL = "confidenceLevel"
    const val RISK_SCORE = "riskScore"
}

fun DocumentSnapshot.toCase(): Case? {
    if (!exists()) return null
    val fusionMap = get(Cases.FUSION_RESULT) as? Map<*, *>
    val fusion = fusionMap?.let {
        val score = (it[FusionKeys.CONFIDENCE_SCORE] as? Number)?.toDouble() ?: 0.0
        FusionResult(
            findings = it[FusionKeys.FINDINGS] as? String ?: "",
            confidenceScore = score,
            riskScore = (it[FusionKeys.RISK_SCORE] as? Number)?.toDouble() ?: 0.0,
            confidenceLevel = ConfidenceLevel.fromWire(it[FusionKeys.CONFIDENCE_LEVEL] as? String),
        )
    }
    return Case(
        caseId = getString(Cases.CASE_ID) ?: id,
        userId = getString(Cases.USER_ID).orEmpty(),
        symptomsText = getString(Cases.SYMPTOMS_TEXT).orEmpty(),
        recommendedTest = getString(Cases.RECOMMENDED_TEST).orEmpty(),
        urgencyLevel = UrgencyLevel.fromWire(getString(Cases.URGENCY_LEVEL)),
        status = CaseStatus.fromWire(getString(Cases.STATUS)),
        xrayUrl = getString(Cases.XRAY_URL),
        labReportUrl = getString(Cases.LAB_REPORT_URL),
        fusionResult = fusion,
        createdAtMillis = (get(Cases.CREATED_AT) as? Timestamp)?.toDate()?.time ?: 0L,
        symptomLocations = (get(Cases.SYMPTOM_LOCATIONS) as? List<*>).orEmpty()
            .mapNotNull { (it as? Map<*, *>)?.toSymptomLocation() },
    )
}

private fun Map<*, *>.toSymptomLocation(): SymptomLocation? {
    val region = BodyRegion.fromWire(this["region"] as? String) ?: return null
    return SymptomLocation(
        region = region,
        descriptor = this["descriptor"] as? String ?: "Pain",
        severity = (this["severity"] as? Number)?.toInt() ?: 0,
        duration = this["duration"] as? String ?: "",
        progression = this["progression"] as? String ?: "",
    )
}

/** Serializes body-map locations for the "symptomLocations" field. */
fun List<SymptomLocation>.toMaps(): List<Map<String, Any>> = map {
    mapOf(
        "region" to it.region.wireValue,
        "descriptor" to it.descriptor,
        "severity" to it.severity,
        "duration" to it.duration,
        "progression" to it.progression,
    )
}

/** Serializes a [FusionResult] into the nested map stored under "fusionResult". */
fun FusionResult.toMap(): Map<String, Any> = mapOf(
    FusionKeys.FINDINGS to findings,
    FusionKeys.CONFIDENCE_SCORE to confidenceScore,
    FusionKeys.CONFIDENCE_LEVEL to confidenceLevel.wireValue,
    FusionKeys.RISK_SCORE to riskScore,
)
