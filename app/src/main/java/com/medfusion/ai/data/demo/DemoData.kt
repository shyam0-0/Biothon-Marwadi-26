package com.medfusion.ai.data.demo

import com.medfusion.ai.domain.model.ActivityLevel
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.CarePlan
import com.medfusion.ai.domain.model.Medication
import com.medfusion.ai.domain.model.UrgencyLevel

/**
 * Seed data + constants for Demo Mode. Lets the whole app run and be demonstrated
 * with no Firebase or backend. See BuildConfig.DEMO_MODE.
 */
object DemoData {
    const val PATIENT_ID = "demo-patient"
    const val PATIENT_NAME = "Demo Patient"
    const val DOCTOR_ID = "demo-doctor"
    const val DOCTOR_NAME = "Dr. Demo Sharma"

    val defaultSlots = listOf("09:00 AM", "10:30 AM", "12:00 PM", "03:00 PM", "04:30 PM")

    /** A ready-made appointment so the doctor queue isn't empty on first launch. */
    fun seedAppointment() = Appointment(
        id = "demo-appt-1",
        patientId = PATIENT_ID,
        patientName = PATIENT_NAME,
        doctorId = DOCTOR_ID,
        doctorName = DOCTOR_NAME,
        date = "2026-07-15",
        timeSlot = "10:30 AM",
        message = "Persistent cough for the last week, worse at night.",
        urgencyLevel = UrgencyLevel.YELLOW,
        status = AppointmentStatus.PENDING,
        caseId = "demo-case-seed",
        createdAtMillis = System.currentTimeMillis(),
    )

    fun carePlan(patientId: String) = CarePlan(
        patientId = patientId,
        medications = listOf(
            Medication("Amoxicillin", "500 mg", "After breakfast"),
            Medication("Vitamin D", "1 tablet", "With lunch"),
        ),
        activityGoals = listOf("30-minute walk", "8 glasses of water", "Sleep by 11 PM"),
        note = "Follow up in two weeks. Keep hydrated and monitor your temperature.",
    )

    val seedActivity = ActivityLevel.MODERATE
}
