package com.medfusion.ai.di

import com.medfusion.ai.data.ai.GeminiApi
import com.medfusion.ai.data.ai.GeminiService
import com.medfusion.ai.domain.ai.AiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Wires the AI abstraction. [AiService] is always backed by [GeminiService]
 * (independent of DEMO_MODE, since Gemini needs no Firebase); GeminiService
 * itself falls back to an on-device analysis when offline in demo/mock builds.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides @Singleton
    fun provideGeminiApi(retrofit: Retrofit): GeminiApi = retrofit.create(GeminiApi::class.java)

    @Provides @Singleton
    fun provideAiService(service: GeminiService): AiService = service
}
