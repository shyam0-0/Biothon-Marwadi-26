package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Auth boundary for the presentation layer. Implementations wrap Firebase Auth +
 * Firestore; view models depend only on this interface (testable, swappable).
 */
interface AuthRepository {

    /** Emits the current signed-in [User], or null when signed out. */
    val currentUser: Flow<User?>

    /** The uid of the signed-in user, or null. Cheap synchronous accessor. */
    fun currentUserId(): String?

    /**
     * Signs in with email/password, then loads the user's profile (including
     * [UserRole]) from Firestore. The optional [expectedRole] guards against a
     * patient signing in on the doctor screen and vice-versa.
     */
    suspend fun login(email: String, password: String, expectedRole: UserRole? = null): Resource<User>

    /**
     * Creates the Firebase Auth account and the matching Firestore "users"
     * document with the selected [role].
     */
    suspend fun register(fullName: String, email: String, password: String, role: UserRole): Resource<User>

    fun logout()
}
