package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory auth for Demo Mode: any credentials succeed and route by role. */
@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser

    override fun currentUserId(): String? = _currentUser.value?.uid ?: DemoData.PATIENT_ID

    override suspend fun login(email: String, password: String, expectedRole: UserRole?): Resource<User> {
        val role = expectedRole ?: UserRole.PATIENT
        return Resource.Success(userFor(role, email))
    }

    override suspend fun register(
        fullName: String,
        email: String,
        password: String,
        role: UserRole,
    ): Resource<User> {
        val user = userFor(role, email).copy(fullName = fullName.ifBlank { defaultName(role) })
        _currentUser.value = user
        return Resource.Success(user)
    }

    override fun logout() {
        _currentUser.value = null
    }

    private fun userFor(role: UserRole, email: String): User {
        val user = User(
            uid = if (role == UserRole.PATIENT) DemoData.PATIENT_ID else DemoData.DOCTOR_ID,
            fullName = defaultName(role),
            email = email.ifBlank { "demo@medfusion.ai" },
            role = role,
            emergencyContact = "+1-202-555-0100",
        )
        _currentUser.value = user
        return user
    }

    private fun defaultName(role: UserRole) =
        if (role == UserRole.PATIENT) DemoData.PATIENT_NAME else DemoData.DOCTOR_NAME
}
