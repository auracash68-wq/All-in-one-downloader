package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkSecurityAuditTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNetworkSecurityConfigFileExistsAndIsSecure() {
        val configResId = context.resources.getIdentifier("network_security_config", "xml", context.packageName)
        assertTrue("network_security_config XML resource must exist", configResId != 0)

        val parser = context.resources.getXml(configResId)
        var hasBaseConfig = false
        var eventType = parser.eventType
        while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                if (parser.name == "base-config") {
                    hasBaseConfig = true
                    val cleartext = parser.getAttributeValue(null, "cleartextTrafficPermitted")
                    assertEquals("false", cleartext)
                }
            }
            eventType = parser.next()
        }
        assertTrue("Base configuration must be present in network security config", hasBaseConfig)
    }

    @Test
    fun testAndroidManifestDoesNotEnableBlanketUsesCleartextTraffic() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            val content = manifestFile.readText()
            assertFalse(
                "AndroidManifest must not declare blanket android:usesCleartextTraffic='true'",
                content.contains("android:usesCleartextTraffic=\"true\"")
            )
            assertTrue(
                "AndroidManifest must declare android:networkSecurityConfig",
                content.contains("android:networkSecurityConfig=\"@xml/network_security_config\"")
            )
        }
    }

    @Test
    fun testDownloadEngineDoesNotBypassCertificates() {
        val engineFile = File("src/main/java/com/example/engine/DownloadEngine.kt")
        if (engineFile.exists()) {
            val content = engineFile.readText()
            assertFalse(
                "DownloadEngine must not contain --no-check-certificates flag",
                content.contains("--no-check-certificates")
            )
        }
    }

    private fun assertEquals(expected: String, actual: String?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
