package com.medfusion.ai

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Annotated for Hilt so the dependency graph is built
 * once at process start and shared across activities, view models and services.
 */
@HiltAndroidApp
class MedFusionApplication : Application()
