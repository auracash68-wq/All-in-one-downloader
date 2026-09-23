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
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
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

        // Seed initial data matching design mockups
        CoroutineScope(Dispatchers.IO).launch {
            repository.checkAndSeedInitialData()
        }

        // Initialize YoutubeDL, FFmpeg and Aria2c engine
        initializeEngines()

        // Create Notification Channel for downloads
        createNotificationChannel()
    }

    private fun initializeEngines() {
        try {
            YoutubeDL.getInstance().init(this)
            Log.d(TAG, "YoutubeDL initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize YoutubeDL: ${e.message}", e)
        }

        try {
            FFmpeg.getInstance().init(this)
            Log.d(TAG, "FFmpeg initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize FFmpeg: ${e.message}", e)
        }

        try {
            Aria2c.getInstance().init(this)
            Log.d(TAG, "Aria2c initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Aria2c: ${e.message}", e)
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
