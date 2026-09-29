package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("streamclean_prefs", Context.MODE_PRIVATE)

    private val _appearance = MutableStateFlow(
        prefs.getString(KEY_APPEARANCE, "System default") ?: "System default"
    )
    val appearance: StateFlow<String> = _appearance.asStateFlow()

    private val initialLanguage: String = run {
        val saved = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        if (saved in SUPPORTED_LANGUAGES) saved else "English"
    }

    private val _language = MutableStateFlow(initialLanguage)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setAppearance(option: String) {
        prefs.edit().putString(KEY_APPEARANCE, option).apply()
        _appearance.value = option
    }

    fun setLanguage(lang: String) {
        val safeLang = if (lang in SUPPORTED_LANGUAGES) lang else "English"
        prefs.edit().putString(KEY_LANGUAGE, safeLang).apply()
        _language.value = safeLang

        val tag = getLanguageTag(safeLang)
        try {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        } catch (e: Exception) {
            // Graceful fallback if unsupported in current context
        }
    }

    companion object {
        const val KEY_APPEARANCE = "key_appearance"
        const val KEY_LANGUAGE = "key_language"
        const val KEY_NOTIFICATIONS = "key_notifications"
        const val APP_VERSION = "StreamClean V1.0.0"

        val SUPPORTED_LANGUAGES = listOf("English", "বাংলা", "हिन्दी")

        fun getLanguageTag(language: String): String {
            return when (language) {
                "বাংলা" -> "bn"
                "हिन्दी" -> "hi"
                else -> "en"
            }
        }

        fun getLanguageCodeDisplay(language: String): String {
            return when (language) {
                "বাংলা" -> "BN"
                "हिन्दी" -> "HI"
                else -> "EN"
            }
        }
    }
}
