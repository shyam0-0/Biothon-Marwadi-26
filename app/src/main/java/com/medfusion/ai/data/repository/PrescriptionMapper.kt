package com.medfusion.ai.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.medfusion.ai.data.firebase.FirestoreSchema.Prescriptions
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.CarePlanSource
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.Prescription

/** Firestore document → [Prescription]. Medications are stored as a list of maps. */
fun DocumentSnapshot.toPrescription(): Prescription? {
    if (!exists()) return null
    @Suppress("UNCHECKED_CAST")
    val meds = (get(Prescriptions.MEDICATIONS) as? List<Map<String, Any?>>).orEmpty().map {
        Medication(
            name = it["name"] as? String ?: "",
            dosage = it["dosage"] as? String ?: "",
            timing = it["timing"] as? String ?: "",
        )
    }
    return Prescription(
        id = id,
        appointmentId = getString(Prescriptions.APPOINTMENT_ID).orEmpty(),
        patientId = getString(Prescriptions.PATIENT_ID).orEmpty(),
        doctorId = getString(Prescriptions.DOCTOR_ID).orEmpty(),
        doctorName = getString(Prescriptions.DOCTOR_NAME).orEmpty(),
        diagnosis = getString(Prescriptions.DIAGNOSIS).orEmpty(),
        medications = meds,
        advice = getString(Prescriptions.ADVICE).orEmpty(),
        followUpDate = getString(Prescriptions.FOLLOW_UP_DATE),
        createdAtMillis = (get(Prescriptions.CREATED_AT) as? Timestamp)?.toDate()?.time ?: 0L,
    )
}

/** Builds the activity goals for a generated care plan from free-text advice. */
fun carePlanGoalsFrom(advice: String): List<String> {
    val goals = advice.split("\n", ";", ".")
        .map { it.trim() }
        .filter { it.length > 3 }
    return goals.ifEmpty { listOf("Follow your prescription", "Rest and stay hydrated") }
}

/** The dynamic care plan generated when a doctor approves a consultation (Phase 3). */
fun doctorCarePlan(
    patientId: String,
    doctorName: String,
    diagnosis: String,
    medications: List<Medication>,
    advice: String,
    followUpDate: String?,
): CarePlan = CarePlan(
    patientId = patientId,
    medications = medications,
    activityGoals = carePlanGoalsFrom(advice),
    note = advice.trim().ifBlank { null },
    diagnosis = diagnosis.trim(),
    doctorName = doctorName,
    recoveryGoals = listOf("Follow the prescription as directed", "Report if symptoms worsen"),
    lifestyle = listOf("Rest adequately", "Eat balanced meals"),
    hydration = "Drink 8+ glasses of water daily",
    exercise = "Light activity as tolerated; avoid strain",
    sleep = "Aim for 7–8 hours of sleep",
    followUpDate = followUpDate,
    source = CarePlanSource.DOCTOR,
)
