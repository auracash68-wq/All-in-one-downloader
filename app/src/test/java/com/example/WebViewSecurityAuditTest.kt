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
class WebViewSecurityAuditTest {

    @Test
    fun testNoJavaScriptBridgeIsExposed() {
        val file = File("src/main/java/com/example/ui/screens/ChromeBrowserScreen.kt")
        if (file.exists()) {
            val content = file.readText()
            assertFalse(
                "Must not expose JavaScript bridge via addJavascriptInterface",
                content.contains("addJavascriptInterface")
            )
        }
    }

    @Test
    fun testFileAndContentAccessAreRestricted() {
        val file = File("src/main/java/com/example/ui/screens/ChromeBrowserScreen.kt")
        if (file.exists()) {
            val content = file.readText()
            assertTrue(
                "allowFileAccess must be set to false",
                content.contains("settings.allowFileAccess = false")
            )
            assertTrue(
                "allowContentAccess must be set to false",
                content.contains("settings.allowContentAccess = false")
            )
            assertTrue(
                "allowFileAccessFromFileURLs must be set to false",
                content.contains("settings.allowFileAccessFromFileURLs = false")
            )
            assertTrue(
                "allowUniversalAccessFromFileURLs must be set to false",
                content.contains("settings.allowUniversalAccessFromFileURLs = false")
            )
        }
    }

    @Test
    fun testMixedContentNeverAllowed() {
        val file = File("src/main/java/com/example/ui/screens/ChromeBrowserScreen.kt")
        if (file.exists()) {
            val content = file.readText()
            assertTrue(
                "mixedContentMode must be set to MIXED_CONTENT_NEVER_ALLOW",
                content.contains("WebSettings.MIXED_CONTENT_NEVER_ALLOW")
            )
            assertFalse(
                "mixedContentMode must not use MIXED_CONTENT_ALWAYS_ALLOW or MIXED_CONTENT_COMPATIBILITY_MODE",
                content.contains("MIXED_CONTENT_ALWAYS_ALLOW") ||
                        content.contains("MIXED_CONTENT_COMPATIBILITY_MODE")
            )
        }
    }

    @Test
    fun testSslErrorsAreCancelledAndNeverProceed() {
        val file = File("src/main/java/com/example/ui/screens/ChromeBrowserScreen.kt")
        if (file.exists()) {
            val content = file.readText()
            assertTrue(
                "onReceivedSslError must cancel the SSL connection",
                content.contains("handler?.cancel()")
            )
            assertFalse(
                "Must never call handler?.proceed() on SSL error",
                content.contains("handler?.proceed()") || content.contains("handler.proceed()")
            )
        }
    }

    @Test
    fun testDangerousUrlSchemesAreBlocked() {
        val file = File("src/main/java/com/example/ui/screens/ChromeBrowserScreen.kt")
        if (file.exists()) {
            val content = file.readText()
            assertTrue(
                "Must block javascript, file, content, and data schemes",
                content.contains("scheme == \"javascript\" || scheme == \"file\" || scheme == \"content\" || scheme == \"data\"")
            )
        }
    }

    @Test
    fun testIntentSchemesHardened() {
        val file = File("src/main/java/com/example/ui/screens/ChromeBrowserScreen.kt")
        if (file.exists()) {
            val content = file.readText()
            assertTrue(
                "Intent must add CATEGORY_BROWSABLE",
                content.contains("intent.addCategory(Intent.CATEGORY_BROWSABLE)")
            )
            assertTrue(
                "Intent component must be cleared to prevent internal component launching",
                content.contains("intent.component = null")
            )
            assertTrue(
                "Intent selector must be cleared to prevent selector bypasses",
                content.contains("intent.selector = null")
            )
        }
    }
}
