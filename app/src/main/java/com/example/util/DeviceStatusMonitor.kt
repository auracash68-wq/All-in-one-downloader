package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import androidx.compose.ui.graphics.Color

enum class StatusLevel(val color: Color) {
    GREEN(Color(0xFF22C55E)),
    YELLOW(Color(0xFFEAB308)),
    RED(Color(0xFFEF4444))
}

data class DeviceRamState(
    val availableBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isLowMemory: Boolean = false,
    val availablePercent: Int = 0,
    val displayText: String = "Calculating...",
    val detailedText: String = "",
    val statusLevel: StatusLevel = StatusLevel.GREEN
)

data class NetworkSpeedState(
    val speedBytesPerSec: Long = 0L,
    val displayText: String = "0 KB/s",
    val statusLevel: StatusLevel = StatusLevel.GREEN
)

class DeviceStatusMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private var lastTotalRxBytes: Long = TrafficStats.getTotalRxBytes()
    private var lastTotalTxBytes: Long = TrafficStats.getTotalTxBytes()
    private var lastTimestamp: Long = System.currentTimeMillis()

    fun getMemoryState(): DeviceRamState {
        val memoryInfo = ActivityManager.MemoryInfo()
        val am = activityManager
        if (am == null) {
            return DeviceRamState(displayText = "Available", statusLevel = StatusLevel.GREEN)
        }
        am.getMemoryInfo(memoryInfo)

        val availBytes = memoryInfo.availMem
        val totalBytes = memoryInfo.totalMem
        val isLowMem = memoryInfo.lowMemory
        val availPercent = if (totalBytes > 0) ((availBytes.toDouble() / totalBytes) * 100).toInt() else 0

        val availGb = availBytes.toDouble() / (1024 * 1024 * 1024)
        val totalGb = totalBytes.toDouble() / (1024 * 1024 * 1024)

        // Thresholds for RAM health status
        val statusLevel = when {
            isLowMem || availBytes < THRESHOLD_CRITICAL_RAM_BYTES || availPercent < 10 -> StatusLevel.RED
            availBytes < THRESHOLD_WARN_RAM_BYTES || availPercent < 18 -> StatusLevel.YELLOW
            else -> StatusLevel.GREEN
        }

        val display = if (availGb >= 1.0) {
            String.format("%.1f GB free", availGb)
        } else {
            val availMb = availBytes / (1024 * 1024)
            "$availMb MB free"
        }

        val detailed = String.format("%.1f / %.1f GB (%d%% free)", availGb, totalGb, availPercent)

        return DeviceRamState(
            availableBytes = availBytes,
            totalBytes = totalBytes,
            isLowMemory = isLowMem,
            availablePercent = availPercent,
            displayText = display,
            detailedText = detailed,
            statusLevel = statusLevel
        )
    }

    fun sampleInternetSpeed(): NetworkSpeedState {
        val now = System.currentTimeMillis()
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()

        if (currentRx == TrafficStats.UNSUPPORTED.toLong() || lastTotalRxBytes == TrafficStats.UNSUPPORTED.toLong()) {
            return NetworkSpeedState(displayText = "Active", statusLevel = StatusLevel.GREEN)
        }

        val timeDeltaMs = now - lastTimestamp
        if (timeDeltaMs <= 300) {
            return NetworkSpeedState()
        }

        val rxDelta = (currentRx - lastTotalRxBytes).coerceAtLeast(0L)
        val txDelta = (currentTx - lastTotalTxBytes).coerceAtLeast(0L)
        val totalBytesDelta = rxDelta + txDelta

        val speedBytesPerSec = (totalBytesDelta * 1000L) / timeDeltaMs

        lastTotalRxBytes = currentRx
        lastTotalTxBytes = currentTx
        lastTimestamp = now

        val kbps = speedBytesPerSec.toDouble() / 1024.0
        val mbps = speedBytesPerSec.toDouble() / (1024.0 * 1024.0)

        val displayText = when {
            mbps >= 1.0 -> String.format("%.1f MB/s", mbps)
            kbps >= 1.0 -> String.format("%.0f KB/s", kbps)
            else -> "0 KB/s"
        }

        val statusLevel = when {
            speedBytesPerSec >= SPEED_THRESHOLD_HIGH_BPS -> StatusLevel.GREEN
            speedBytesPerSec >= SPEED_THRESHOLD_LOW_BPS -> StatusLevel.YELLOW
            else -> StatusLevel.RED
        }

        return NetworkSpeedState(
            speedBytesPerSec = speedBytesPerSec,
            displayText = displayText,
            statusLevel = statusLevel
        )
    }

    companion object {
        // Tunable thresholds for RAM and Network
        const val THRESHOLD_CRITICAL_RAM_BYTES = 300L * 1024 * 1024 // 300 MB
        const val THRESHOLD_WARN_RAM_BYTES = 600L * 1024 * 1024     // 600 MB

        const val SPEED_THRESHOLD_HIGH_BPS = 1_500_000L // 1.5 MB/s
        const val SPEED_THRESHOLD_LOW_BPS = 100_000L    // 100 KB/s
    }
}
