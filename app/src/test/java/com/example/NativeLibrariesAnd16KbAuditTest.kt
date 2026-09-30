package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeLibrariesAnd16KbAuditTest {

    data class ElfInfo(
        val isElf: Boolean,
        val is64Bit: Boolean,
        val isLittleEndian: Boolean,
        val minPtLoadAlignment: Long,
        val ptLoadSegmentsCount: Int
    )

    private fun parseElfHeaderAndAlignment(file: File): ElfInfo {
        if (!file.exists() || file.length() < 64) {
            return ElfInfo(isElf = false, is64Bit = false, isLittleEndian = true, minPtLoadAlignment = 0, ptLoadSegmentsCount = 0)
        }

        RandomAccessFile(file, "r").use { raf ->
            val magic = ByteArray(4)
            raf.readFully(magic)
            if (magic[0] != 0x7F.toByte() || magic[1] != 'E'.code.toByte() ||
                magic[2] != 'L'.code.toByte() || magic[3] != 'F'.code.toByte()) {
                return ElfInfo(isElf = false, is64Bit = false, isLittleEndian = true, minPtLoadAlignment = 0, ptLoadSegmentsCount = 0)
            }

            val eiClass = raf.readByte().toInt() // 1 = 32-bit, 2 = 64-bit
            val is64Bit = eiClass == 2
            val eiData = raf.readByte().toInt() // 1 = little-endian, 2 = big-endian
            val isLittleEndian = eiData == 1
            val byteOrder = if (isLittleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

            var minAlignment = Long.MAX_VALUE
            var ptLoadCount = 0

            if (is64Bit) {
                // 64-bit ELF header
                val headerBuf = ByteArray(64 - 6)
                raf.readFully(headerBuf)
                val buf = ByteBuffer.wrap(headerBuf).order(byteOrder)

                // e_phoff is at offset 32 (relative to start), so offset 26 in headerBuf
                buf.position(26)
                val ePhoOff = buf.long

                // e_phentsize is at offset 54 (rel offset 48)
                buf.position(48)
                val ePhentSize = buf.short.toInt()

                // e_phnum is at offset 56 (rel offset 50)
                val ePhNum = buf.short.toInt()

                raf.seek(ePhoOff)
                val phBuf = ByteArray(ePhentSize)
                for (i in 0 until ePhNum) {
                    raf.readFully(phBuf)
                    val pBuf = ByteBuffer.wrap(phBuf).order(byteOrder)
                    val pType = pBuf.int
                    if (pType == 1) { // PT_LOAD
                        ptLoadCount++
                        // In 64-bit, p_align is at offset 48 (0x30)
                        pBuf.position(48)
                        val pAlign = pBuf.long
                        if (pAlign in 1 until minAlignment) {
                            minAlignment = pAlign
                        }
                    }
                }
            } else {
                // 32-bit ELF header
                val headerBuf = ByteArray(52 - 6)
                raf.readFully(headerBuf)
                val buf = ByteBuffer.wrap(headerBuf).order(byteOrder)

                // e_phoff is at offset 28 (rel offset 22)
                buf.position(22)
                val ePhoOff = buf.int.toLong() and 0xFFFFFFFFL

                // e_phentsize is at offset 42 (rel offset 36)
                buf.position(36)
                val ePhentSize = buf.short.toInt()

                // e_phnum is at offset 44 (rel offset 38)
                val ePhNum = buf.short.toInt()

                raf.seek(ePhoOff)
                val phBuf = ByteArray(ePhentSize)
                for (i in 0 until ePhNum) {
                    raf.readFully(phBuf)
                    val pBuf = ByteBuffer.wrap(phBuf).order(byteOrder)
                    val pType = pBuf.int
                    if (pType == 1) { // PT_LOAD
                        ptLoadCount++
                        // In 32-bit, p_align is at offset 28 (0x1C)
                        pBuf.position(28)
                        val pAlign = pBuf.int.toLong() and 0xFFFFFFFFL
                        if (pAlign in 1 until minAlignment) {
                            minAlignment = pAlign
                        }
                    }
                }
            }

            return ElfInfo(
                isElf = true,
                is64Bit = is64Bit,
                isLittleEndian = isLittleEndian,
                minPtLoadAlignment = if (minAlignment == Long.MAX_VALUE) 0 else minAlignment,
                ptLoadSegmentsCount = ptLoadCount
            )
        }
    }

    @Test
    fun testAll64BitNativeLibrariesAre16KbPageAligned() {
        val mergedLibsDirs = listOf(
            File("build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib"),
            File("app/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib")
        )

        val activeDir = mergedLibsDirs.firstOrNull { it.exists() }
        assertNotNull("Merged native libraries directory must exist after build", activeDir)

        val soFiles = activeDir!!.walkTopDown().filter { it.isFile && it.extension == "so" }.toList()
        assertTrue("Native .so files must be present in build outputs", soFiles.isNotEmpty())

        val inspected64BitLibs = mutableListOf<String>()

        for (file in soFiles) {
            val elfInfo = parseElfHeaderAndAlignment(file)
            if (elfInfo.isElf && elfInfo.is64Bit) {
                val relPath = file.relativeTo(activeDir).path
                inspected64BitLibs.add(relPath)
                assertTrue(
                    "64-bit library $relPath has PT_LOAD alignment of ${elfInfo.minPtLoadAlignment} bytes, which is LESS than required 16384 bytes (16 KB) for Android 15/16!",
                    elfInfo.minPtLoadAlignment >= 16384L
                )
            }
        }

        assertTrue("Must have verified at least one 64-bit native library (arm64-v8a / x86_64)", inspected64BitLibs.isNotEmpty())
    }

    @Test
    fun testArm64FfmpegAndPythonNativeBinariesCompatibility() {
        val mergedLibsDirs = listOf(
            File("build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/arm64-v8a"),
            File("app/build/intermediates/merged_native_libs/debug/mergeDebugNativeLibs/out/lib/arm64-v8a")
        )

        val arm64Dir = mergedLibsDirs.firstOrNull { it.exists() }
        if (arm64Dir != null) {
            val ffmpegSo = File(arm64Dir, "libffmpeg.so")
            if (ffmpegSo.exists()) {
                val elf = parseElfHeaderAndAlignment(ffmpegSo)
                assertTrue("libffmpeg.so must be valid 64-bit ELF", elf.isElf && elf.is64Bit)
                assertEquals("libffmpeg.so must be 16KB aligned (16384 bytes)", 16384L, elf.minPtLoadAlignment)
            }

            val pythonSo = File(arm64Dir, "libpython.so")
            if (pythonSo.exists()) {
                val elf = parseElfHeaderAndAlignment(pythonSo)
                assertTrue("libpython.so must be valid 64-bit ELF", elf.isElf && elf.is64Bit)
                assertEquals("libpython.so must be 16KB aligned (16384 bytes)", 16384L, elf.minPtLoadAlignment)
            }

            val qjsSo = File(arm64Dir, "libqjs.so")
            if (qjsSo.exists()) {
                val elf = parseElfHeaderAndAlignment(qjsSo)
                assertTrue("libqjs.so must be valid 64-bit ELF", elf.isElf && elf.is64Bit)
                assertEquals("libqjs.so must be 16KB aligned (16384 bytes)", 16384L, elf.minPtLoadAlignment)
            }
        }
    }

    @Test
    fun testBuildGradleConfiguresStandardAbiFilters() {
        val buildGradleFile = File("build.gradle.kts")
        val appBuildGradleFile = File("app/build.gradle.kts")
        val file = if (appBuildGradleFile.exists()) appBuildGradleFile else buildGradleFile
        assertTrue("build.gradle.kts must exist", file.exists())

        val content = file.readText()
        assertTrue("Must specify arm64-v8a ABI filter", content.contains("arm64-v8a"))
        assertTrue("Must specify armeabi-v7a ABI filter", content.contains("armeabi-v7a"))
        assertTrue("Must specify x86 ABI filter", content.contains("x86"))
        assertTrue("Must specify x86_64 ABI filter", content.contains("x86_64"))
    }
}
