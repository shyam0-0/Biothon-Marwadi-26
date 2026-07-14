package com.medfusion.ai.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.core.util.fail
import com.medfusion.ai.core.util.resourceOf
import com.medfusion.ai.data.firebase.FirestoreSchema.Users
import com.medfusion.ai.di.IoDispatcher
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                // Fetch the profile doc; failures resolve to null rather than crash.
                firestore.collection(Users.COLLECTION).document(uid).get()
                    .addOnSuccessListener { snap -> trySend(snap.toUser(uid)) }
                    .addOnFailureListener { trySend(null) }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override suspend fun login(
        email: String,
        password: String,
        expectedRole: UserRole?,
    ): Resource<User> = withContext(io) {
        resourceOf {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid
                ?: fail(AppError.Server())

            val snap = firestore.collection(Users.COLLECTION).document(uid).get().await()
            val user = snap.toUser(uid)
                ?: fail(AppError.NotFound("Your profile was not found. Please contact support."))

            if (expectedRole != null && user.role != expectedRole) {
                // Wrong door: sign back out so the session doesn't linger.
                auth.signOut()
                fail(
                    AppError.Validation(
                        "This account is registered as a ${user.role.wireValue}. " +
                            "Please use the ${expectedRole.wireValue} sign-in."
                    )
                )
            }
            user
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
    ): Resource<User> = withContext(io) {
        resourceOf {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val uid = result.user?.uid
                ?: throw IllegalStateException("Account creation returned no user")

            val profile = mapOf(
                Users.FULL_NAME to fullName.trim(),
                Users.EMAIL to email.trim(),
                Users.ROLE to role.wireValue,
                Users.PREFERRED_LANGUAGE to "en",
                Users.CREATED_AT to FieldValue.serverTimestamp(),
            )
            firestore.collection(Users.COLLECTION).document(uid).set(profile).await()

            User(uid = uid, fullName = fullName.trim(), email = email.trim(), role = role)
        }
    }

    override fun logout() = auth.signOut()
}

/** Maps a Firestore user document into the domain [User], or null if absent. */
private fun com.google.firebase.firestore.DocumentSnapshot.toUser(uid: String): User? {
    if (!exists()) return null
    val role = UserRole.fromWire(getString(Users.ROLE)) ?: return null
    return User(
        uid = uid,
        fullName = getString(Users.FULL_NAME).orEmpty(),
        email = getString(Users.EMAIL).orEmpty(),
        role = role,
        emergencyContact = getString(Users.EMERGENCY_CONTACT),
        preferredLanguage = getString(Users.PREFERRED_LANGUAGE) ?: "en",
    )
}
