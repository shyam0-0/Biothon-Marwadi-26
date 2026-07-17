package com.medfusion.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medfusion.ai.core.network.ConnectivityObserver
import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notification center + bell badge (Phase 6.5). Scopes the shared notification
 * repository to the signed-in user's portal, so the same components serve both
 * Patient and Doctor. Also exposes connectivity for the app-wide offline banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val role: StateFlow<UserRole?> = authRepository.currentUser
        .flatMapLatest { flowOf(it?.role) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notifications: StateFlow<List<AppNotification>> = role
        .flatMapLatest { r -> r?.let { notificationRepository.observe(it) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = role
        .flatMapLatest { r -> r?.let { notificationRepository.unreadCount(it) } ?: flowOf(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        val r = role.value ?: return
        viewModelScope.launch { notificationRepository.markAllRead(r) }
    }

    fun clearAll() {
        val r = role.value ?: return
        viewModelScope.launch { notificationRepository.clear(r) }
    }
}
