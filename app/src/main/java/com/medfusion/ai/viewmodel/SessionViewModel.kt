package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the current signed-in [User] to dashboards and provides logout.
 * Scoped per screen (dashboards) — reads the repository's auth flow.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() = authRepository.logout()
}
