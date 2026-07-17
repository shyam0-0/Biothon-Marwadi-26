package com.medfusion.ai.di

import com.medfusion.ai.data.notifications.LocalNotificationRepository
import com.medfusion.ai.data.repository.DoctorProfileRepositoryImpl
import com.medfusion.ai.domain.repository.DoctorProfileRepository
import com.medfusion.ai.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase 6.5 platform services: the local notification center and the doctor
 * profile store. Both are interface-bound so remote/push/backed variants can be
 * swapped in later without touching consumers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: LocalNotificationRepository): NotificationRepository

    @Binds @Singleton
    abstract fun bindDoctorProfileRepository(impl: DoctorProfileRepositoryImpl): DoctorProfileRepository
}
