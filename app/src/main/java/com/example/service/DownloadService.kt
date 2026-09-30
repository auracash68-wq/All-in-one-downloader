package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StreamCleanApplication
import com.example.engine.DownloadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private lateinit var downloadEngine: DownloadEngine
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StreamClean:DownloadServiceWakeLock")?.apply {
                setReferenceCounted(false)
            }
        }
        try {
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(60 * 60 * 1000L /* 1 hour max */)
                    Log.d(TAG, "Acquired partial WakeLock for active download")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Released partial WakeLock")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock: ${e.message}")
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (manager != null && manager.getNotificationChannel(StreamCleanApplication.CHANNEL_ID) == null) {
                val name = "StreamClean Downloads"
                val descriptionText = "Notifications for active video and audio downloads"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(StreamCleanApplication.CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun startServiceForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}", e)
        }
    }

    private fun stopServiceGracefully() {
        Log.i(TAG, "Stopping DownloadService gracefully: releasing wake lock and removing foreground notification")
        releaseWakeLock()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground: ${e.message}")
        }
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        downloadEngine = DownloadEngine.getInstance(this)

        val initialActive = downloadEngine.activeDownload.value
        val initialNotif = if (initialActive != null && initialActive.isRunning) {
            val prefix = if (initialActive.queueTotal > 1) "[${initialActive.queueIndex}/${initialActive.queueTotal}] " else ""
            buildNotification(
                title = "$prefix${initialActive.title.ifEmpty { initialActive.fileName }}",
                progress = initialActive.progress,
                isIndeterminate = initialActive.isVerifying || (initialActive.progress == 0 && initialActive.speed == "Starting..."),
                speed = initialActive.speed,
                eta = initialActive.eta,
                taskId = initialActive.id
            )
        } else {
            buildNotification("Preparing download...", 0, true)
        }
        startServiceForeground(initialNotif)

        serviceScope.launch {
            downloadEngine.activeDownload.collectLatest { state ->
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (state != null && state.isRunning) {
                    acquireWakeLock()
                    val prefix = if (state.queueTotal > 1) "[${state.queueIndex}/${state.queueTotal}] " else ""
                    val notif = buildNotification(
                        title = "$prefix${state.title.ifEmpty { state.fileName }}",
                        progress = state.progress,
                        isIndeterminate = state.isVerifying || (state.progress == 0 && (state.speed == "Starting..." || state.speed.isEmpty())),
                        speed = state.speed,
                        eta = state.eta,
                        taskId = state.id
                    )
                    manager?.notify(NOTIFICATION_ID, notif)
                } else if (state != null && state.isCompleted && state.queueTotal > 1 && state.queueIndex == state.queueTotal) {
                    // Multiple Download Batch Success Notification
                    releaseWakeLock()
                    val batchSuccessNotif = buildBatchSuccessNotification(state.queueTotal)
                    manager?.notify(BATCH_NOTIFICATION_ID, batchSuccessNotif)
                    if (!downloadEngine.hasActiveOrQueuedTasks()) {
                        stopServiceGracefully()
                    }
                } else if (state != null && (state.isCompleted || state.isFailed || state.isCancelled || state.isPaused)) {
                    releaseWakeLock()
                    if (downloadEngine.hasActiveOrQueuedTasks()) {
                        val queueNotif = buildNotification("Processing queue...", 0, true)
                        manager?.notify(NOTIFICATION_ID, queueNotif)
                    } else {
                        stopServiceGracefully()
                    }
                } else if (state == null) {
                    if (downloadEngine.hasActiveOrQueuedTasks()) {
                        val preparingNotif = buildNotification("Preparing download...", 0, true)
                        manager?.notify(NOTIFICATION_ID, preparingNotif)
                    } else {
                        // Allow brief grace window for pending enqueue operations before shutting down
                        delay(1500L)
                        if (!downloadEngine.hasActiveOrQueuedTasks() && downloadEngine.activeDownload.value == null) {
                            stopServiceGracefully()
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannel()

        if (intent == null) {
            // System restarted the service after killing it (START_STICKY)
            Log.i(TAG, "DownloadService restarted by system after process recreation")
            if (!downloadEngine.hasActiveOrQueuedTasks()) {
                serviceScope.launch {
                    val app = application as? StreamCleanApplication
                    val repo = app?.repository
                    val interrupted = repo?.getInterruptedDownloads() ?: emptyList()
                    if (interrupted.isEmpty()) {
                        stopServiceGracefully()
                    } else {
                        Log.i(TAG, "Found ${interrupted.size} interrupted downloads on service restart")
                        downloadEngine.recoverInterruptedDownloads()
                    }
                }
            }
            return START_STICKY
        }

        when (intent.action) {
            ACTION_CANCEL -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    downloadEngine.cancelDownload(taskId)
                } else {
                    downloadEngine.cancelActiveDownload()
                }
                if (!downloadEngine.hasActiveOrQueuedTasks()) {
                    stopServiceGracefully()
                }
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    downloadEngine.pauseDownload(taskId)
                } else {
                    downloadEngine.pauseActiveDownload()
                }
                if (!downloadEngine.hasActiveOrQueuedTasks()) {
                    stopServiceGracefully()
                }
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    downloadEngine.resumeTask(taskId)
                }
            }
            else -> {
                // If service started with no active/queued tasks, check grace window
                if (!downloadEngine.hasActiveOrQueuedTasks() && downloadEngine.activeDownload.value == null) {
                    serviceScope.launch {
                        delay(1500L)
                        if (!downloadEngine.hasActiveOrQueuedTasks() && downloadEngine.activeDownload.value == null) {
                            Log.i(TAG, "No active or queued work found after startup grace period. Stopping service.")
                            stopServiceGracefully()
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "onTrimMemory called with level $level")
        if (level >= TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) {
            Log.e(TAG, "Critical memory threshold reached! Pausing active downloads to prevent OOM crash.")
            downloadEngine.pauseForLowMemory()
            releaseWakeLock()
            stopServiceGracefully()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "DownloadService onDestroy invoked")
        if (::downloadEngine.isInitialized && downloadEngine.hasActiveOrQueuedTasks()) {
            Log.w(TAG, "DownloadService destroyed while tasks were active. Pausing active download to prevent orphaned processes.")
            try {
                downloadEngine.pauseActiveDownload()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing download on service destroy: ${e.message}")
            }
        }
        releaseWakeLock()
        serviceScope.cancel()
    }

    private fun buildNotification(
        title: String,
        progress: Int,
        isIndeterminate: Boolean,
        speed: String = "",
        eta: String = "",
        taskId: Long? = null
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (isIndeterminate && speed.isEmpty()) {
            "Preparing download..."
        } else {
            buildString {
                append("$progress%")
                if (speed.isNotBlank()) {
                    append(" • ")
                    append(speed)
                }
                if (eta.isNotBlank()) {
                    append(" • ETA: ")
                    append(eta)
                }
            }
        }

        val contentTitle = if (title.startsWith("Downloading:") || title.startsWith("Preparing") || title.startsWith("Processing")) {
            title
        } else {
            "Downloading: $title"
        }

        val builder = NotificationCompat.Builder(this, StreamCleanApplication.CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(subtext)
            .setSmallIcon(R.drawable.ic_streamclean_logo)
            .setProgress(100, progress, isIndeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (taskId != null) {
            val pauseIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                ((taskId % 10000).toInt() + 100).coerceAtLeast(0),
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)

            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val cancelPendingIntent = PendingIntent.getService(
                this,
                ((taskId % 10000).toInt() + 200).coerceAtLeast(0),
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        } else {
            val cancelIntent = Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            val cancelPendingIntent = PendingIntent.getService(
                this, 1, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
        }

        return builder.build()
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
        const val ACTION_PAUSE = "com.example.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.service.ACTION_RESUME"
        const val EXTRA_TASK_ID = "com.example.service.EXTRA_TASK_ID"

        fun start(context: Context) {
            try {
                val intent = Intent(context, DownloadService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start DownloadService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_CANCEL
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop DownloadService: ${e.message}", e)
            }
        }

        fun pause(context: Context, taskId: Long? = null) {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_PAUSE
                    if (taskId != null) putExtra(EXTRA_TASK_ID, taskId)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause DownloadService: ${e.message}", e)
            }
        }

        fun resume(context: Context, taskId: Long) {
            try {
                val intent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_RESUME
                    putExtra(EXTRA_TASK_ID, taskId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume DownloadService: ${e.message}", e)
            }
        }
    }
}
