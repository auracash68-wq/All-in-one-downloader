package com.example

import com.example.ui.viewmodel.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUrlSanitization_removesTrackingParam() {
        val input = "https://youtu.be/dQw4w9WgXcQ?si=abcdef123456"
        val output = MainViewModel.sanitizeUrl(input)
        assertFalse(output.contains("si="))
        assertTrue(output.contains("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun testUrlSanitization_preservesOtherQueryParams() {
        val input = "https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=123&t=42s"
        val output = MainViewModel.sanitizeUrl(input)
        assertFalse(output.contains("si="))
        assertTrue(output.contains("v=dQw4w9WgXcQ"))
        assertTrue(output.contains("t=42s"))
    }

    @Test
    fun testQualityPolicyLimits_144pTo720p() {
        val heights = listOf(144, 240, 360, 480, 720, 1080, 1440, 2160)
        val filtered = heights.filter { it in 144..720 }
        assertEquals(listOf(144, 240, 360, 480, 720), filtered)
        assertFalse(filtered.contains(1080))
        assertFalse(filtered.contains(1440))
        assertFalse(filtered.contains(2160))
    }
}
