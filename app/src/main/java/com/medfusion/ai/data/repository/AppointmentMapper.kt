package com.medfusion.ai.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.medfusion.ai.data.firebase.FirestoreSchema.Appointments
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.UrgencyLevel

/** Firestore document → [Appointment]. Field keys live in FirestoreSchema. */
fun DocumentSnapshot.toAppointment(): Appointment? {
    if (!exists()) return null
    return Appointment(
        id = id,
        patientId = getString(Appointments.PATIENT_ID).orEmpty(),
        patientName = getString(Appointments.PATIENT_NAME).orEmpty(),
        doctorId = getString(Appointments.DOCTOR_ID).orEmpty(),
        doctorName = getString(Appointments.DOCTOR_NAME).orEmpty(),
        date = getString(Appointments.DATE).orEmpty(),
        timeSlot = getString(Appointments.TIME_SLOT).orEmpty(),
        message = getString(Appointments.MESSAGE).orEmpty(),
        urgencyLevel = UrgencyLevel.fromWire(getString(Appointments.URGENCY_LEVEL)),
        status = AppointmentStatus.fromWire(getString(Appointments.STATUS)),
        caseId = getString(Appointments.CASE_ID),
        roomUrl = getString(Appointments.ROOM_URL),
        createdAtMillis = (get(Appointments.CREATED_AT) as? Timestamp)?.toDate()?.time ?: 0L,
        specialty = getString(Appointments.SPECIALTY),
        doctorNotes = getString(Appointments.DOCTOR_NOTES),
        diagnosis = getString(Appointments.DIAGNOSIS),
        prescriptionId = getString(Appointments.PRESCRIPTION_ID),
    )
}

/**
 * Smart-queue ordering: highest urgency first, then earliest requested date, then
 * time slot. Applied client-side because Firestore can't order by our urgency
 * priority (a derived value) directly.
 */
fun List<Appointment>.sortedForQueue(): List<Appointment> =
    sortedWith(
        compareByDescending<Appointment> { it.urgencyLevel.priority }
            .thenBy { it.date }
            .thenBy { it.timeSlot }
    )
