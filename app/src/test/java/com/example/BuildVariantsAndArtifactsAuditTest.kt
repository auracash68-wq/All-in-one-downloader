package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BuildVariantsAndArtifactsAuditTest {

    private fun findFile(path: String): File {
        val direct = File(path)
        if (direct.exists()) return direct
        val parent = File("..", path)
        if (parent.exists()) return parent
        return direct
    }

    @Test
    fun testBuildGradleDeclaresBothDebugAndReleaseBuildTypes() {
        val buildFile = findFile("build.gradle.kts")
        val appBuildFile = findFile("app/build.gradle.kts")
        val activeBuild = if (appBuildFile.exists()) appBuildFile else buildFile
        assertTrue("build.gradle.kts must exist", activeBuild.exists())

        val content = activeBuild.readText()
        assertTrue("Must declare release build type", content.contains("release {"))
        assertTrue("Must declare debug build type", content.contains("debug {"))
        assertTrue("Release must configure minify/R8", content.contains("isMinifyEnabled = true"))
        assertTrue("Release must configure proguard-rules.pro", content.contains("proguard-rules.pro"))
    }

    @Test
    fun testGitHubActionsDebugWorkflowIntact() {
        val workflowFile = findFile(".github/workflows/build-debug-apk.yml")
        assertTrue("build-debug-apk.yml must exist", workflowFile.exists())

        val content = workflowFile.readText()
        assertTrue("Workflow must run assembleDebug", content.contains("assembleDebug"))
        assertTrue("Workflow must preserve upload-artifact", content.contains("upload-artifact"))
    }

    @Test
    fun testBuildOutputArtifactsCanBeGenerated() {
        val buildDir = findFile("app/build/outputs")
        if (buildDir.exists()) {
            val apkDir = File(buildDir, "apk")
            assertTrue("outputs/apk directory must exist when built", apkDir.exists())
        }
    }
}
