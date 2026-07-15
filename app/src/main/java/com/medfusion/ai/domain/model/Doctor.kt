package com.medfusion.ai.domain.model

/**
 * A consultable doctor, filtered by [specialty] during booking (Phase 2). Stored
 * in the Firestore "doctors" collection (or seeded in Demo Mode).
 */
data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val yearsExperience: Int = 0,
    val rating: Double = 0.0,
    val qualification: String = "",
)
