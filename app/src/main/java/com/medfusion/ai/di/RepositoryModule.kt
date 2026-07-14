package com.medfusion.ai.di

import com.medfusion.ai.BuildConfig
import com.medfusion.ai.data.demo.FakeAppointmentRepository
import com.medfusion.ai.data.demo.FakeAuthRepository
import com.medfusion.ai.data.demo.FakeCareRepository
import com.medfusion.ai.data.demo.FakeCaseRepository
import com.medfusion.ai.data.demo.FakeEmergencyRepository
import com.medfusion.ai.data.demo.FakeReportRepository
import com.medfusion.ai.data.demo.FakeStorageRepository
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Binds repository interfaces to their implementations. When BuildConfig.DEMO_MODE
 * is on (debug), in-memory fakes are used so the whole app runs with no Firebase
 * or backend. Providers ensure only the chosen implementation is instantiated —
 * so the real Firebase-backed impls are never created in Demo Mode.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideAuthRepository(
        real: Provider<AuthRepositoryImpl>,
        fake: Provider<FakeAuthRepository>,
    ): AuthRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()

    @Provides @Singleton
    fun provideCaseRepository(
        real: Provider<CaseRepositoryImpl>,
        fake: Provider<FakeCaseRepository>,
    ): CaseRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()

    @Provides @Singleton
    fun provideStorageRepository(
        real: Provider<StorageRepositoryImpl>,
        fake: Provider<FakeStorageRepository>,
    ): StorageRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()

    @Provides @Singleton
    fun provideReportRepository(
        real: Provider<ReportRepositoryImpl>,
        fake: Provider<FakeReportRepository>,
    ): ReportRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()

    @Provides @Singleton
    fun provideAppointmentRepository(
        real: Provider<AppointmentRepositoryImpl>,
        fake: Provider<FakeAppointmentRepository>,
    ): AppointmentRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()

    @Provides @Singleton
    fun provideCareRepository(
        real: Provider<CareRepositoryImpl>,
        fake: Provider<FakeCareRepository>,
    ): CareRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()

    @Provides @Singleton
    fun provideEmergencyRepository(
        real: Provider<EmergencyRepositoryImpl>,
        fake: Provider<FakeEmergencyRepository>,
    ): EmergencyRepository = if (BuildConfig.DEMO_MODE) fake.get() else real.get()
}
