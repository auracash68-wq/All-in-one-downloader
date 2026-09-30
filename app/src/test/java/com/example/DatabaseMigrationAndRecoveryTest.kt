package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.engine.DownloadEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationAndRecoveryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var dao: DownloadDao
    private lateinit var repository: DownloadRepository
    private lateinit var engine: DownloadEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.downloadDao()
        repository = DownloadRepository(context, dao)
        engine = DownloadEngine.getInstance(context)
        engine.resetForTesting()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRoomSchemaVersion4AndColumnsExist() {
        val db = database.openHelper.writableDatabase
        val cursor = db.query("PRAGMA table_info(downloads)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        cursor.close()

        assertTrue("Column downloadedBytes must exist in version 4", columns.contains("downloadedBytes"))
        assertTrue("Column totalBytes must exist in version 4", columns.contains("totalBytes"))
        assertTrue("Column isResumable must exist in version 4", columns.contains("isResumable"))
        assertTrue("Column tempFilePath must exist in version 4", columns.contains("tempFilePath"))
    }

    @Test
    fun testMigration3To4AlterTable() {
        val db = database.openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS test_downloads_v3 (id INTEGER PRIMARY KEY NOT NULL, title TEXT NOT NULL)")
        // Execute the exact SQL statements from MIGRATION_3_4
        db.execSQL("ALTER TABLE test_downloads_v3 ADD COLUMN downloadedBytes INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE test_downloads_v3 ADD COLUMN totalBytes INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE test_downloads_v3 ADD COLUMN isResumable INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE test_downloads_v3 ADD COLUMN tempFilePath TEXT NOT NULL DEFAULT ''")

        val cursor = db.query("PRAGMA table_info(test_downloads_v3)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        cursor.close()

        assertTrue(columns.contains("downloadedBytes"))
        assertTrue(columns.contains("totalBytes"))
        assertTrue(columns.contains("isResumable"))
        assertTrue(columns.contains("tempFilePath"))
    }

    @Test
    fun testStaleDownloadingRecoveryMarksAsPaused() = runBlocking {
        // Insert a download in DOWNLOADING state representing process death during active transfer
        val staleEntity = DownloadEntity(
            id = 5001L,
            url = "https://example.com/stream.mp4",
            title = "Interrupted Download",
            fileName = "stream_5001.mp4",
            filePath = "",
            fileSizeBytes = 10485760L,
            formattedSize = "10.0 MB",
            status = "DOWNLOADING",
            progress = 45,
            downloadedBytes = 4718592L,
            totalBytes = 10485760L,
            tempFilePath = "/tmp/fake_staging.mp4"
        )
        dao.insert(staleEntity)

        val interrupted = dao.getInterruptedDownloads()
        assertEquals(1, interrupted.size)
        assertEquals(5001L, interrupted[0].id)
        assertEquals("DOWNLOADING", interrupted[0].status)

        // Execute recovery
        engine.recoverInterruptedDownloads(repository)

        // Inspect recovered record
        val recovered = dao.getDownloadById(5001L)
        assertNotNull(recovered)
        assertEquals("PAUSED", recovered?.status)
        assertEquals(45, recovered?.progress)
        assertTrue(recovered?.errorMessage?.contains("Interrupted") == true)
        // Ensure corrupted partial file was NEVER marked as COMPLETED
        assertNotEquals("COMPLETED", recovered?.status)
    }

    @Test
    fun testStaleVerifyingRecoveryWithoutValidFileMarksAsPaused() = runBlocking {
        // Task was in VERIFYING when app was killed, but final file does not exist
        val staleVerifying = DownloadEntity(
            id = 5002L,
            url = "https://example.com/video2.mp4",
            title = "Unfinished Verification",
            fileName = "non_existent_file.mp4",
            filePath = "",
            status = "VERIFYING",
            progress = 100
        )
        dao.insert(staleVerifying)

        engine.recoverInterruptedDownloads(repository)

        val recovered = dao.getDownloadById(5002L)
        assertNotNull(recovered)
        // Must NOT be marked as COMPLETED because file is missing/unverified
        assertNotEquals("COMPLETED", recovered?.status)
        assertEquals("PAUSED", recovered?.status)
    }

    @Test
    fun testUpdateDownloadProgressAndStatusDetails() = runBlocking {
        val entity = DownloadEntity(
            id = 6001L,
            url = "https://example.com/video.mp4",
            title = "Progress Test",
            fileName = "progress_test.mp4",
            status = "DOWNLOADING",
            progress = 10
        )
        dao.insert(entity)

        dao.updateProgress(
            id = 6001L,
            progress = 55,
            downloadedBytes = 5500000L,
            totalBytes = 10000000L
        )

        val updated = dao.getDownloadById(6001L)
        assertEquals(55, updated?.progress)
        assertEquals(5500000L, updated?.downloadedBytes)
        assertEquals(10000000L, updated?.totalBytes)

        dao.updateStatusDetails(
            id = 6001L,
            status = "PAUSED",
            progress = 55,
            error = "Network connection lost",
            tempFilePath = "/tmp/progress_test.temp"
        )

        val paused = dao.getDownloadById(6001L)
        assertEquals("PAUSED", paused?.status)
        assertEquals("Network connection lost", paused?.errorMessage)
        assertEquals("/tmp/progress_test.temp", paused?.tempFilePath)
    }
}
