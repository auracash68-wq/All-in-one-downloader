package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.preferences.SettingsManager
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StreamCleanApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: DownloadRepository
        private set

    lateinit var settingsManager: SettingsManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Room Database, Settings, and Repository
        database = AppDatabase.getInstance(this)
        repository = DownloadRepository(this, database.downloadDao())
        settingsManager = SettingsManager(this)

        // Create Notification Channels for downloads & completions
        createNotificationChannel()
        com.example.util.NotificationHelper.createNotificationChannels(this)

        // Pre-warm YoutubeDL engine and trigger recovery of any downloads interrupted by prior process death
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val engine = com.example.engine.DownloadEngine.getInstance(this@StreamCleanApplication)
                engine.ensureEngineInitialized()
                engine.recoverInterruptedDownloads()
            } catch (e: Exception) {
                Log.w(TAG, "Error initializing engine or recovering interrupted downloads: ${e.message}")
            }
        }

        // Ensure WebView cache directories exist to prevent first-launch filesystem warnings
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!webViewCacheDir.exists()) {
                webViewCacheDir.mkdirs()
            }
            val wasmCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!wasmCacheDir.exists()) {
                wasmCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            // Non-fatal directory initialization
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "StreamClean Downloads"
            val descriptionText = "Notifications for active video and audio downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            Log.w(TAG, "System memory running low (level: $level). Cleaning up caches.")
            // Perform light memory release
            System.gc()
        }
    }

    companion object {
        private const val TAG = "StreamCleanApp"
        const val CHANNEL_ID = "streamclean_download_channel"

        lateinit var instance: StreamCleanApplication
            private set
    }
}
