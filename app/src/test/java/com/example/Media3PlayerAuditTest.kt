package com.example

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DownloadEntity
import com.example.util.FileSecurityUtil
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
class Media3PlayerAuditTest {

    private lateinit var context: Context
    private var player: ExoPlayer? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        player?.stop()
        player?.clearMediaItems()
        player?.release()
        player = null
    }

    @Test
    fun testExoPlayerLifecycle_createAndRelease() {
        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer

        assertNotNull(exoPlayer)
        assertEquals(Player.REPEAT_MODE_OFF, exoPlayer.repeatMode)

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.release()
        player = null
    }

    @Test
    fun testExoPlayer_loadApprovedLocalFile() {
        val downloadDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
        val validMedia = File(downloadDir, "valid_player_test.mp4")
        validMedia.writeText("sample video test payload")

        assertTrue(
            "File must be inside approved directory",
            FileSecurityUtil.isPathInApprovedDirectory(context, validMedia, allowPrivate = true)
        )

        val exoPlayer = ExoPlayer.Builder(context).build()
        player = exoPlayer

        val mediaItem = MediaItem.fromUri(Uri.fromFile(validMedia))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        assertEquals(1, exoPlayer.mediaItemCount)
    }

    @Test
    fun testExoPlayer_securityCheckRejectsUnauthorizedFilePath() {
        val unauthorizedFile = File(context.filesDir, "unauthorized_player.mp4")
        unauthorizedFile.writeText("unauthorized payload")

        assertFalse(
            "Unauthorized file path must be rejected by security check",
            FileSecurityUtil.isPathInApprovedDirectory(context, unauthorizedFile, allowPrivate = false)
        )
    }

    @Test
    fun testVideoPlayerScreenFileAudit() {
        val screenFile = File("src/main/java/com/example/ui/screens/VideoPlayerScreen.kt")
        assertTrue("VideoPlayerScreen.kt must exist", screenFile.exists())
        val content = screenFile.readText()

        // Verify lifecycle release
        assertTrue("Must call exoPlayer.release()", content.contains("exoPlayer.release()"))
        assertTrue("Must call exoPlayer.stop()", content.contains("exoPlayer.stop()"))

        // Verify screen keep awake
        assertTrue("Must handle FLAG_KEEP_SCREEN_ON", content.contains("FLAG_KEEP_SCREEN_ON"))

        // Verify orientation handling
        assertTrue("Must handle SCREEN_ORIENTATION_LANDSCAPE", content.contains("SCREEN_ORIENTATION_LANDSCAPE"))
        assertTrue("Must handle SCREEN_ORIENTATION_UNSPECIFIED", content.contains("SCREEN_ORIENTATION_UNSPECIFIED"))

        // Verify back handling
        assertTrue("Must implement BackHandler", content.contains("BackHandler"))

        // Verify error handling
        assertTrue("Must implement error listener", content.contains("onPlayerError"))
    }
}
