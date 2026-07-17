package com.medfusion.ai.navigation

/**
 * Central registry of navigation routes and argument keys. Declaring the full
 * map up front means later phases only add screen composables to the NavHost —
 * they never have to edit route strings, keeping navigation stable.
 */
object Routes {

    // Entry / auth (Phase 0–1)
    const val ROLE_SELECTION = "role_selection"
    const val PATIENT_LOGIN = "patient_login"
    const val DOCTOR_LOGIN = "doctor_login"
    const val REGISTER = "register?role={role}"

    // Dashboards
    const val PATIENT_DASHBOARD = "patient_dashboard"
    const val DOCTOR_DASHBOARD = "doctor_dashboard"

    // Patient journey (Phase 2–7)
    const val SYMPTOM_TRIAGE = "symptom_triage"
    const val UPLOAD_RESULTS = "upload_results/{caseId}"
    const val ANALYSIS = "analysis/{caseId}"
    const val RESULT = "result/{caseId}"
    const val BOOK_APPOINTMENT = "book_appointment/{caseId}?urgency={urgency}&specialty={specialty}"

    // Consultation & care (Phase 9–11)
    const val PATIENT_APPOINTMENTS = "patient_appointments"
    const val VIDEO_CALL = "video_call/{appointmentId}"
    const val DOCTOR_CONSULTATION = "doctor_consultation/{appointmentId}"
    const val PRESCRIPTION = "prescription/{appointmentId}"
    const val CARE_PLAN = "care_plan"
    const val VITALS_MONITOR = "vitals_monitor"

    // Patient Passport (Phase 5)
    const val PATIENT_PASSPORT = "patient_passport"

    // Doctor portal (Phase 6)
    const val DOCTOR_PATIENTS = "doctor_patients"
    const val DOCTOR_PATIENT_PROFILE = "doctor_patient/{patientId}?patientName={patientName}"
    const val DOCTOR_SCHEDULE = "doctor_schedule"

    // Settings (Phase 12)
    const val SETTINGS = "settings"

    // Platform polish (Phase 6.5)
    const val NOTIFICATIONS = "notifications"
    const val DOCTOR_PROFILE = "doctor_profile"

    /** Argument keys, kept beside the routes that use them. */
    object Args {
        const val ROLE = "role"
        const val CASE_ID = "caseId"
        const val URGENCY = "urgency"
        const val SPECIALTY = "specialty"
        const val APPOINTMENT_ID = "appointmentId"
        const val PATIENT_ID = "patientId"
        const val PATIENT_NAME = "patientName"
    }

    // --- Type-safe builders for parameterized routes -------------------------

    fun register(role: String) = "register?role=$role"
    fun uploadResults(caseId: String) = "upload_results/$caseId"
    fun analysis(caseId: String) = "analysis/$caseId"
    fun result(caseId: String) = "result/$caseId"
    fun bookAppointment(caseId: String, urgency: String, specialty: String = "") =
        "book_appointment/$caseId?urgency=$urgency&specialty=${specialty.encodeArg()}"

    /** Sentinel caseId for follow-up bookings that aren't backed by a triage case. */
    const val FOLLOW_UP_CASE = "follow-up"

    /** Follow-up booking from the care plan (Phase 4). */
    fun bookFollowUp(specialty: String = "") =
        bookAppointment(FOLLOW_UP_CASE, "green", specialty)
    fun videoCall(appointmentId: String) = "video_call/$appointmentId"

    /** Doctor patient directory → read-only patient profile (Phase 6). */
    fun doctorPatientProfile(patientId: String, patientName: String): String {
        val encoded = java.net.URLEncoder.encode(patientName.ifBlank { "Patient" }, "UTF-8")
            .replace("+", "%20")
        return "doctor_patient/$patientId?patientName=$encoded"
    }
    fun doctorConsultation(appointmentId: String) = "doctor_consultation/$appointmentId"
    fun prescription(appointmentId: String) = "prescription/$appointmentId"

    /** Minimal encoding so specialty names with spaces/slashes are route-safe.
     *  Navigation URL-decodes the value back to plain text in the destination. */
    private fun String.encodeArg(): String =
        java.net.URLEncoder.encode(this.ifBlank { "General Physician" }, "UTF-8")
            .replace("+", "%20")
}
