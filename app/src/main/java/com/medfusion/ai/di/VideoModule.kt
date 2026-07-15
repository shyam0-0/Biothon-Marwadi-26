package com.medfusion.ai.di

import com.medfusion.ai.data.video.BrowserVideoProvider
import com.medfusion.ai.domain.video.VideoProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the modular video provider. Swap the binding to change video backends. */
@Module
@InstallIn(SingletonComponent::class)
abstract class VideoModule {
    @Binds @Singleton
    abstract fun bindVideoProvider(impl: BrowserVideoProvider): VideoProvider
}
