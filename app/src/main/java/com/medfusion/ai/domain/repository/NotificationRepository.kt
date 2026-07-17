package com.medfusion.ai.domain.repository

import com.medfusion.ai.domain.model.AppNotification
import com.medfusion.ai.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Notification center contract (Phase 6.5). Local-only today; a future FCM
 * integration simply posts remote messages through [post] — consumers observe
 * the same flows either way, so the architecture doesn't change.
 */
interface NotificationRepository {

    /** Newest-first notifications for one portal. */
    fun observe(audience: UserRole): Flow<List<AppNotification>>

    /** Live unread count for the bell badge. */
    fun unreadCount(audience: UserRole): Flow<Int>

    /** Adds a notification; silently ignored if its dedupeKey already exists. */
    suspend fun post(notification: AppNotification)

    suspend fun markRead(id: String)

    suspend fun markAllRead(audience: UserRole)

    suspend fun clear(audience: UserRole)
}
