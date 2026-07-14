package com.medfusion.ai.di

import com.medfusion.ai.data.report.ReportRepositoryImpl
import com.medfusion.ai.data.repository.AppointmentRepositoryImpl
import com.medfusion.ai.data.repository.AuthRepositoryImpl
import com.medfusion.ai.data.repository.CareRepositoryImpl
import com.medfusion.ai.data.repository.CaseRepositoryImpl
import com.medfusion.ai.data.repository.EmergencyRepositoryImpl
import com.medfusion.ai.data.repository.StorageRepositoryImpl
import com.medfusion.ai.domain.repository.AppointmentRepository
import com.medfusion.ai.domain.repository.AuthRepository
import com.medfusion.ai.domain.repository.CareRepository
import com.medfusion.ai.domain.repository.CaseRepository
import com.medfusion.ai.domain.repository.EmergencyRepository
import com.medfusion.ai.domain.repository.ReportRepository
import com.medfusion.ai.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces to their implementations. New repositories are
 * added here as later phases introduce them.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindCaseRepository(impl: CaseRepositoryImpl): CaseRepository

    @Binds @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds @Singleton
    abstract fun bindAppointmentRepository(impl: AppointmentRepositoryImpl): AppointmentRepository

    @Binds @Singleton
    abstract fun bindCareRepository(impl: CareRepositoryImpl): CareRepository

    @Binds @Singleton
    abstract fun bindEmergencyRepository(impl: EmergencyRepositoryImpl): EmergencyRepository
}
