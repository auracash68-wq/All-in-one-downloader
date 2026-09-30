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
    const val ALERTS_CHANNEL_ID = "streamclean_download_alerts_channel"

    const val COMPLETION_NOTIFICATION_ID = 2001
    const val FAILURE_NOTIFICATION_ID = 2002
    const val CANCELLATION_NOTIFICATION_ID = 2003

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Completion Channel (High Importance)
            val completionName = "Download Completion"
            val completionDesc = "Alerts when a video or audio download finishes"
            val completionChannel = NotificationChannel(
                COMPLETION_CHANNEL_ID,
                completionName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = completionDesc
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(completionChannel)

            // 2. Alerts Channel (Default Importance) for Failures / Cancellations
            val alertsName = "Download Alerts"
            val alertsDesc = "Alerts when a download encounters an error or is cancelled"
            val alertsChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                alertsName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = alertsDesc
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertsChannel)

            // 3. Ongoing Download Channel (Low Importance)
            val ongoingName = "StreamClean Downloads"
            val ongoingDesc = "Notifications for active video and audio downloads"
            val ongoingChannel = NotificationChannel(
                StreamCleanApplication.CHANNEL_ID,
                ongoingName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = ongoingDesc
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(ongoingChannel)
        }
    }

    /**
     * Safely checks whether notifications can be posted on the current device and OS.
     * On Android 13+ (API 33+), checks POST_NOTIFICATIONS permission.
     * On earlier versions, checks NotificationManagerCompat.areNotificationsEnabled().
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking notification permission: ${e.message}")
            false
        }
    }

    /**
     * Triggers a completion notification.
     * Respects user settings and runtime permissions without throwing exceptions.
     */
    fun showDownloadCompleteNotification(
        context: Context,
        title: String = "Download Complete",
        body: String = "Video download successful. Please check the Downloads section.",
        taskId: Long? = null
    ) {
        try {
            val settings = StreamCleanApplication.instance.settingsManager
            if (!settings.notificationsEnabled.value) {
                Log.d(TAG, "Notifications disabled in settings. Skipping completion notification.")
                return
            }

            if (!hasNotificationPermission(context)) {
                Log.d(TAG, "Notification permission not granted. Skipping completion notification safely.")
                return
            }

            val isAppInForeground = try {
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            } catch (e: Exception) {
                false
            }

            if (isAppInForeground) {
                Log.d(TAG, "App is in FOREGROUND. Skipping notification as user sees UI cards directly.")
                return
            }

            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAVIGATE_TAB, "DOWNLOADS")
            }

            val notificationId = taskId?.let { (2000 + (it % 1000)).toInt() } ?: COMPLETION_NOTIFICATION_ID
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
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

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "Download complete notification posted successfully (id: $notificationId).")
        } catch (e: Exception) {
            Log.e(TAG, "Safely caught error posting completion notification: ${e.message}", e)
        }
    }

    /**
     * Triggers a download failed notification.
     */
    fun showDownloadFailedNotification(
        context: Context,
        title: String,
        errorMessage: String,
        taskId: Long? = null
    ) {
        try {
            val settings = StreamCleanApplication.instance.settingsManager
            if (!settings.notificationsEnabled.value) return

            if (!hasNotificationPermission(context)) return

            val isAppInForeground = try {
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            } catch (e: Exception) {
                false
            }

            if (isAppInForeground) return

            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAVIGATE_TAB, "DOWNLOADS")
            }

            val notificationId = taskId?.let { (3000 + (it % 1000)).toInt() } ?: FAILURE_NOTIFICATION_ID
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
                .setContentTitle("Download Failed: $title")
                .setContentText(errorMessage.ifEmpty { "An unexpected error occurred during download" })
                .setSmallIcon(R.drawable.ic_streamclean_logo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "Download failed notification posted successfully (id: $notificationId).")
        } catch (e: Exception) {
            Log.e(TAG, "Safely caught error posting failed notification: ${e.message}", e)
        }
    }

    /**
     * Triggers a download cancelled notification.
     */
    fun showDownloadCancelledNotification(
        context: Context,
        title: String,
        taskId: Long? = null
    ) {
        try {
            val settings = StreamCleanApplication.instance.settingsManager
            if (!settings.notificationsEnabled.value) return

            if (!hasNotificationPermission(context)) return

            val isAppInForeground = try {
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            } catch (e: Exception) {
                false
            }

            if (isAppInForeground) return

            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAVIGATE_TAB, "DOWNLOADS")
            }

            val notificationId = taskId?.let { (4000 + (it % 1000)).toInt() } ?: CANCELLATION_NOTIFICATION_ID
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
                .setContentTitle("Download Cancelled: $title")
                .setContentText("The download was cancelled.")
                .setSmallIcon(R.drawable.ic_streamclean_logo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "Download cancelled notification posted successfully (id: $notificationId).")
        } catch (e: Exception) {
            Log.e(TAG, "Safely caught error posting cancelled notification: ${e.message}", e)
        }
    }
}
