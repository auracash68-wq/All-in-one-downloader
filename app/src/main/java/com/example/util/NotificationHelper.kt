package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.MainActivity
import com.example.R
import com.example.StreamCleanApplication

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    const val COMPLETION_CHANNEL_ID = "streamclean_download_complete_channel"
    private const val NOTIFICATION_ID = 2001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Download Completion"
            val descriptionText = "Alerts when a video or audio download finishes in the background"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(COMPLETION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks app lifecycle and triggers a completion notification ONLY if the app is in
     * the BACKGROUND or CLOSED state. If the app is in the FOREGROUND, no notification is posted.
     */
    fun showDownloadCompleteNotification(
        context: Context,
        title: String = "Download Complete",
        body: String = "Video download successful. Please check the Downloads section."
    ) {
        try {
            // Check if notifications are enabled in user settings
            val settings = StreamCleanApplication.instance.settingsManager
            if (!settings.notificationsEnabled.value) {
                Log.d(TAG, "Notifications disabled in settings. Skipping notification.")
                return
            }

            // Check if app is in foreground
            val isAppInForeground = try {
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking ProcessLifecycleOwner: ${e.message}")
                false
            }

            if (isAppInForeground) {
                Log.d(TAG, "App is in FOREGROUND. Skipping notification as per specification.")
                return
            }

            // Ensure notification channel exists
            createNotificationChannels(context)

            // Check runtime permission for Android 13+ (API 33+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot post notification.")
                    return
                }
            }

            // PendingIntent to launch app and navigate to Downloads screen
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAVIGATE_TAB, "DOWNLOADS")
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, COMPLETION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_streamclean_logo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "Download complete notification posted successfully for background state.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post download complete notification", e)
        }
    }
}
