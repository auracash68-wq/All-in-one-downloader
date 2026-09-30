package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import androidx.compose.ui.graphics.Color

enum class StatusLevel(val color: Color) {
    GREEN(Color(0xFF22C55E)),
    YELLOW(Color(0xFFEAB308)),
    RED(Color(0xFFEF4444))
}

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

    /**
     * On-demand check for critical system memory pressure during heavy download operations.
     */
    fun isDeviceLowMemory(): Boolean {
        val am = activityManager ?: return false
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory || memoryInfo.availMem < THRESHOLD_CRITICAL_RAM_BYTES
    }

    /**
     * Checks if the device has an active, working network connection.
     */
    fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        const val THRESHOLD_CRITICAL_RAM_BYTES = 300L * 1024 * 1024 // 300 MB
        const val SPEED_THRESHOLD_HIGH_BPS = 1_500_000L // 1.5 MB/s
        const val SPEED_THRESHOLD_LOW_BPS = 100_000L    // 100 KB/s
    }
}
