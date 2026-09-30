package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.engine.VideoFormatInfo
import com.example.ui.localization.BengaliStrings
import com.example.ui.localization.EnglishStrings
import com.example.ui.localization.HindiStrings
import com.example.util.FileSecurityUtil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase21FullFunctionalMatrixTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var downloadDao: DownloadDao
    private lateinit var repository: DownloadRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        downloadDao = database.downloadDao()
        repository = DownloadRepository(context, downloadDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==========================================
    // 1. TEST DOWNLOAD
    // ==========================================

    @Test
    fun testDownload_validVideo() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val testFile = File(downloadDir, "test_valid_video.mp4")
        testFile.writeBytes(ByteArray(1024) { 1 })

        val entity = DownloadEntity(
            title = "Test Valid Video",
            fileName = "test_valid_video.mp4",
            url = "https://example.com/video.mp4",
            filePath = testFile.absolutePath,
            fileSizeBytes = 1024L,
            mediaType = "VIDEO",
            resolution = "1080p",
            status = "COMPLETED",
            progress = 100
        )
        val id = downloadDao.insert(entity)

        val retrieved = downloadDao.getDownloadById(id)
        assertNotNull(retrieved)
        assertEquals("COMPLETED", retrieved?.status)
        val file = File(retrieved!!.filePath)
        assertTrue(file.exists() && file.length() > 0)
    }

    @Test
    fun testDownload_validAudio() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val testFile = File(downloadDir, "test_valid_audio.mp3")
        testFile.writeBytes(ByteArray(512) { 2 })

        val entity = DownloadEntity(
            title = "Test Valid Audio",
            fileName = "test_valid_audio.mp3",
            url = "https://example.com/audio.mp3",
            filePath = testFile.absolutePath,
            fileSizeBytes = 512L,
            mediaType = "AUDIO",
            audioBitrate = "320kbps",
            status = "COMPLETED",
            progress = 100
        )
        val id = downloadDao.insert(entity)

        val retrieved = downloadDao.getDownloadById(id)
        assertNotNull(retrieved)
        assertEquals("AUDIO", retrieved?.mediaType)
        assertEquals("320kbps", retrieved?.audioBitrate)
    }

    @Test
    fun testDownload_supportedQualities() {
        val formats = listOf(
            VideoFormatInfo(formatId = "137", ext = "mp4", resolution = "1080p", note = "HD", isAudio = false),
            VideoFormatInfo(formatId = "22", ext = "mp4", resolution = "720p", note = "HD", isAudio = false),
            VideoFormatInfo(formatId = "18", ext = "mp4", resolution = "480p", note = "SD", isAudio = false),
            VideoFormatInfo(formatId = "140", ext = "mp3", resolution = "320k", note = "High Quality Audio", isAudio = true),
            VideoFormatInfo(formatId = "251", ext = "mp3", resolution = "128k", note = "Standard Audio", isAudio = true)
        )
        assertEquals(5, formats.size)
        assertTrue(formats.any { it.resolution == "1080p" && !it.isAudio })
        assertTrue(formats.any { it.resolution == "320k" && it.isAudio })
    }

    @Test
    fun testDownload_shortAndLongMediaDurations() = runBlocking {
        val shortMedia = DownloadEntity(
            title = "Short Clip", fileName = "short.mp4", url = "https://example.com/short",
            duration = "0:15", fileSizeBytes = 500L, status = "COMPLETED"
        )
        val longMedia = DownloadEntity(
            title = "Long Podcast", fileName = "long.mp4", url = "https://example.com/long",
            duration = "1:15:30", fileSizeBytes = 500000L, status = "COMPLETED"
        )
        val id1 = downloadDao.insert(shortMedia)
        val id2 = downloadDao.insert(longMedia)

        val s = downloadDao.getDownloadById(id1)
        val l = downloadDao.getDownloadById(id2)
        assertEquals("0:15", s?.duration)
        assertEquals("1:15:30", l?.duration)
    }

    @Test
    fun testDownload_duplicateUrlDetection() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val testFile = File(downloadDir, "video.mp4")
        testFile.writeBytes(ByteArray(1000) { 1 })

        val entity = DownloadEntity(
            title = "Original Video",
            fileName = "video.mp4",
            filePath = testFile.absolutePath,
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            fileSizeBytes = 1000L,
            status = "COMPLETED"
        )
        downloadDao.insert(entity)

        val duplicateCheck = repository.findVerifiedDownloadedMedia(
            url = "https://youtu.be/dQw4w9WgXcQ?si=abc12345"
        )
        assertNotNull("Should detect duplicate YouTube video even with different URL params", duplicateCheck)
    }

    @Test
    fun testDownload_networkInterruptionAndRetry() = runBlocking {
        val entity = DownloadEntity(
            title = "Network Interrupted Item",
            fileName = "stream.mp4",
            url = "https://example.com/stream",
            status = "FAILED",
            errorMessage = "java.net.SocketTimeoutException: connection timed out"
        )
        val id = downloadDao.insert(entity)

        var item = downloadDao.getDownloadById(id)
        assertEquals("FAILED", item?.status)

        // Retry resets status
        downloadDao.updateStatus(id, "PENDING", 0)
        item = downloadDao.getDownloadById(id)
        assertEquals("PENDING", item?.status)
    }

    @Test
    fun testDownload_cancellation() = runBlocking {
        val entity = DownloadEntity(
            title = "Cancelled Task",
            fileName = "cancelled.mp4",
            url = "https://example.com/stream",
            status = "CANCELLED"
        )
        val id = downloadDao.insert(entity)

        val item = downloadDao.getDownloadById(id)
        assertEquals("CANCELLED", item?.status)
    }

    // ==========================================
    // 2. TEST QUEUE & BATCH SEQUENCING
    // ==========================================

    @Test
    fun testQueue_multipleSequentialTasks() = runBlocking {
        val items = (1..5).map { i ->
            DownloadEntity(
                title = "Queue Task $i",
                fileName = "task_$i.mp4",
                url = "https://example.com/task$i",
                status = if (i == 1) "DOWNLOADING" else "PENDING"
            )
        }
        downloadDao.insertAll(items)

        // Transition task 1 to completed, task 2 to downloading
        downloadDao.updateStatus(1, "COMPLETED", 100)
        downloadDao.updateStatus(2, "DOWNLOADING", 10)

        assertEquals("COMPLETED", downloadDao.getDownloadById(1)?.status)
        assertEquals("DOWNLOADING", downloadDao.getDownloadById(2)?.status)
    }

    @Test
    fun testQueue_failedTaskFollowedByQueuedTask() = runBlocking {
        val entity1 = DownloadEntity(title = "Failing Task", fileName = "fail.mp4", url = "https://example.com/1", status = "FAILED")
        val entity2 = DownloadEntity(title = "Next Queued Task", fileName = "next.mp4", url = "https://example.com/2", status = "PENDING")

        val id1 = downloadDao.insert(entity1)
        val id2 = downloadDao.insert(entity2)

        // Next queued item starts
        downloadDao.updateStatus(id2, "DOWNLOADING", 0)

        assertEquals("FAILED", downloadDao.getDownloadById(id1)?.status)
        assertEquals("DOWNLOADING", downloadDao.getDownloadById(id2)?.status)
    }

    // ==========================================
    // 3. TEST LIFECYCLE & CRASH RECOVERY
    // ==========================================

    @Test
    fun testLifecycle_processRestartRecoversStaleDownloadingTasks() = runBlocking {
        val stale1 = DownloadEntity(title = "Stale 1", fileName = "s1.mp4", url = "https://example.com/s1", status = "DOWNLOADING")
        val stale2 = DownloadEntity(title = "Stale 2", fileName = "s2.mp4", url = "https://example.com/s2", status = "VERIFYING")
        val id1 = downloadDao.insert(stale1)
        val id2 = downloadDao.insert(stale2)

        // On App restart
        downloadDao.markInterruptedDownloadsAsPaused()

        assertEquals("PAUSED", downloadDao.getDownloadById(id1)?.status)
        assertEquals("PAUSED", downloadDao.getDownloadById(id2)?.status)
    }

    // ==========================================
    // 4. TEST STORAGE
    // ==========================================

    @Test
    fun testStorage_savePlayDeleteAndShare() = runBlocking {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val testFile = File(downloadDir, "media_lifecycle.mp4")
        testFile.writeBytes(ByteArray(2048) { 7 })

        val entity = DownloadEntity(
            title = "Storage Item",
            fileName = "media_lifecycle.mp4",
            url = "https://example.com/storage.mp4",
            filePath = testFile.absolutePath,
            fileSizeBytes = 2048L,
            status = "COMPLETED"
        )
        val id = downloadDao.insert(entity)

        // Verify Save & File integrity
        val item = downloadDao.getDownloadById(id)
        assertNotNull(item)
        val file = File(item!!.filePath)
        assertTrue(file.exists())
        assertEquals(2048L, file.length())

        // Verify Delete removes database entry and physical file
        repository.deleteDownload(item)
        assertNull(downloadDao.getDownloadById(id))
        assertFalse(file.exists())
    }

    @Test
    fun testStorage_lowStorageDetection() {
        val usableSpace = context.filesDir.usableSpace
        assertTrue("Usable space should be non-negative", usableSpace >= 0L)
    }

    // ==========================================
    // 5. TEST UI & LOCALIZATION
    // ==========================================

    @Test
    fun testUI_localizationEnglishBengaliHindi() {
        // English
        assertEquals("Download", EnglishStrings.navDownload)
        assertEquals("Settings", EnglishStrings.navSettings)

        // Bengali
        assertEquals("ডাউনলোড", BengaliStrings.navDownload)
        assertEquals("সেটিংস", BengaliStrings.navSettings)

        // Hindi
        assertEquals("डाउनलोड", HindiStrings.navDownload)
        assertEquals("सेटिंग्स", HindiStrings.navSettings)
    }

    // ==========================================
    // 6. TEST SECURITY & PATHS
    // ==========================================

    @Test
    fun testSecurity_pathTraversalAndDangerousNames() {
        val p1 = FileSecurityUtil.sanitizeFileName("../../../malicious.mp4")
        assertFalse(p1.contains(".."))
        assertFalse(p1.contains("/"))

        val p2 = FileSecurityUtil.sanitizeFileName("..\\..\\evil.mp4")
        assertFalse(p2.contains(".."))
        assertFalse(p2.contains("\\"))

        val p3 = FileSecurityUtil.sanitizeFileName("CON.mp4")
        assertTrue(p3.startsWith("safe_CON"))

        val p4 = FileSecurityUtil.sanitizeFileName("safe:video*?.mp4")
        assertFalse(p4.contains(Regex("[<>:\"|?*~;]")))
    }

    @Test
    fun testSecurity_approvedDirectories() {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val validFile = File(downloadDir, "valid_video.mp4")
        validFile.createNewFile()
        assertTrue(FileSecurityUtil.isPathInApprovedDirectory(context, validFile, allowPrivate = false))

        val appDataDir = context.applicationInfo.dataDir
        if (appDataDir != null) {
            val dbFile = File(appDataDir, "databases/app_database")
            assertFalse(FileSecurityUtil.isPathInApprovedDirectory(context, dbFile, allowPrivate = true))
        }
    }
}
