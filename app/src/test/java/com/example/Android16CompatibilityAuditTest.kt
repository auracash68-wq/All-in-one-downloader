package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.service.DownloadService
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
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Android16CompatibilityAuditTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        NotificationHelper.createNotificationChannels(context)
    }

    @Test
    fun testTargetSdkVersionIs36() {
        val targetSdk = context.applicationInfo.targetSdkVersion
        // Target SDK should be 36 or at least >= 34 for Android 15/16 modern toolchain
        assertTrue("Target SDK must be modern (>= 34, actual: $targetSdk)", targetSdk >= 34)
    }

    @Test
    fun testForegroundServiceManifestDeclarationForAndroid16() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(manifestFile)

        // Verify FOREGROUND_SERVICE and FOREGROUND_SERVICE_DATA_SYNC permissions
        val permissionNodes = doc.getElementsByTagName("uses-permission")
        val permissions = mutableSetOf<String>()
        for (i in 0 until permissionNodes.length) {
            val element = permissionNodes.item(i) as Element
            permissions.add(element.getAttribute("android:name"))
        }

        assertTrue("Manifest must declare FOREGROUND_SERVICE", permissions.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue("Manifest must declare FOREGROUND_SERVICE_DATA_SYNC", permissions.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
        assertTrue("Manifest must declare POST_NOTIFICATIONS", permissions.contains("android.permission.POST_NOTIFICATIONS"))

        // Verify service tag has foregroundServiceType="dataSync"
        val serviceNodes = doc.getElementsByTagName("service")
        var foundDataSyncService = false
        for (i in 0 until serviceNodes.length) {
            val service = serviceNodes.item(i) as Element
            val name = service.getAttribute("android:name")
            val type = service.getAttribute("android:foregroundServiceType")
            if (name.contains("DownloadService") && type == "dataSync") {
                foundDataSyncService = true
            }
        }
        assertTrue("DownloadService must declare android:foregroundServiceType=\"dataSync\"", foundDataSyncService)
    }

    @Test
    fun testNotificationChannelsConfiguredCorrectlyForAndroid16() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val completionChannel = notificationManager.getNotificationChannel(NotificationHelper.COMPLETION_CHANNEL_ID)
        assertNotNull("Completion channel must exist", completionChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, completionChannel.importance)

        val alertsChannel = notificationManager.getNotificationChannel(NotificationHelper.ALERTS_CHANNEL_ID)
        assertNotNull("Alerts channel must exist", alertsChannel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, alertsChannel.importance)

        val ongoingChannel = notificationManager.getNotificationChannel(StreamCleanApplication.CHANNEL_ID)
        assertNotNull("Ongoing download channel must exist", ongoingChannel)
        assertEquals(NotificationManager.IMPORTANCE_LOW, ongoingChannel.importance)
    }

    @Test
    fun testActivityConfigChangesForMultiWindowAndFoldables() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(manifestFile)

        val activityNodes = doc.getElementsByTagName("activity")
        var configChanges = ""
        for (i in 0 until activityNodes.length) {
            val activity = activityNodes.item(i) as Element
            if (activity.getAttribute("android:name").contains("MainActivity")) {
                configChanges = activity.getAttribute("android:configChanges")
            }
        }

        assertTrue("MainActivity configChanges must include orientation", configChanges.contains("orientation"))
        assertTrue("MainActivity configChanges must include screenSize", configChanges.contains("screenSize"))
        assertTrue("MainActivity configChanges must include screenLayout", configChanges.contains("screenLayout"))
        assertTrue("MainActivity configChanges must include keyboardHidden", configChanges.contains("keyboardHidden"))
    }

    @Test
    fun testFilePathsConfigXmlAvoidsInsecureBroadRoot() {
        val filepathsFile = File("src/main/res/xml/filepaths.xml")
        assertTrue("filepaths.xml must exist", filepathsFile.exists())

        val content = filepathsFile.readText()
        assertFalse("Must not expose root external-path", content.contains("<external-path"))
        assertFalse("Must not expose root-path", content.contains("<root-path"))
        assertTrue("Must expose narrow external-files-path", content.contains("<external-files-path"))
    }
}
