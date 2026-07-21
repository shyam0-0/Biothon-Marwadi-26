import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY", "")

android {
    namespace = "com.medfusion.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.medfusion.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Gemini API Key
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Base URL of the FastAPI backend. Override per build type / local.properties.
        buildConfigField("String", "API_BASE_URL", "\"https://api.medfusion.ai/\"")
        buildConfigField("boolean", "USE_MOCK_AI_FALLBACK", "false")

        // DEMO_MODE swaps all repositories for in-memory fakes (no Firebase/backend
        // needed) so the full app can be run and demoed with zero setup.
        buildConfigField("boolean", "DEMO_MODE", "false")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false

            // Point to a locally-running FastAPI instance during development.
            // 10.0.2.2 is the host machine's loopback as seen from the emulator.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")

            // Fall back to on-device mock AI when the backend is unreachable, so
            // the full UI journey is demoable before FastAPI is running.
            buildConfigField("boolean", "USE_MOCK_AI_FALLBACK", "true")

            // Run with in-memory fakes — no Firebase/backend required. Set to
            // false once you've added a real google-services.json to go live.
            buildConfigField("boolean", "DEMO_MODE", "false")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "USE_MOCK_AI_FALLBACK", "false")
            buildConfigField("boolean", "DEMO_MODE", "false")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image loading
    implementation(libs.coil.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // Coroutines / storage / permissions
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.permissions)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}