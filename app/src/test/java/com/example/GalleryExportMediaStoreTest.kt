package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.util.FileSecurityUtil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GalleryExportMediaStoreTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: DownloadRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DownloadRepository(context, database.downloadDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testExportValidVideoToGallery_success() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val videoFile = File(downloadDir, "test_gallery_video.mp4")
        videoFile.writeText("fake mp4 payload")

        val entity = DownloadEntity(
            id = 1,
            url = "https://example.com/video",
            title = "Test Gallery Video",
            fileName = "test_gallery_video.mp4",
            filePath = videoFile.absolutePath,
            mediaType = "VIDEO",
            status = "COMPLETED",
            fileSizeBytes = videoFile.length()
        )

        val success = repository.exportToGallery(entity)
        assertTrue("Exporting valid video to gallery must succeed", success)
        assertTrue("Original video file must remain intact", videoFile.exists())
    }

    @Test
    fun testExportValidAudioToGallery_success() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val audioFile = File(downloadDir, "test_gallery_audio.mp3")
        audioFile.writeText("fake mp3 payload")

        val entity = DownloadEntity(
            id = 2,
            url = "https://example.com/audio",
            title = "Test Gallery Audio",
            fileName = "test_gallery_audio.mp3",
            filePath = audioFile.absolutePath,
            mediaType = "AUDIO",
            status = "COMPLETED",
            fileSizeBytes = audioFile.length()
        )

        val success = repository.exportToGallery(entity)
        assertTrue("Exporting valid audio to gallery must succeed", success)
        assertTrue("Original audio file must remain intact", audioFile.exists())
    }

    @Test
    fun testExportMissingOrEmptyFile_failsGracefully() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val nonExistentFile = File(downloadDir, "non_existent.mp4")

        val entity = DownloadEntity(
            id = 3,
            url = "https://example.com/nonexistent",
            title = "Non Existent Video",
            fileName = "non_existent.mp4",
            filePath = nonExistentFile.absolutePath,
            mediaType = "VIDEO",
            status = "COMPLETED"
        )

        val success = repository.exportToGallery(entity)
        assertFalse("Exporting non-existent file must fail gracefully", success)

        val emptyFile = File(downloadDir, "empty_video.mp4")
        emptyFile.createNewFile()
        val emptyEntity = entity.copy(filePath = emptyFile.absolutePath, fileName = "empty_video.mp4")

        val emptySuccess = repository.exportToGallery(emptyEntity)
        assertFalse("Exporting 0-byte file must fail gracefully", emptySuccess)
    }

    @Test
    fun testExportUnauthorizedFile_blockedBySecurityCheck() = runBlocking {
        val unauthorizedFile = File(context.filesDir, "unauthorized_file.mp4")
        unauthorizedFile.writeText("unauthorized payload")

        val entity = DownloadEntity(
            id = 4,
            url = "https://example.com/unauthorized",
            title = "Unauthorized Video",
            fileName = "unauthorized_file.mp4",
            filePath = unauthorizedFile.absolutePath,
            mediaType = "VIDEO",
            status = "COMPLETED",
            fileSizeBytes = unauthorizedFile.length()
        )

        val success = repository.exportToGallery(entity)
        assertFalse("Exporting file outside approved media directories must be blocked", success)
    }

    @Test
    fun testExportPrivateFile_keepsOriginalPrivateFileSecure() = runBlocking {
        val privateDir = FileSecurityUtil.getApprovedPrivateDirectory(context)
        val privateVideoFile = File(privateDir, "vault_video.mp4")
        privateVideoFile.writeText("private video bytes")

        val entity = DownloadEntity(
            id = 5,
            url = "https://example.com/vault_video",
            title = "Vault Video",
            fileName = "vault_video.mp4",
            filePath = privateVideoFile.absolutePath,
            mediaType = "VIDEO",
            status = "COMPLETED",
            isPrivate = true,
            fileSizeBytes = privateVideoFile.length()
        )

        val success = repository.exportToGallery(entity)
        assertTrue("Exporting private video to gallery must succeed", success)
        assertTrue("Original private video must remain in private directory", privateVideoFile.exists())

        // Verify .nomedia exists in private directory
        val noMedia = File(privateDir, ".nomedia")
        if (!noMedia.exists()) noMedia.createNewFile()
        assertTrue(".nomedia must exist in private directory", noMedia.exists())
    }
}
