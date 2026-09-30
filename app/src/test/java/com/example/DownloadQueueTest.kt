package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.example.engine.ActiveDownloadState
import com.example.engine.DownloadEngine
import kotlinx.coroutines.runBlocking
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
class DownloadQueueTest {

    private lateinit var context: Context
    private lateinit var engine: DownloadEngine
    private lateinit var repository: DownloadRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        engine = DownloadEngine.getInstance(context)
        engine.resetForTesting()
        val database = AppDatabase.getInstance(context)
        repository = DownloadRepository(context, database.downloadDao())
    }

    @Test
    fun testQueueCorrectnessAndActiveStateOwnership() {
        // Enqueue Task A, Task B, Task C
        val idA = 1001L
        val idB = 1002L
        val idC = 1003L

        engine.enqueueDownload(
            entityId = idA,
            url = "https://example.com/videoA.mp4",
            title = "Task A",
            mediaType = "VIDEO",
            resolution = "720p",
            queueIndex = 1,
            queueTotal = 3,
            repository = repository
        )

        // Verify Task A initially set
        val activeAfterA = engine.activeDownload.value
        assertNotNull(activeAfterA)
        assertEquals(idA, activeAfterA?.id)

        // Enqueue Task B and Task C
        engine.enqueueDownload(
            entityId = idB,
            url = "https://example.com/videoB.mp4",
            title = "Task B",
            mediaType = "VIDEO",
            resolution = "720p",
            queueIndex = 2,
            queueTotal = 3,
            repository = repository
        )

        engine.enqueueDownload(
            entityId = idC,
            url = "https://example.com/videoC.mp4",
            title = "Task C",
            mediaType = "VIDEO",
            resolution = "720p",
            queueIndex = 3,
            queueTotal = 3,
            repository = repository
        )

        // CRITICAL QUEUE TEST: Task B and Task C MUST NOT overwrite activeDownload state of Task A!
        val activeAfterAll = engine.activeDownload.value
        assertNotNull(activeAfterAll)
        assertEquals("Active download must still be owned by Task A", idA, activeAfterAll?.id)

        // Verify all cards are present in downloadCards
        val cards = engine.downloadCards.value
        assertTrue("Download cards must contain Task A", cards.any { it.id == idA })
        assertTrue("Download cards must contain Task B", cards.any { it.id == idB })
        assertTrue("Download cards must contain Task C", cards.any { it.id == idC })

        // Verify Task B and Task C were initialized in QUEUED state (not running)
        val cardB = cards.first { it.id == idB }
        val cardC = cards.first { it.id == idC }
        assertFalse("Queued Task B must not be running while Task A is active", cardB.isRunning)
        assertFalse("Queued Task C must not be running while Task A is active", cardC.isRunning)
        assertEquals("Queued...", cardB.speed)
        assertEquals("Queued...", cardC.speed)
    }

    @Test
    fun testCancellationOfQueuedTask() {
        val idA = 2001L
        val idB = 2002L

        engine.enqueueDownload(
            entityId = idA,
            url = "https://example.com/video1.mp4",
            title = "Task 1",
            mediaType = "VIDEO",
            resolution = "720p",
            queueIndex = 1,
            queueTotal = 2,
            repository = repository
        )

        engine.enqueueDownload(
            entityId = idB,
            url = "https://example.com/video2.mp4",
            title = "Task 2",
            mediaType = "VIDEO",
            resolution = "720p",
            queueIndex = 2,
            queueTotal = 2,
            repository = repository
        )

        // Cancel queued Task B
        engine.cancelDownload(idB)

        // Verify Task B card is cancelled and never becomes active
        val cardB = engine.downloadCards.value.firstOrNull { it.id == idB }
        assertNotNull(cardB)
        assertTrue("Cancelled task card must be marked isCancelled", cardB!!.isCancelled)
        assertFalse("Cancelled task must not be running", cardB.isRunning)
        assertEquals("Cancelled", cardB.speed)

        val active = engine.activeDownload.value
        // Active download must either be null or still Task 1, but NEVER cancelled Task 2
        assertTrue("Active download must not be cancelled Task 2", active == null || active.id != idB)
    }

    @Test
    fun testDuplicateDetection() {
        val url = "https://example.com/stream_duplicate_test.mp4"

        assertFalse("URL must not be flagged before enqueuing", engine.isAlreadyDownloadingOrQueued(url, "VIDEO"))

        engine.enqueueDownload(
            entityId = 3001L,
            url = url,
            title = "Duplicate Test",
            mediaType = "VIDEO",
            resolution = "720p",
            repository = repository
        )

        assertTrue("URL must be detected as already downloading or queued", engine.isAlreadyDownloadingOrQueued(url, "VIDEO"))
    }

    @Test
    fun testRoomDeterministicLifecycleStatus() = runBlocking {
        val testEntity = DownloadEntity(
            id = 4001L,
            url = "https://example.com/lifecycle.mp4",
            title = "Lifecycle Test",
            fileName = "",
            filePath = "",
            status = "DOWNLOADING",
            progress = 0
        )

        repository.insertOrUpdateDownload(testEntity)
        var fromDb = repository.getDownloadById(4001L)
        assertEquals("DOWNLOADING", fromDb?.status)

        repository.updateDownloadStatus(4001L, "VERIFYING", 100)
        fromDb = repository.getDownloadById(4001L)
        assertEquals("VERIFYING", fromDb?.status)
        assertEquals(100, fromDb?.progress)

        repository.updateDownloadStatus(4001L, "CANCELLED", 0)
        fromDb = repository.getDownloadById(4001L)
        assertEquals("CANCELLED", fromDb?.status)

        repository.updateDownloadStatus(4001L, "FAILED", 0)
        fromDb = repository.getDownloadById(4001L)
        assertEquals("FAILED", fromDb?.status)
    }
}
