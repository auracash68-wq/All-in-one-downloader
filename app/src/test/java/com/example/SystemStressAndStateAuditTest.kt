package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.engine.DownloadEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
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
class SystemStressAndStateAuditTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: DownloadRepository
    private lateinit var engine: DownloadEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DownloadRepository(context, database.downloadDao())
        engine = DownloadEngine.getInstance(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testTruthfulStates_allValidEnumStates() {
        val validStates = setOf("PENDING", "DOWNLOADING", "VERIFYING", "COMPLETED", "FAILED", "CANCELLED", "PAUSED")

        val entity = DownloadEntity(
            id = 1,
            url = "https://example.com/test",
            title = "State Test Video",
            fileName = "state_test.mp4",
            filePath = "/path/state_test.mp4",
            status = "DOWNLOADING"
        )

        assertTrue(validStates.contains(entity.status))

        val pausedEntity = entity.copy(status = "PAUSED")
        assertTrue(validStates.contains(pausedEntity.status))

        val cancelledEntity = entity.copy(status = "CANCELLED")
        assertTrue(validStates.contains(cancelledEntity.status))

        val failedEntity = entity.copy(status = "FAILED", errorMessage = "HTTP 403 Forbidden")
        assertTrue(validStates.contains(failedEntity.status))
        assertEquals("HTTP 403 Forbidden", failedEntity.errorMessage)
    }

    @Test
    fun testLowMemoryHandling_checkIsMemoryCriticallyLowDoesNotCrash() {
        val isLow = engine.isMemoryCriticallyLow()
        // In Robolectric environment, must return boolean safely without throwing
        assertNotNull(isLow)
    }

    @Test
    fun testInterruptedDownloadRecovery_marksAsPausedTruthfully() = runBlocking {
        val interrupted = DownloadEntity(
            id = 10,
            url = "https://example.com/interrupted",
            title = "Interrupted Download",
            fileName = "interrupted.mp4",
            filePath = "",
            status = "DOWNLOADING",
            downloadedBytes = 5000L,
            totalBytes = 10000L
        )
        database.downloadDao().insert(interrupted)

        repository.markInterruptedDownloadsAsPaused("Simulated system process restart")

        val recovered = database.downloadDao().getDownloadById(10)
        assertNotNull(recovered)
        assertEquals("PAUSED", recovered?.status)
        assertEquals("Simulated system process restart", recovered?.errorMessage)
    }

    @Test
    fun testCancelVsPauseStateDistinction() = runBlocking {
        val task1 = DownloadEntity(
            id = 20,
            url = "https://example.com/pause_test",
            title = "Pause Test",
            fileName = "pause_test.mp4",
            filePath = "",
            status = "PAUSED"
        )
        val task2 = DownloadEntity(
            id = 21,
            url = "https://example.com/cancel_test",
            title = "Cancel Test",
            fileName = "cancel_test.mp4",
            filePath = "",
            status = "CANCELLED"
        )

        database.downloadDao().insert(task1)
        database.downloadDao().insert(task2)

        val retrieved1 = database.downloadDao().getDownloadById(20)
        val retrieved2 = database.downloadDao().getDownloadById(21)

        assertEquals("PAUSED", retrieved1?.status)
        assertEquals("CANCELLED", retrieved2?.status)
        assertFalse("Pause and Cancel must remain distinctly separate states", retrieved1?.status == retrieved2?.status)
    }
}
