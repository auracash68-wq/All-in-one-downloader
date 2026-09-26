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
    val privateDownloads: Flow<List<DownloadEntity>> = downloadDao.getPrivateDownloads()
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

    suspend fun insertOrUpdateDownload(item: DownloadEntity): Long = withContext(Dispatchers.IO) {
        downloadDao.insert(item)
    }

    suspend fun updateDownload(item: DownloadEntity) = withContext(Dispatchers.IO) {
        downloadDao.update(item)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        downloadDao.deleteById(id)
    }

    /**
     * Checks if a verified media item matching the given URL or metadata already exists in Downloads.
     * Returns the matching DownloadEntity if a verified local file exists, or null otherwise.
     */
    suspend fun findVerifiedDownloadedMedia(
        url: String,
        metadata: VideoMetadata? = null,
        mediaType: String? = null
    ): DownloadEntity? = withContext(Dispatchers.IO) {
        val completedList = downloadDao.getCompletedList()
        val requestedTitle = metadata?.title?.trim()
        val requestedDuration = metadata?.duration?.trim()

        for (item in completedList) {
            // Must be completed with a real verified non-empty file on disk
            if (item.status != "COMPLETED" || item.filePath.isEmpty()) continue
            val file = File(item.filePath)
            if (!file.exists() || file.length() <= 0) continue

            // If mediaType specified (AUDIO vs VIDEO), must match
            if (mediaType != null && !item.mediaType.equals(mediaType, ignoreCase = true)) {
                continue
            }

            // 1. Check exact or sanitized URL match
            if (isSameUrlOrMedia(item.url, url)) {
                return@withContext item
            }

            // 2. Check metadata title and duration match (when available and non-generic)
            if (!requestedTitle.isNullOrBlank() && !requestedDuration.isNullOrBlank() &&
                requestedTitle != "Media Download" && requestedTitle != "StreamClean Download" &&
                item.title.trim().equals(requestedTitle, ignoreCase = true) &&
                item.duration.trim() == requestedDuration
            ) {
                return@withContext item
            }
        }
        null
    }

    private fun isSameUrlOrMedia(savedUrl: String, newUrl: String): Boolean {
        if (savedUrl.isBlank() || newUrl.isBlank()) return false
        val s1 = savedUrl.substringBefore("?si=").substringBefore("&si=").trim()
        val s2 = newUrl.substringBefore("?si=").substringBefore("&si=").trim()
        if (s1.equals(s2, ignoreCase = true)) return true

        // Check YouTube video ID
        val ytId1 = extractYouTubeVideoId(s1)
        val ytId2 = extractYouTubeVideoId(s2)
        if (ytId1 != null && ytId2 != null && ytId1 == ytId2) return true

        // Check Facebook video/reel ID
        val fbId1 = extractFacebookMediaId(s1)
        val fbId2 = extractFacebookMediaId(s2)
        if (fbId1 != null && fbId2 != null && fbId1 == fbId2) return true

        return false
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val regex = Regex("(?:youtu\\.be/|youtube\\.com/(?:watch\\?.*v=|embed/|shorts/|v/))([a-zA-Z0-9_-]{11})", RegexOption.IGNORE_CASE)
        return regex.find(url)?.groupValues?.get(1)
    }

    private fun extractFacebookMediaId(url: String): String? {
        val regex = Regex("(?:facebook\\.com|fb\\.watch)/(?:.*/)?(?:videos/|reel/|posts/|story\\.php\\?story_fbid=)?(\\d+)", RegexOption.IGNORE_CASE)
        return regex.find(url)?.groupValues?.get(1)
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

    /**
     * Moves a downloaded media file to the app's private external directory:
     * context.getExternalFilesDir("private") with a .nomedia file.
     */
    suspend fun moveToPrivate(item: DownloadEntity): DownloadEntity? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(item.filePath)
            if (item.filePath.isEmpty() || !sourceFile.exists() || sourceFile.length() <= 0) {
                Log.w("DownloadRepository", "Cannot move to private: file does not exist or empty (${item.filePath})")
                return@withContext null
            }

            val privateDir = context.getExternalFilesDir("private") ?: File(context.filesDir, "private")
            if (!privateDir.exists()) {
                privateDir.mkdirs()
            }

            // Create .nomedia file so Android Media Scanner does NOT index this folder
            val noMediaFile = File(privateDir, ".nomedia")
            if (!noMediaFile.exists()) {
                try {
                    noMediaFile.createNewFile()
                } catch (e: Exception) {
                    Log.w("DownloadRepository", "Could not create .nomedia file: ${e.message}")
                }
            }

            val destFile = File(privateDir, sourceFile.name)
            val moved = if (sourceFile.renameTo(destFile)) {
                true
            } else {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                sourceFile.delete()
                true
            }

            if (moved && destFile.exists()) {
                val updated = item.copy(
                    filePath = destFile.absolutePath,
                    isPrivate = true
                )
                downloadDao.update(updated)
                Log.i("DownloadRepository", "Successfully moved ${item.title} to private folder: ${destFile.absolutePath}")
                return@withContext updated
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error moving file to private directory", e)
        }
        null
    }

    /**
     * Moves a private media file back to the public/normal downloads directory.
     */
    suspend fun removeFromPrivate(item: DownloadEntity): DownloadEntity? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(item.filePath)
            if (item.filePath.isEmpty() || !sourceFile.exists() || sourceFile.length() <= 0) {
                Log.w("DownloadRepository", "Cannot remove from private: file does not exist (${item.filePath})")
                return@withContext null
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val destFile = File(downloadsDir, sourceFile.name)
            val moved = if (sourceFile.renameTo(destFile)) {
                true
            } else {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                sourceFile.delete()
                true
            }

            if (moved && destFile.exists()) {
                val updated = item.copy(
                    filePath = destFile.absolutePath,
                    isPrivate = false
                )
                downloadDao.update(updated)
                Log.i("DownloadRepository", "Successfully removed ${item.title} from private folder: ${destFile.absolutePath}")
                return@withContext updated
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error removing file from private directory", e)
        }
        null
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
}
