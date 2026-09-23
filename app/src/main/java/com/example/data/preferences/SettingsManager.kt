package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("streamclean_prefs", Context.MODE_PRIVATE)

    private val _downloadLocation = MutableStateFlow(
        prefs.getString(KEY_DOWNLOAD_LOCATION, "/Internal storage/Download/StreamClean") ?: "/Internal storage/Download/StreamClean"
    )
    val downloadLocation: StateFlow<String> = _downloadLocation.asStateFlow()

    private val _wifiOnly = MutableStateFlow(prefs.getBoolean(KEY_WIFI_ONLY, true))
    val wifiOnly: StateFlow<Boolean> = _wifiOnly.asStateFlow()

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

    fun setWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, enabled).apply()
        _wifiOnly.value = enabled
    }

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
    }

    fun setDownloadLocation(loc: String) {
        prefs.edit().putString(KEY_DOWNLOAD_LOCATION, loc).apply()
        _downloadLocation.value = loc
    }

    companion object {
        const val KEY_DOWNLOAD_LOCATION = "key_download_location"
        const val KEY_WIFI_ONLY = "key_wifi_only"
        const val KEY_APPEARANCE = "key_appearance"
        const val KEY_LANGUAGE = "key_language"
        const val KEY_NOTIFICATIONS = "key_notifications"
        const val APP_VERSION = "2.4.0 (Build 2408)"
    }
}
