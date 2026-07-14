package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Presentation state shared by the login and registration screens. */
data class AuthUiState(
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
)

/** One-shot navigation signals — consumed once, never replayed on rotation. */
sealed interface AuthEvent {
    data class Authenticated(val user: User) : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun login(email: String, password: String, expectedRole: UserRole) {
        val validation = validateLogin(email, password)
        if (validation != null) {
            _uiState.update { it.copy(error = validation) }
            return
        }
        submit { authRepository.login(email, password, expectedRole) }
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole,
    ) {
        val validation = validateRegistration(fullName, email, password, confirmPassword)
        if (validation != null) {
            _uiState.update { it.copy(error = validation) }
            return
        }
        submit { authRepository.register(fullName, email, password, role) }
    }

    /** Clears a shown error, e.g. when the user edits a field after a failure. */
    fun consumeError() = _uiState.update { it.copy(error = null) }

    private inline fun submit(crossinline call: suspend () -> Resource<User>) {
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = call()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.emit(AuthEvent.Authenticated(result.data))
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, error = result.error) }
                }
            }
        }
    }

    // --- Validation -------------------------------------------------------

    private fun validateLogin(email: String, password: String): AppError? = when {
        email.isBlank() || password.isBlank() ->
            AppError.Validation("Please enter your email and password.")
        !isEmailValid(email) -> AppError.Validation("Please enter a valid email address.")
        else -> null
    }

    private fun validateRegistration(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): AppError? = when {
        fullName.isBlank() -> AppError.Validation("Please enter your full name.")
        !isEmailValid(email) -> AppError.Validation("Please enter a valid email address.")
        password.length < 6 -> AppError.Validation("Password must be at least 6 characters.")
        password != confirmPassword -> AppError.Validation("Passwords do not match.")
        else -> null
    }

    private fun isEmailValid(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
}
