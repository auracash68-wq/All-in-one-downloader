package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppIdentityAndVersioningAuditTest {

    @Test
    fun testAppIdentityAndVersioningConfig() {
        val buildFile = File("build.gradle.kts")
        val appBuildFile = File("app/build.gradle.kts")
        val activeBuild = if (appBuildFile.exists()) appBuildFile else buildFile
        assertTrue("build.gradle.kts must exist", activeBuild.exists())

        val content = activeBuild.readText()

        // Verify namespace
        assertTrue("Namespace must be com.example", content.contains("namespace = \"com.example\""))

        // Verify applicationId
        assertTrue("Application ID must be com.aistudio.streamclean.dlapp", content.contains("applicationId = \"com.aistudio.streamclean.dlapp\""))

        // Verify positive integer versionCode
        val versionCodeRegex = Regex("versionCode\\s*=\\s*(\\d+)")
        val matchCode = versionCodeRegex.find(content)
        assertTrue("versionCode must be declared", matchCode != null)
        val codeValue = matchCode!!.groupValues[1].toInt()
        assertTrue("versionCode must be a positive integer >= 1", codeValue >= 1)

        // Verify semantic versionName
        val versionNameRegex = Regex("versionName\\s*=\\s*\"([^\"]+)\"")
        val matchName = versionNameRegex.find(content)
        assertTrue("versionName must be declared", matchName != null)
        val nameValue = matchName!!.groupValues[1]
        assertTrue("versionName must be non-empty semantic version", nameValue.isNotEmpty() && nameValue.contains("."))
    }
}
