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
class ProGuardR8ConfigurationTest {

    @Test
    fun testProGuardRulesFileExistsAndHasNoBlanketRules() {
        val rulesFile = File("proguard-rules.pro")
        assertTrue("proguard-rules.pro must exist in app module", rulesFile.exists())
        val content = rulesFile.readText()

        // Blanket rules must NOT exist
        assertFalse("Must not use blanket keep class ** { *; } rule", content.contains("-keep class ** { *; }"))
        assertFalse("Must not use blanket -keep class * { *; } rule", content.contains("-keep class * { *; }"))
    }

    @Test
    fun testProGuardPreservesRequiredComponents() {
        val rulesFile = File("proguard-rules.pro")
        assertTrue(rulesFile.exists())
        val content = rulesFile.readText()

        // 1. JNI / Native methods
        assertTrue("Must preserve JNI native methods", content.contains("native <methods>;"))

        // 2. youtubedl-android & FFmpeg
        assertTrue("Must preserve YoutubeDL API", content.contains("com.yausername.youtubedl_android.YoutubeDL"))
        assertTrue("Must preserve YoutubeDL mappers", content.contains("com.yausername.youtubedl_android.mapper.**"))

        // 3. Room database
        assertTrue("Must preserve Room DownloadEntity", content.contains("DownloadEntity"))
        assertTrue("Must preserve Room DownloadDao", content.contains("DownloadDao"))
        assertTrue("Must preserve RoomDatabase subclasses", content.contains("androidx.room.RoomDatabase"))

        // 4. Media3 / ExoPlayer
        assertTrue("Must preserve Media3 / ExoPlayer constructors", content.contains("androidx.media3.exoplayer.**"))

        // 5. App Services
        assertTrue("Must preserve DownloadService", content.contains("DownloadService"))
        assertTrue("Must preserve FileProvider", content.contains("FileProvider"))
    }

    @Test
    fun testBuildGradleRetainsBothDebugAndRelease() {
        val buildGradle = File("build.gradle.kts")
        assertTrue(buildGradle.exists())
        val content = buildGradle.readText()

        assertTrue("Must retain debug build type", content.contains("debug {"))
        assertTrue("Must retain release build type", content.contains("release {"))
        assertTrue("Must enable R8 minification on release", content.contains("isMinifyEnabled = true"))
    }
}
