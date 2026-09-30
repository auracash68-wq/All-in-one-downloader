package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseSigningSecurityAuditTest {

    private fun findRootFile(fileName: String): File {
        val direct = File(fileName)
        if (direct.exists()) return direct
        val parent = File("..", fileName)
        if (parent.exists()) return parent
        return direct
    }

    @Test
    fun testGitIgnorePreventsKeystoresAndKeyProperties() {
        val gitignoreFile = findRootFile(".gitignore")
        assertTrue(".gitignore must exist in root", gitignoreFile.exists())

        val content = gitignoreFile.readText()
        assertTrue("Must ignore *.jks", content.contains("*.jks"))
        assertTrue("Must ignore *.keystore", content.contains("*.keystore"))
        assertTrue("Must ignore key.properties", content.contains("key.properties"))
        assertTrue("Must ignore keystore.properties", content.contains("keystore.properties"))
        assertTrue("Must ignore local.properties", content.contains("local.properties"))
        assertTrue("Must ignore .env", content.contains(".env"))
    }

    @Test
    fun testNoCommittedProductionKeystoreOrPasswords() {
        // Verify no real production key.properties or secret files are committed
        val keyProps = findRootFile("key.properties")
        assertFalse("Real key.properties must NOT be committed in workspace", keyProps.exists())

        val uploadJks = findRootFile("my-upload-key.jks")
        assertFalse("Real production my-upload-key.jks must NOT be committed in workspace", uploadJks.exists())
    }

    @Test
    fun testBuildGradleSigningConfigIsSecureAndFlexible() {
        val buildFile = findRootFile("build.gradle.kts")
        val appBuildFile = findRootFile("app/build.gradle.kts")
        val activeBuild = if (appBuildFile.exists()) appBuildFile else buildFile
        assertTrue("build.gradle.kts must exist", activeBuild.exists())

        val content = activeBuild.readText()

        // Verify release signing checks key.properties and env variables
        assertTrue("Signing config must support key.properties", content.contains("key.properties"))
        assertTrue("Signing config must support environment variables", content.contains("System.getenv(\"STORE_PASSWORD\")") || content.contains("System.getenv(\"RELEASE_STORE_PASSWORD\")"))

        // Verify debug signing config is configured
        assertTrue("Debug signing config must be present", content.contains("getByName(\"debug\")"))
    }

    @Test
    fun testGitHubActionsWorkflowsRemainEnabled() {
        val workflowFile = findRootFile(".github/workflows/build-debug-apk.yml")
        assertTrue("build-debug-apk.yml workflow must exist", workflowFile.exists())

        val content = workflowFile.readText()
        assertTrue("Workflow must build Debug APK", content.contains("assembleDebug"))
        assertTrue("Workflow must upload APK artifact", content.contains("upload-artifact"))
    }
}
