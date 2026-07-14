package com.medfusion.ai.domain.model

/**
 * An authenticated MedFusion user. Persisted in the Firestore "users" collection
 * keyed by [uid]; [role] decides which side of the app they land on.
 */
data class User(
    val uid: String,
    val fullName: String,
    val email: String,
    val role: UserRole,
    val emergencyContact: String? = null,
    val preferredLanguage: String = "en",
)
