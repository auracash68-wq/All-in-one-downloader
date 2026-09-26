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

    private val _language = MutableStateFlow(
        prefs.getString(KEY_LANGUAGE, "English") ?: "English"
    )
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
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _language.value = lang

        val tag = getLanguageTag(lang)
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
        const val APP_VERSION = "2.4.0 (Build 2408)"

        fun getLanguageTag(language: String): String {
            return when (language) {
                "Español" -> "es"
                "Français" -> "fr"
                "Deutsch" -> "de"
                "বাংলা" -> "bn"
                "हिन्दी" -> "hi"
                "العربية" -> "ar"
                "日本語" -> "ja"
                else -> "en"
            }
        }

        fun getLanguageCodeDisplay(language: String): String {
            return when (language) {
                "Español" -> "ES"
                "Français" -> "FR"
                "Deutsch" -> "DE"
                "বাংলা" -> "BN"
                "हिन्दी" -> "HI"
                "العربية" -> "AR"
                "日本語" -> "JA"
                else -> "EN"
            }
        }
    }
}
