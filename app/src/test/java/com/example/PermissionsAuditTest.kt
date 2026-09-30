package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionsAuditTest {

    @Test
    fun testManifestContainsOnlyRequiredPermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()

        // Required permissions
        assertTrue("Manifest must declare INTERNET", content.contains("android.permission.INTERNET"))
        assertTrue("Manifest must declare ACCESS_NETWORK_STATE", content.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue("Manifest must declare POST_NOTIFICATIONS", content.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue("Manifest must declare FOREGROUND_SERVICE", content.contains("android.permission.FOREGROUND_SERVICE\""))
        assertTrue("Manifest must declare FOREGROUND_SERVICE_DATA_SYNC", content.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
        assertTrue("Manifest must declare WAKE_LOCK", content.contains("android.permission.WAKE_LOCK"))
    }

    @Test
    fun testManifestDoesNotContainUnnecessaryStoragePermissions() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val content = manifestFile.readText()

        // Unnecessary storage permissions removed
        assertFalse("Manifest must NOT declare READ_MEDIA_VIDEO", content.contains("android.permission.READ_MEDIA_VIDEO"))
        assertFalse("Manifest must NOT declare READ_MEDIA_AUDIO", content.contains("android.permission.READ_MEDIA_AUDIO"))
        assertFalse("Manifest must NOT declare READ_MEDIA_IMAGES", content.contains("android.permission.READ_MEDIA_IMAGES"))
        assertFalse("Manifest must NOT declare READ_EXTERNAL_STORAGE", content.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse("Manifest must NOT declare WRITE_EXTERNAL_STORAGE", content.contains("android.permission.WRITE_EXTERNAL_STORAGE"))
        assertFalse("Manifest must NOT declare MANAGE_EXTERNAL_STORAGE", content.contains("android.permission.MANAGE_EXTERNAL_STORAGE"))
    }
}
