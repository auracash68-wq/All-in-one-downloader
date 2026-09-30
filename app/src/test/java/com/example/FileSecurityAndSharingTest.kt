package com.example

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.example.util.FileSecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FileSecurityAndSharingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSanitizeFileName_pathTraversal() {
        val input = "../../../etc/passwd"
        val sanitized = FileSecurityUtil.sanitizeFileName(input)
        assertFalse("Must not contain ../ traversal", sanitized.contains(".."))
        assertFalse("Must not contain /", sanitized.contains("/"))
        assertEquals("etc_passwd", sanitized)
    }

    @Test
    fun testSanitizeFileName_windowsTraversalAndSeparators() {
        val input = "..\\..\\windows\\system32\\cmd.exe"
        val sanitized = FileSecurityUtil.sanitizeFileName(input)
        assertFalse("Must not contain ..", sanitized.contains(".."))
        assertFalse("Must not contain backslash", sanitized.contains("\\"))
        assertEquals("windows_system32_cmd.exe", sanitized)
    }

    @Test
    fun testSanitizeFileName_controlAndIllegalCharacters() {
        val input = "Video\u0000Title\r\n<Special>:\"Chars\"|?*~;"
        val sanitized = FileSecurityUtil.sanitizeFileName(input)
        assertFalse("Must not contain null char", sanitized.contains("\u0000"))
        assertFalse("Must not contain newline", sanitized.contains("\n"))
        assertFalse("Must not contain carriage return", sanitized.contains("\r"))
        assertFalse("Must not contain forbidden chars", sanitized.contains(Regex("[<>:\"|?*~;]")))
        assertEquals("VideoTitle_Special_Chars", sanitized)
    }

    @Test
    fun testSanitizeFileName_reservedDeviceNames() {
        val reservedNames = listOf("CON", "PRN", "AUX", "NUL", "COM1", "COM9", "LPT1", "LPT9")
        for (name in reservedNames) {
            val sanitized = FileSecurityUtil.sanitizeFileName(name)
            assertTrue("Reserved name $name must be safely prefixed", sanitized.startsWith("safe_"))
        }

        val reservedWithExt = FileSecurityUtil.sanitizeFileName("aux.mp4")
        assertTrue("Reserved name aux.mp4 must be safely prefixed", reservedWithExt.startsWith("safe_"))
    }

    @Test
    fun testSanitizeFileName_emptyOrBlank() {
        assertEquals("media_download", FileSecurityUtil.sanitizeFileName(""))
        assertEquals("media_download", FileSecurityUtil.sanitizeFileName("   "))
        assertEquals("media_download", FileSecurityUtil.sanitizeFileName("...___---"))
    }

    @Test
    fun testSanitizeFileName_lengthEnforcement() {
        val veryLongName = "A".repeat(120) + ".mp4"
        val sanitized = FileSecurityUtil.sanitizeFileName(veryLongName)
        assertTrue("Sanitized filename must not exceed 60 characters", sanitized.length <= 60)
        assertTrue("Extension must be preserved if space allows", sanitized.endsWith(".mp4"))
    }

    @Test
    fun testPathInApprovedDirectory_downloadDirectory() {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val validFile = File(downloadDir, "valid_video.mp4")
        validFile.createNewFile()

        assertTrue(
            "File in approved download directory must be accepted",
            FileSecurityUtil.isPathInApprovedDirectory(context, validFile, allowPrivate = false)
        )
    }

    @Test
    fun testPathInApprovedDirectory_privateDirectory() {
        val privateDir = FileSecurityUtil.getApprovedPrivateDirectory(context)
        val privateFile = File(privateDir, "private_video.mp4")
        privateFile.createNewFile()

        assertTrue(
            "Private file must be approved when allowPrivate=true",
            FileSecurityUtil.isPathInApprovedDirectory(context, privateFile, allowPrivate = true)
        )
        assertFalse(
            "Private file must NOT be approved when allowPrivate=false",
            FileSecurityUtil.isPathInApprovedDirectory(context, privateFile, allowPrivate = false)
        )
    }

    @Test
    fun testPathInApprovedDirectory_rejectsInternalDatabasesAndSharedPrefs() {
        val appDataDir = context.applicationInfo.dataDir
        if (appDataDir != null) {
            val dbFile = File(appDataDir, "databases/app_database")
            assertFalse(
                "Database file must be rejected as unapproved",
                FileSecurityUtil.isPathInApprovedDirectory(context, dbFile, allowPrivate = true)
            )

            val spFile = File(appDataDir, "shared_prefs/settings.xml")
            assertFalse(
                "SharedPreferences file must be rejected as unapproved",
                FileSecurityUtil.isPathInApprovedDirectory(context, spFile, allowPrivate = true)
            )
        }
    }

    @Test
    fun testValidateShareableFile_validFile() {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val validFile = File(downloadDir, "share_test.mp4")
        validFile.writeText("sample video payload")

        val validated = FileSecurityUtil.validateShareableFile(context, validFile)
        assertNotNull(validated)
        assertEquals(validFile.canonicalPath, validated.canonicalPath)
    }

    @Test
    fun testValidateShareableFile_rejectsMissingOrEmptyFile() {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val emptyFile = File(downloadDir, "empty.mp4")
        emptyFile.createNewFile()

        try {
            FileSecurityUtil.validateShareableFile(context, emptyFile)
            fail("Expected IllegalArgumentException for 0-byte file")
        } catch (e: IllegalArgumentException) {
            // Success
        }

        val nonExistentFile = File(downloadDir, "does_not_exist.mp4")
        try {
            FileSecurityUtil.validateShareableFile(context, nonExistentFile)
            fail("Expected IllegalArgumentException for missing file")
        } catch (e: IllegalArgumentException) {
            // Success
        }
    }

    @Test
    fun testValidateShareableFile_rejectsUnauthorizedInternalFile() {
        val internalFile = File(context.filesDir, "secret_key.txt")
        internalFile.writeText("super_secret_payload")

        try {
            FileSecurityUtil.validateShareableFile(context, internalFile)
            fail("Expected SecurityException when validating internal app file outside approved media directories")
        } catch (e: SecurityException) {
            // Success
        }
    }

    @Test
    fun testCreateShareIntent_setsFlagsAndClipData() {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val testVideo = File(downloadDir, "share_me.mp4")
        testVideo.writeText("test video bytes")

        val intent = FileSecurityUtil.createShareIntent(
            context = context,
            file = testVideo,
            title = "My Cool Video",
            mediaType = "VIDEO"
        )

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("video/mp4", intent.type)
        assertEquals("My Cool Video", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertNotNull("EXTRA_STREAM must be populated", intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertNotNull("ClipData must be populated for secure URI permission granting", intent.clipData)
        assertTrue(
            "FLAG_GRANT_READ_URI_PERMISSION must be present",
            (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0
        )
    }

    @Test
    fun testFilepathsXmlAudit() {
        val filepathsFile = File("src/main/res/xml/filepaths.xml")
        if (filepathsFile.exists()) {
            val xml = filepathsFile.readText()

            // Must NOT expose root external storage (<external-path)
            assertFalse(
                "filepaths.xml must not contain <external-path (root storage exposure)",
                xml.contains("<external-path")
            )

            // Must NOT expose internal app files (<files-path)
            assertFalse(
                "filepaths.xml must not contain <files-path (internal data sandbox exposure)",
                xml.contains("<files-path")
            )

            // Must NOT expose cache (<cache-path or <external-cache-path)
            assertFalse(
                "filepaths.xml must not contain <cache-path",
                xml.contains("<cache-path")
            )
            assertFalse(
                "filepaths.xml must not contain <external-cache-path",
                xml.contains("<external-cache-path")
            )

            // Must contain restricted <external-files-path with Download and private paths
            assertTrue(
                "filepaths.xml must contain external-files-path for Download",
                xml.contains("path=\"Download\"")
            )
            assertTrue(
                "filepaths.xml must contain external-files-path for private",
                xml.contains("path=\"private\"")
            )
        }
    }

    @Test
    fun testAndroidManifestFileProviderAudit() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            val manifest = manifestFile.readText()

            assertTrue(
                "FileProvider must have android:exported=\"false\"",
                manifest.contains("android:exported=\"false\"")
            )
            assertTrue(
                "FileProvider must have android:grantUriPermissions=\"true\"",
                manifest.contains("android:grantUriPermissions=\"true\"")
            )
            assertTrue(
                "FileProvider authority must use applicationId.fileprovider",
                manifest.contains("android:authorities=\"\${applicationId}.fileprovider\"")
            )
        }
    }
}
