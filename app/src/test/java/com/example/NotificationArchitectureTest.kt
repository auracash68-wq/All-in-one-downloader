package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.SettingsManager
import com.example.util.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationArchitectureTest {

    private lateinit var context: Context
    private lateinit var settingsManager: SettingsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsManager = SettingsManager(context)
    }

    @Test
    fun testNotificationChannelsCreation() {
        NotificationHelper.createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Verify Completion Channel
        val completionChannel = notificationManager.getNotificationChannel(NotificationHelper.COMPLETION_CHANNEL_ID)
        assertNotNull("Completion channel should exist", completionChannel)
        assertEquals("Download Completion", completionChannel.name)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, completionChannel.importance)

        // Verify Alerts Channel
        val alertsChannel = notificationManager.getNotificationChannel(NotificationHelper.ALERTS_CHANNEL_ID)
        assertNotNull("Alerts channel should exist", alertsChannel)
        assertEquals("Download Alerts", alertsChannel.name)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, alertsChannel.importance)

        // Verify Active Downloads Channel
        val ongoingChannel = notificationManager.getNotificationChannel(StreamCleanApplication.CHANNEL_ID)
        assertNotNull("Ongoing downloads channel should exist", ongoingChannel)
        assertEquals("StreamClean Downloads", ongoingChannel.name)
        assertEquals(NotificationManager.IMPORTANCE_LOW, ongoingChannel.importance)
    }

    @Test
    fun testNotificationPermissionCheckDoesNotCrash() {
        // hasNotificationPermission should return a boolean safely without throwing
        val hasPermission = NotificationHelper.hasNotificationPermission(context)
        // In Robolectric tests, notifications are enabled by default for test contexts
        assertNotNull(hasPermission)
    }

    @Test
    fun testSafePostingWhenNotificationsDisabledInSettings() {
        settingsManager.setNotificationsEnabled(false)

        // Should return early safely without throwing any exceptions
        NotificationHelper.showDownloadCompleteNotification(
            context = context,
            title = "Test Video",
            body = "Completed",
            taskId = 1234L
        )

        NotificationHelper.showDownloadFailedNotification(
            context = context,
            title = "Test Video",
            errorMessage = "Network timeout",
            taskId = 1234L
        )

        NotificationHelper.showDownloadCancelledNotification(
            context = context,
            title = "Test Video",
            taskId = 1234L
        )

        // Restore settings
        settingsManager.setNotificationsEnabled(true)
        assertTrue(settingsManager.notificationsEnabled.value)
    }

    @Test
    fun testPermissionAskedTracking() {
        assertFalse(settingsManager.isNotificationPermissionAsked())
        settingsManager.setNotificationPermissionAsked(true)
        assertTrue(settingsManager.isNotificationPermissionAsked())
        settingsManager.setNotificationPermissionAsked(false)
        assertFalse(settingsManager.isNotificationPermissionAsked())
    }

    @Test
    fun testSafePostingCallsDoNotCrash() {
        settingsManager.setNotificationsEnabled(true)

        // All posting methods must be non-crashing under any conditions
        NotificationHelper.showDownloadCompleteNotification(
            context = context,
            title = "Test Success",
            body = "Finished successfully",
            taskId = 555L
        )

        NotificationHelper.showDownloadFailedNotification(
            context = context,
            title = "Test Fail",
            errorMessage = "Server 500 error",
            taskId = 555L
        )

        NotificationHelper.showDownloadCancelledNotification(
            context = context,
            title = "Test Cancel",
            taskId = 555L
        )

        assertTrue(true)
    }
}
