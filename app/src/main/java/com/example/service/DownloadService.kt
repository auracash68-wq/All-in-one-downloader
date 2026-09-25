package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StreamCleanApplication
import com.example.engine.DownloadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var downloadEngine: DownloadEngine

    override fun onCreate() {
        super.onCreate()
        downloadEngine = DownloadEngine.getInstance(this)
        startForeground(NOTIFICATION_ID, buildNotification("Preparing download...", 0, true))

        serviceScope.launch {
            downloadEngine.activeDownload.collectLatest { state ->
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                if (state != null && state.isRunning) {
                    val prefix = if (state.queueTotal > 1) "[${state.queueIndex}/${state.queueTotal}] " else ""
                    val notif = buildNotification(
                        title = "$prefix${state.title.ifEmpty { state.fileName }}",
                        progress = state.progress,
                        isIndeterminate = false,
                        speed = state.speed
                    )
                    manager.notify(NOTIFICATION_ID, notif)
                } else if (state != null && state.isCompleted && state.queueTotal > 1 && state.queueIndex == state.queueTotal) {
                    // Feature 10: Multiple Download Success Notification
                    val batchSuccessNotif = buildBatchSuccessNotification(state.queueTotal)
                    manager.notify(BATCH_NOTIFICATION_ID, batchSuccessNotif)
                } else if (state == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            downloadEngine.cancelActiveDownload()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "onTrimMemory called with level $level")
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) {
            Log.e(TAG, "Critical memory threshold reached! Pausing active downloads to prevent OOM crash.")
            downloadEngine.pauseForLowMemory()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun buildNotification(
        title: String,
        progress: Int,
        isIndeterminate: Boolean,
        speed: String = ""
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (speed.isNotEmpty()) "$progress% • $speed" else "$progress%"

        return NotificationCompat.Builder(this, StreamCleanApplication.CHANNEL_ID)
            .setContentTitle("Downloading: $title")
            .setContentText(subtext)
            .setSmallIcon(R.drawable.ic_streamclean_logo)
            .setProgress(100, progress, isIndeterminate)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildBatchSuccessNotification(totalCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 2, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, StreamCleanApplication.CHANNEL_ID)
            .setContentTitle("✓ Multiple downloads successful")
            .setContentText("All $totalCount selected downloads have completed successfully.")
            .setSmallIcon(R.drawable.ic_streamclean_logo)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    companion object {
        private const val TAG = "DownloadService"
        const val NOTIFICATION_ID = 1001
        const val BATCH_NOTIFICATION_ID = 1002
        const val ACTION_CANCEL = "com.example.service.ACTION_CANCEL"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }
}
