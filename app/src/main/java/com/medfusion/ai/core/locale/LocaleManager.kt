package com.medfusion.ai.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Thin wrapper over AppCompat's per-app locale API. Centralizes reading and
 * setting the app language so the Settings screen and the data layer (which
 * forwards the language to the AI endpoints) agree on one source of truth.
 */
object LocaleManager {

    val supported = listOf("en", "hi", "ta")

    /** Current 2-letter language code, defaulting to English. */
    fun current(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return tags.takeIf { it.isNotBlank() }?.substringBefore('-')?.lowercase() ?: "en"
    }

    fun set(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
