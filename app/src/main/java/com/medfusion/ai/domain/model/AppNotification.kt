package com.medfusion.ai.domain.model

import java.util.UUID

/**
 * Smart Notification Center (Phase 6.5). Notifications are LOCAL only: they are
 * posted by the app's own workflows (booking, consultation, care plan…) into
 * [com.medfusion.ai.domain.repository.NotificationRepository]. The kind — not
 * free text — is stored, and the UI resolves it to a localized title/body, so
 * notifications follow the app language. A future remote push service only has
 * to post into the same repository; nothing else changes.
 */
enum class NotificationKind {
    // Patient
    APPOINTMENT_BOOKED,
    APPOINTMENT_ACCEPTED,
    APPOINTMENT_RESCHEDULED,
    APPOINTMENT_CANCELLED,
    CONSULTATION_STARTING_SOON,
    DOCTOR_JOINED_CALL,
    PRESCRIPTION_READY,
    CARE_PLAN_READY,
    CHECK_IN_REMINDER,
    FOLLOW_UP_REMINDER,
    RECOVERY_UPDATE,
    DEVICE_CONNECTION_LOST,   // ESP32, future compatibility
    EMERGENCY_RECOMMENDATION,

    // Doctor
    NEW_APPOINTMENT_REQUEST,
    REPORT_UPLOADED,
    ANALYSIS_COMPLETED,
    CONSULTATION_REMINDER,
    FOLLOW_UP_BOOKED,
}

/** One entry in the notification center, targeted at a portal ([audience]). */
data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val audience: UserRole,
    val kind: NotificationKind,
    /** Contextual detail (doctor name, date, slot…) shown as the body text. */
    val detail: String? = null,
    /** Navigation route opened when the notification is tapped. */
    val route: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    /** Optional key to suppress duplicates (e.g. one check-in reminder per day). */
    val dedupeKey: String? = null,
)
