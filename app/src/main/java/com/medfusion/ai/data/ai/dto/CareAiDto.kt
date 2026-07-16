package com.medfusion.ai.data.ai.dto

import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CarePlanSource
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.ProgressAnalysis
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** JSON contract for the recovery-progress analysis (Phase 3). */
@JsonClass(generateAdapter = true)
data class ProgressAnalysisDto(
    @Json(name = "status") val status: String?,
    @Json(name = "summary") val summary: String?,
    @Json(name = "followUpRecommended") val followUpRecommended: Boolean?,
    @Json(name = "followUpReason") val followUpReason: String?,
)

fun ProgressAnalysisDto.toDomain() = ProgressAnalysis(
    status = status?.trim().orEmpty().ifEmpty { "Recovery in progress" },
    summary = summary?.trim().orEmpty(),
    followUpRecommended = followUpRecommended ?: false,
    followUpReason = followUpReason?.trim()?.takeIf { it.isNotEmpty() },
)

/** JSON contract for an AI-generated wellness plan (Phase 3). */
@JsonClass(generateAdapter = true)
data class WellnessPlanDto(
    @Json(name = "medications") val medications: List<MedicationDto>?,
    @Json(name = "activityGoals") val activityGoals: List<String>?,
    @Json(name = "recoveryGoals") val recoveryGoals: List<String>?,
    @Json(name = "lifestyle") val lifestyle: List<String>?,
    @Json(name = "hydration") val hydration: String?,
    @Json(name = "exercise") val exercise: String?,
    @Json(name = "sleep") val sleep: String?,
    @Json(name = "note") val note: String?,
)

@JsonClass(generateAdapter = true)
data class MedicationDto(
    @Json(name = "name") val name: String?,
    @Json(name = "dosage") val dosage: String?,
    @Json(name = "timing") val timing: String?,
)

/** Maps to a [CarePlan] with a blank patientId (the caller fills it in). */
fun WellnessPlanDto.toDomain() = CarePlan(
    patientId = "",
    medications = medications.orEmpty().mapNotNull {
        if (it.name.isNullOrBlank()) null
        else Medication(it.name.trim(), it.dosage?.trim().orEmpty(), it.timing?.trim().orEmpty())
    },
    activityGoals = activityGoals.orEmpty().clean(),
    note = note?.trim(),
    recoveryGoals = recoveryGoals.orEmpty().clean(),
    lifestyle = lifestyle.orEmpty().clean(),
    hydration = hydration?.trim(),
    exercise = exercise?.trim(),
    sleep = sleep?.trim(),
    source = CarePlanSource.AI_WELLNESS,
)

private fun List<String>.clean() = map { it.trim() }.filter { it.isNotEmpty() }
