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
        // Body-map symptom localization (Phase 5.6): list of maps
        // {region, descriptor, severity, duration, progression}.
        const val SYMPTOM_LOCATIONS = "symptomLocations"
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

        // Phase 7.1: the doctor's own editable professional profile (Phase 6.5
        // UI) is merged into the same directory document, keyed by doctorId, so
        // it stays in sync with what patients see in the booking search.
        const val PHOTO_URL = "photoUrl"
        const val HOSPITAL = "hospital"
        const val LANGUAGES_SPOKEN = "languagesSpoken"
        const val BIOGRAPHY = "biography"
        const val AVAILABILITY_TEXT = "availabilityText"
        const val LICENSE_NUMBER = "licenseNumber"

        // Links a directory entry to the Firebase Auth account that should see
        // it as "their" Doctor Portal — set once a doctor signs in and is
        // matched/claims this profile. Never hardcoded.
        const val DOCTOR_AUTH_UID = "doctorAuthUid"
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

    object Passports {
        const val COLLECTION = "patient_passports"
        const val FULL_NAME = "fullName"
        const val AGE = "age"
        const val GENDER = "gender"
        const val BLOOD_GROUP = "bloodGroup"
        const val HEIGHT_CM = "heightCm"
        const val WEIGHT_KG = "weightKg"
        const val PHOTO_URL = "photoUrl"
        const val CONTACT_NUMBER = "contactNumber"
        const val EMERGENCY_CONTACT = "emergencyContact"
        const val ALLERGIES = "allergies"
        const val CHRONIC_DISEASES = "chronicDiseases"
        const val CURRENT_MEDICATIONS = "currentMedications"
        const val PREVIOUS_DIAGNOSES = "previousDiagnoses"
        const val PREVIOUS_SURGERIES = "previousSurgeries"
        const val VACCINATIONS = "vaccinations"
        const val SMOKER = "smoker"
        const val ALCOHOL = "alcohol"
        const val PREGNANT = "pregnant"
        const val UPDATED_AT = "updatedAt"

        // Subcollection: stored AI consultations (Phase 5 AI history).
        const val AI_HISTORY = "ai_history"
        const val AI_DATE = "dateMillis"
        const val AI_SYMPTOMS = "symptoms"
        const val AI_SUMMARY = "summary"
        const val AI_CONDITIONS = "conditions"      // list of maps: name/confidence/reason
        const val AI_SEVERITY = "severity"
        const val AI_TESTS = "recommendedTests"
        const val AI_SPECIALIST = "recommendedSpecialist"
        const val AI_LOCATIONS = "locations"        // list of summary strings (Phase 5.6)

        // Subcollection: the automatic medical timeline (Phase 5).
        const val TIMELINE = "timeline"
        const val EVENT_TYPE = "type"
        const val EVENT_TITLE = "title"
        const val EVENT_DETAIL = "detail"
        const val EVENT_DATE = "dateMillis"
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

    /**
     * Phase 7.3/7.4: written by the FastAPI backend (ESP32 -> backend ->
     * Firestore), never by the Android app — this is read-only from here.
     * A separate top-level collection from [Users]/[Passports] by design,
     * matching the backend's existing schema exactly.
     */
    object LatestVitals {
        const val PATIENTS_COLLECTION = "patients"
        const val SUBCOLLECTION = "latestVitals"
        const val DOCUMENT_ID = "current"
        const val DEVICE_ID = "deviceId"
        const val HEART_RATE = "heartRate"
        const val SPO2 = "spo2"
        const val TIMESTAMP = "timestamp"
        const val CREATED_AT = "createdAt"
        const val SOURCE = "source"
    }
}
