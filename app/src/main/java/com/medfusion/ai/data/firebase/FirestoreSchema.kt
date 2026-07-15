package com.medfusion.ai.data.firebase

/**
 * Firestore collection names and field keys in one place so every repository
 * reads/writes the same schema — no stringly-typed drift between features.
 */
object FirestoreSchema {

    object Users {
        const val COLLECTION = "users"
        const val FULL_NAME = "fullName"
        const val EMAIL = "email"
        const val ROLE = "role"
        const val EMERGENCY_CONTACT = "emergencyContact"
        const val PREFERRED_LANGUAGE = "preferredLanguage"
        const val CREATED_AT = "createdAt"
    }

    object Cases {
        const val COLLECTION = "cases"
        const val CASE_ID = "caseId"
        const val USER_ID = "userId"
        const val SYMPTOMS_TEXT = "symptomsText"
        const val RECOMMENDED_TEST = "recommendedTest"
        const val URGENCY_LEVEL = "urgencyLevel"
        const val STATUS = "status"
        const val XRAY_URL = "xrayUrl"
        const val LAB_REPORT_URL = "labReportUrl"
        const val FUSION_RESULT = "fusionResult"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    object Appointments {
        const val COLLECTION = "appointments"
        const val PATIENT_ID = "patientId"
        const val PATIENT_NAME = "patientName"
        const val DOCTOR_ID = "doctorId"
        const val DOCTOR_NAME = "doctorName"
        const val DATE = "date"
        const val TIME_SLOT = "timeSlot"
        const val MESSAGE = "message"
        const val URGENCY_LEVEL = "urgencyLevel"
        const val STATUS = "status"
        const val ROOM_URL = "roomUrl"
        const val CASE_ID = "caseId"
        const val CREATED_AT = "createdAt"
        const val SPECIALTY = "specialty"
        const val DOCTOR_NOTES = "doctorNotes"
        const val DIAGNOSIS = "diagnosis"
        const val PRESCRIPTION_ID = "prescriptionId"
    }

    object Doctors {
        const val COLLECTION = "doctors"
        const val NAME = "name"
        const val SPECIALTY = "specialty"
        const val YEARS_EXPERIENCE = "yearsExperience"
        const val RATING = "rating"
        const val QUALIFICATION = "qualification"
    }

    object Prescriptions {
        const val COLLECTION = "prescriptions"
        const val APPOINTMENT_ID = "appointmentId"
        const val PATIENT_ID = "patientId"
        const val DOCTOR_ID = "doctorId"
        const val DOCTOR_NAME = "doctorName"
        const val DIAGNOSIS = "diagnosis"
        const val MEDICATIONS = "medications"
        const val ADVICE = "advice"
        const val FOLLOW_UP_DATE = "followUpDate"
        const val CREATED_AT = "createdAt"
    }

    object DoctorAvailability {
        const val COLLECTION = "doctor_availability"
        const val DOCTOR_ID = "doctorId"
        const val DATE = "date"
        const val SLOTS = "slots"
    }

    object CarePlans {
        const val COLLECTION = "care_plans"
        const val PATIENT_ID = "patientId"
        const val MEDICATIONS = "medications"     // list of maps: name/dosage/timing
        const val ACTIVITY_GOALS = "activityGoals" // list of strings
        const val NOTE = "note"
        const val DIAGNOSIS = "diagnosis"
        const val DOCTOR_NAME = "doctorName"
        const val RECOVERY_GOALS = "recoveryGoals"
        const val LIFESTYLE = "lifestyle"
        const val HYDRATION = "hydration"
        const val EXERCISE = "exercise"
        const val SLEEP = "sleep"
        const val FOLLOW_UP_DATE = "followUpDate"
        const val SOURCE = "source"

        // Subcollection of a care plan document.
        const val DAILY_LOGS = "daily_logs"
        const val LOG_DATE = "date"
        const val LOG_SLEEP_HOURS = "sleepHours"
        const val LOG_ACTIVITY_LEVEL = "activityLevel"
        const val LOG_MOOD = "mood"
        const val LOG_PAIN_LEVEL = "painLevel"
        const val LOG_CURRENT_SYMPTOMS = "currentSymptoms"
        const val LOG_MEDICATION_TAKEN = "medicationTaken"
        const val LOG_TEMPERATURE = "temperature"
        const val LOG_NOTES = "notes"
        const val LOG_CREATED_AT = "createdAt"
    }

    object PendingApprovals {
        const val COLLECTION = "pending_doctor_approvals"
        const val PATIENT_ID = "patientId"
        const val MESSAGE = "message"
        const val TYPE = "type"          // e.g. "medication"
        const val STATUS = "status"      // pending | approved | rejected
        const val CREATED_AT = "createdAt"
    }

    object EmergencyEvents {
        const val COLLECTION = "emergency_events"
        const val PATIENT_ID = "patientId"
        const val HEART_RATE = "heartRate"
        const val ACTION = "action"      // confirmed | dismissed | auto_escalated
        const val HOSPITAL = "hospital"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val CREATED_AT = "createdAt"
    }
}
