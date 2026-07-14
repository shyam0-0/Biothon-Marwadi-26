package com.medfusion.ai.domain.model

/**
 * The two audiences of the platform. Stored as [wireValue] in the Firestore
 * "users" collection and used to route to the correct dashboard after login.
 */
enum class UserRole(val wireValue: String) {
    PATIENT("patient"),
    DOCTOR("doctor");

    companion object {
        fun fromWire(value: String?): UserRole? =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
    }
}
