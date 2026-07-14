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

        // Subcollection of a care plan document.
        const val DAILY_LOGS = "daily_logs"
        const val LOG_DATE = "date"
        const val LOG_SLEEP_HOURS = "sleepHours"
        const val LOG_ACTIVITY_LEVEL = "activityLevel"
        const val LOG_MOOD = "mood"
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
