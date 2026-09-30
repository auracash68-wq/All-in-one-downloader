package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String = "",
    val title: String,
    val fileName: String,
    val filePath: String = "",
    val fileSizeBytes: Long = 0,
    val formattedSize: String = "",
    val duration: String = "",
    val thumbnailUri: String? = null,
    val mediaType: String = "VIDEO", // "VIDEO" or "AUDIO"
    val resolution: String = "720p",
    val status: String = "COMPLETED", // "PENDING", "DOWNLOADING", "VERIFYING", "COMPLETED", "FAILED", "CANCELLED", "PAUSED"
    val progress: Int = 100,
    val relativeDate: String = "Today",
    val timestamp: Long = System.currentTimeMillis(),
    val isPrivate: Boolean = false,
    val formatId: String = "",
    val audioBitrate: String = "192kbps",
    val errorMessage: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val isResumable: Boolean = false,
    val tempFilePath: String = ""
)
