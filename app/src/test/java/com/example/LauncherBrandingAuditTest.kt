package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherBrandingAuditTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testAppNameMatchesStreamClean() {
        val appNameRes = context.getString(R.string.app_name)
        assertEquals("StreamClean", appNameRes)
    }

    @Test
    fun testManifestLauncherIconAndRoundIconConfigured() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(manifestFile)

        val appNodes = doc.getElementsByTagName("application")
        assertTrue("Application tag must exist", appNodes.length > 0)
        val appElement = appNodes.item(0) as Element

        val icon = appElement.getAttribute("android:icon")
        val roundIcon = appElement.getAttribute("android:roundIcon")
        val label = appElement.getAttribute("android:label")

        assertEquals("@mipmap/ic_launcher", icon)
        assertEquals("@mipmap/ic_launcher_round", roundIcon)
        assertEquals("@string/app_name", label)
    }

    @Test
    fun testAdaptiveIconStructureAndMonochromeLayer() {
        val launcherXml = File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        assertTrue("mipmap-anydpi-v26/ic_launcher.xml must exist", launcherXml.exists())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(launcherXml)

        val root = doc.documentElement
        assertEquals("adaptive-icon", root.tagName)

        val backgrounds = root.getElementsByTagName("background")
        assertTrue("Must declare <background>", backgrounds.length > 0)
        val bgElement = backgrounds.item(0) as Element
        assertEquals("@drawable/ic_launcher_background", bgElement.getAttribute("android:drawable"))

        val foregrounds = root.getElementsByTagName("foreground")
        assertTrue("Must declare <foreground>", foregrounds.length > 0)
        val fgElement = foregrounds.item(0) as Element
        assertEquals("@drawable/ic_launcher_foreground", fgElement.getAttribute("android:drawable"))

        val monochromes = root.getElementsByTagName("monochrome")
        assertTrue("Must declare <monochrome> for Material You themed icons", monochromes.length > 0)
        val monoElement = monochromes.item(0) as Element
        assertEquals("@drawable/ic_launcher_monochrome", monoElement.getAttribute("android:drawable"))
    }

    @Test
    fun testRoundAdaptiveIconStructureAndMonochromeLayer() {
        val roundXml = File("src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml")
        assertTrue("mipmap-anydpi-v26/ic_launcher_round.xml must exist", roundXml.exists())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(roundXml)

        val root = doc.documentElement
        assertEquals("adaptive-icon", root.tagName)

        val monochromes = root.getElementsByTagName("monochrome")
        assertTrue("Must declare <monochrome> for round icon", monochromes.length > 0)
        val monoElement = monochromes.item(0) as Element
        assertEquals("@drawable/ic_launcher_monochrome", monoElement.getAttribute("android:drawable"))
    }

    @Test
    fun testMonochromeDrawableExistsAndIsValidVector() {
        val monoFile = File("src/main/res/drawable/ic_launcher_monochrome.xml")
        assertTrue("ic_launcher_monochrome.xml must exist", monoFile.exists())

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(monoFile)
        assertEquals("vector", doc.documentElement.tagName)
    }

    @Test
    fun testDensityMipmapFallbackResourcesExist() {
        val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
        for (density in densities) {
            val dir = File("src/main/res/mipmap-$density")
            assertTrue("mipmap-$density directory must exist", dir.exists() && dir.isDirectory)
            val files = dir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(
                "mipmap-$density must contain ic_launcher (png or webp)",
                files.any { it.startsWith("ic_launcher.") }
            )
            assertTrue(
                "mipmap-$density must contain ic_launcher_round (png or webp)",
                files.any { it.startsWith("ic_launcher_round.") }
            )
        }
    }
}
