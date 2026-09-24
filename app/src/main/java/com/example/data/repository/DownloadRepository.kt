package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.engine.DownloadEngine
import com.example.engine.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val videoDownloads: Flow<List<DownloadEntity>> = downloadDao.getDownloadsByType("VIDEO")
    val audioDownloads: Flow<List<DownloadEntity>> = downloadDao.getDownloadsByType("AUDIO")
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()

    suspend fun fetchVideoInfo(url: String): VideoMetadata = withContext(Dispatchers.IO) {
        val sanitized = url.substringBefore("?si=").substringBefore("&si=").trim()
        Log.d("DownloadRepository", "fetchVideoInfo requested for sanitized URL: $sanitized (original: $url)")
        try {
            val engine = DownloadEngine.getInstance(context)
            engine.fetchFormats(sanitized)
        } catch (e: Throwable) {
            Log.e("DownloadRepository", "Error fetching video info for URL: $sanitized with full stack trace:", e)
            throw e
        }
    }

    suspend fun getDownloadById(id: Long): DownloadEntity? = withContext(Dispatchers.IO) {
        downloadDao.getDownloadById(id)
    }

    suspend fun insertDownload(item: DownloadEntity): Long = withContext(Dispatchers.IO) {
        downloadDao.insert(item)
    }

    suspend fun updateDownload(item: DownloadEntity) = withContext(Dispatchers.IO) {
        downloadDao.update(item)
    }

    suspend fun deleteDownload(item: DownloadEntity) = withContext(Dispatchers.IO) {
        try {
            if (item.filePath.isNotEmpty()) {
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error deleting physical file", e)
        }
        downloadDao.delete(item)
    }

    suspend fun exportToGallery(item: DownloadEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(item.filePath)
            if (item.filePath.isEmpty() || !sourceFile.exists() || sourceFile.length() <= 0) {
                Log.w("DownloadRepository", "Cannot export to gallery: file does not exist or is empty (${item.filePath})")
                return@withContext false
            }

            val isVideo = item.mediaType == "VIDEO"
            val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"
            val collectionUri = if (isVideo) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_MOVIES + "/StreamClean" else Environment.DIRECTORY_MUSIC + "/StreamClean"
                    )
                }
            }

            val uri: Uri? = context.contentResolver.insert(collectionUri, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                return@withContext true
            }
            false
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error exporting to MediaStore", e)
            false
        }
    }

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        if (downloadDao.getCount() == 0) {
            // Seed the 3 files visible in Image 3 screenshot
            val initialFiles = listOf(
                DownloadEntity(
                    id = 1,
                    title = "Cinematic_Drone_Reel_202...",
                    fileName = "Cinematic_Drone_Reel_2024.mp4",
                    filePath = "",
                    fileSizeBytes = 44879052L,
                    formattedSize = "42.8 MB",
                    duration = "04:12",
                    thumbnailUri = "drone_thumb",
                    mediaType = "VIDEO",
                    resolution = "1080p",
                    status = "COMPLETED",
                    progress = 100,
                    relativeDate = "Today",
                    timestamp = System.currentTimeMillis()
                ),
                DownloadEntity(
                    id = 2,
                    title = "Product_Design_Tutorial.mp4",
                    fileName = "Product_Design_Tutorial.mp4",
                    filePath = "",
                    fileSizeBytes = 123941683L,
                    formattedSize = "118.2 MB",
                    duration = "12:45",
                    thumbnailUri = "tutorial_thumb",
                    mediaType = "VIDEO",
                    resolution = "720p",
                    status = "COMPLETED",
                    progress = 100,
                    relativeDate = "Yesterday",
                    timestamp = System.currentTimeMillis() - 86400000L
                ),
                DownloadEntity(
                    id = 3,
                    title = "Motion_Graphics_Explainer....",
                    fileName = "Motion_Graphics_Explainer_V2.mp4",
                    filePath = "",
                    fileSizeBytes = 16148070L,
                    formattedSize = "15.4 MB",
                    duration = "01:30",
                    thumbnailUri = "graphics_thumb",
                    mediaType = "VIDEO",
                    resolution = "720p",
                    status = "COMPLETED",
                    progress = 100,
                    relativeDate = "3 days ago",
                    timestamp = System.currentTimeMillis() - 86400000L * 3
                )
            )
            downloadDao.insertAll(initialFiles)
        }
    }
}
