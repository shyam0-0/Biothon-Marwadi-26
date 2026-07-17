package com.medfusion.ai.data.notifications

import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory local notification store — used in both Demo Mode and live builds
 * (notifications are session-local by design in this phase). Swapping in a
 * persistent or push-backed implementation later only requires a new binding.
 */
@Singleton
class LocalNotificationRepository @Inject constructor() : NotificationRepository {

    private val notifications = MutableStateFlow<List<AppNotification>>(emptyList())

    override fun observe(audience: UserRole): Flow<List<AppNotification>> =
        notifications.map { list ->
            list.filter { it.audience == audience }.sortedByDescending { it.timestampMillis }
        }

    override fun unreadCount(audience: UserRole): Flow<Int> =
        notifications.map { list -> list.count { it.audience == audience && !it.read } }

    override suspend fun post(notification: AppNotification) {
        notifications.update { current ->
            val duplicate = notification.dedupeKey != null &&
                current.any { it.dedupeKey == notification.dedupeKey }
            if (duplicate) current else current + notification
        }
    }

    override suspend fun markRead(id: String) {
        notifications.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
    }

    override suspend fun markAllRead(audience: UserRole) {
        notifications.update { list ->
            list.map { if (it.audience == audience) it.copy(read = true) else it }
        }
    }

    override suspend fun clear(audience: UserRole) {
        notifications.update { list -> list.filterNot { it.audience == audience } }
    }
}
