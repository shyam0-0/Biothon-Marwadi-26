package com.medfusion.ai.di

import com.medfusion.ai.data.notifications.LocalNotificationRepository
import com.medfusion.ai.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase 6.5 platform services: the local notification center. Session-local by
 * design (Phase 7.1 leaves notification persistence untouched); interface-bound
 * so a remote/push-backed variant can be swapped in later without touching
 * consumers. The doctor profile store moved to [RepositoryModule] in Phase 7.1
 * so it can switch between the Firestore and Demo Mode implementations like the
 * other repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: LocalNotificationRepository): NotificationRepository
}
