package com.medfusion.ai.data.ai.dto

import com.medfusion.ai.domain.model.MedicineExplanation
import com.medfusion.ai.domain.model.PatientExplanation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** JSON contract for the patient-friendly medical translation (Phase 5.6). */
@JsonClass(generateAdapter = true)
data class PatientExplanationDto(
    @Json(name = "whatDoctorFound") val whatDoctorFound: String?,
    @Json(name = "medicines") val medicines: List<MedicineExplanationDto>?,
    @Json(name = "whatToDo") val whatToDo: List<String>?,
    @Json(name = "recovery") val recovery: String?,
    @Json(name = "warningSigns") val warningSigns: List<String>?,
)

@JsonClass(generateAdapter = true)
data class MedicineExplanationDto(
    @Json(name = "name") val name: String?,
    @Json(name = "purpose") val purpose: String?,
)

fun PatientExplanationDto.toDomain() = PatientExplanation(
    whatDoctorFound = whatDoctorFound?.trim().orEmpty(),
    medicines = medicines.orEmpty()
        .filter { !it.name.isNullOrBlank() }
        .map { MedicineExplanation(it.name!!.trim(), it.purpose?.trim().orEmpty()) },
    whatToDo = whatToDo.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
    recovery = recovery?.trim().orEmpty(),
    warningSigns = warningSigns.orEmpty().map { it.trim() }.filter { it.isNotEmpty() },
)
