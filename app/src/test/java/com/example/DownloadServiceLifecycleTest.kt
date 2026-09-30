package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.service.DownloadService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadServiceLifecycleTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testServiceIntentActions() {
        val cancelIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
            putExtra(DownloadService.EXTRA_TASK_ID, 1001L)
        }
        assertEquals(DownloadService.ACTION_CANCEL, cancelIntent.action)
        assertEquals(1001L, cancelIntent.getLongExtra(DownloadService.EXTRA_TASK_ID, -1L))

        val pauseIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE
            putExtra(DownloadService.EXTRA_TASK_ID, 1002L)
        }
        assertEquals(DownloadService.ACTION_PAUSE, pauseIntent.action)
        assertEquals(1002L, pauseIntent.getLongExtra(DownloadService.EXTRA_TASK_ID, -1L))

        val resumeIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
            putExtra(DownloadService.EXTRA_TASK_ID, 1003L)
        }
        assertEquals(DownloadService.ACTION_RESUME, resumeIntent.action)
        assertEquals(1003L, resumeIntent.getLongExtra(DownloadService.EXTRA_TASK_ID, -1L))
    }

    @Test
    fun testServiceCreationAndStartCommand() {
        val serviceController = Robolectric.buildService(DownloadService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        val startId = service.onStartCommand(null, 0, 1)
        assertEquals(android.app.Service.START_STICKY, startId)

        serviceController.destroy()
    }

    @Test
    fun testServiceCancelActionWhenNoWorkStopsService() {
        val serviceController = Robolectric.buildService(DownloadService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        val cancelIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
            putExtra(DownloadService.EXTRA_TASK_ID, 9999L)
        }
        val result = service.onStartCommand(cancelIntent, 0, 2)
        assertEquals(android.app.Service.START_NOT_STICKY, result)

        serviceController.destroy()
    }

    @Test
    fun testServicePauseActionWhenNoWorkStopsService() {
        val serviceController = Robolectric.buildService(DownloadService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        val pauseIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE
            putExtra(DownloadService.EXTRA_TASK_ID, 8888L)
        }
        val result = service.onStartCommand(pauseIntent, 0, 3)
        assertEquals(android.app.Service.START_NOT_STICKY, result)

        serviceController.destroy()
    }

    @Test
    fun testServiceResumeAction() {
        val serviceController = Robolectric.buildService(DownloadService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        val resumeIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME
            putExtra(DownloadService.EXTRA_TASK_ID, 7777L)
        }
        val result = service.onStartCommand(resumeIntent, 0, 4)
        assertEquals(android.app.Service.START_STICKY, result)

        serviceController.destroy()
    }

    @Test
    fun testServiceNotificationChannelCreation() {
        val serviceController = Robolectric.buildService(DownloadService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(StreamCleanApplication.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals("StreamClean Downloads", channel.name)

        serviceController.destroy()
    }

    @Test
    fun testServiceCompanionMethodsSafeExecution() {
        // Test that companion methods do not crash
        DownloadService.start(context)
        DownloadService.pause(context, 100L)
        DownloadService.resume(context, 100L)
        DownloadService.stop(context)
        assertTrue(true)
    }

    @Test
    fun testServiceOnTrimMemoryCritical() {
        val serviceController = Robolectric.buildService(DownloadService::class.java)
        val service = serviceController.create().get()
        assertNotNull(service)

        // Trigger onTrimMemory critical level
        service.onTrimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        assertTrue(true)

        serviceController.destroy()
    }
}
