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
import com.example.util.FileSecurityUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val duplicateMutex = Mutex()

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

    suspend fun updateDownloadStatus(id: Long, status: String, progress: Int = 0) = withContext(Dispatchers.IO) {
        downloadDao.updateStatus(id, status, progress)
    }

    suspend fun updateDownloadProgress(
        id: Long,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) = withContext(Dispatchers.IO) {
        downloadDao.updateProgress(id, progress, downloadedBytes, totalBytes)
    }

    suspend fun updateDownloadStatusDetails(
        id: Long,
        status: String,
        progress: Int = 0,
        error: String = "",
        tempFilePath: String = ""
    ) = withContext(Dispatchers.IO) {
        downloadDao.updateStatusDetails(id, status, progress, error, tempFilePath)
    }

    suspend fun getInterruptedDownloads(): List<DownloadEntity> = withContext(Dispatchers.IO) {
        downloadDao.getInterruptedDownloads()
    }

    suspend fun markInterruptedDownloadsAsPaused(reason: String = "Interrupted by system shutdown") = withContext(Dispatchers.IO) {
        downloadDao.markInterruptedDownloadsAsPaused(status = "PAUSED", error = reason)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        downloadDao.deleteById(id)
    }

    /**
     * Checks if a verified media item matching the given URL or metadata already exists in Downloads.
     * Returns the matching DownloadEntity if a verified local file exists, or null otherwise.
     * Protected by duplicateMutex to eliminate concurrent check race conditions.
     */
    suspend fun findVerifiedDownloadedMedia(
        url: String,
        metadata: VideoMetadata? = null,
        mediaType: String? = null
    ): DownloadEntity? = withContext(Dispatchers.IO) {
        duplicateMutex.withLock {
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
                    return@withLock item
                }

                // 2. Check metadata title and duration match (when available and non-generic)
                if (!requestedTitle.isNullOrBlank() && !requestedDuration.isNullOrBlank() &&
                    requestedTitle != "Media Download" && requestedTitle != "StreamClean Download" &&
                    item.title.trim().equals(requestedTitle, ignoreCase = true) &&
                    item.duration.trim() == requestedDuration
                ) {
                    return@withLock item
                }
            }
            null
        }
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
                // Security check: only delete file if it strictly resides inside approved directories
                if (FileSecurityUtil.isPathInApprovedDirectory(context, file, allowPrivate = true)) {
                    if (file.exists()) {
                        file.delete()
                    }
                } else {
                    Log.w("DownloadRepository", "Security block: refusing to delete file outside approved directories: ${file.path}")
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

            // Security check: source file must be in approved download directory
            if (!FileSecurityUtil.isPathInApprovedDirectory(context, sourceFile, allowPrivate = false)) {
                Log.e("DownloadRepository", "Security block: source file not in approved download directory: ${sourceFile.canonicalPath}")
                return@withContext null
            }

            val privateDir = FileSecurityUtil.getApprovedPrivateDirectory(context)

            // Create .nomedia file so Android Media Scanner does NOT index this folder
            val noMediaFile = File(privateDir, ".nomedia")
            if (!noMediaFile.exists()) {
                try {
                    noMediaFile.createNewFile()
                } catch (e: Exception) {
                    Log.w("DownloadRepository", "Could not create .nomedia file: ${e.message}")
                }
            }

            val safeName = FileSecurityUtil.sanitizeFileName(sourceFile.name)
            val destFile = File(privateDir, safeName)
            if (!FileSecurityUtil.isPathInApprovedDirectory(context, destFile, allowPrivate = true)) {
                Log.e("DownloadRepository", "Security block: destination file path escapes private directory: ${destFile.path}")
                return@withContext null
            }

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

            // Security check: source file must be in approved private directory
            if (!FileSecurityUtil.isPathInApprovedDirectory(context, sourceFile, allowPrivate = true)) {
                Log.e("DownloadRepository", "Security block: source file not in approved private directory: ${sourceFile.canonicalPath}")
                return@withContext null
            }

            val downloadsDir = FileSecurityUtil.getApprovedDownloadDirectory(context)
            val safeName = FileSecurityUtil.sanitizeFileName(sourceFile.name)
            val destFile = File(downloadsDir, safeName)
            if (!FileSecurityUtil.isPathInApprovedDirectory(context, destFile, allowPrivate = false)) {
                Log.e("DownloadRepository", "Security block: destination file path escapes download directory: ${destFile.path}")
                return@withContext null
            }

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
        var insertedUri: Uri? = null
        try {
            val sourceFile = File(item.filePath)
            if (item.filePath.isEmpty() || !sourceFile.exists() || sourceFile.length() <= 0) {
                Log.w("DownloadRepository", "Cannot export to gallery: file does not exist or is empty (${item.filePath})")
                return@withContext false
            }

            // Security check: source file must be in approved media directory
            if (!FileSecurityUtil.isPathInApprovedDirectory(context, sourceFile, allowPrivate = true)) {
                Log.e("DownloadRepository", "Security violation: export source outside approved directories: ${sourceFile.canonicalPath}")
                return@withContext false
            }

            val isVideo = item.mediaType == "VIDEO" || sourceFile.extension.lowercase() in setOf("mp4", "mkv", "webm", "mov", "3gp", "avi")
            val mimeType = resolveMimeType(sourceFile, isVideo)
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

            // Sanitize file name for MediaStore DISPLAY_NAME to protect against path traversal
            val baseName = if (item.fileName.isNotBlank()) item.fileName else sourceFile.name
            val safeDisplayName = FileSecurityUtil.sanitizeFileName(baseName)
            val correctExt = if (sourceFile.extension.isNotBlank()) ".${sourceFile.extension}" else if (isVideo) ".mp4" else ".mp3"
            val finalDisplayName = if (safeDisplayName.endsWith(correctExt, ignoreCase = true)) {
                safeDisplayName
            } else {
                "${safeDisplayName.substringBeforeLast(".")}$correctExt"
            }

            val nowSeconds = System.currentTimeMillis() / 1000
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalDisplayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.TITLE, item.title.ifBlank { safeDisplayName })
                put(MediaStore.MediaColumns.DATE_ADDED, nowSeconds)
                put(MediaStore.MediaColumns.DATE_MODIFIED, nowSeconds)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isVideo) Environment.DIRECTORY_MOVIES + "/StreamClean" else Environment.DIRECTORY_MUSIC + "/StreamClean"
                    )
                }
            }

            insertedUri = context.contentResolver.insert(collectionUri, values)
            if (insertedUri == null) {
                Log.e("DownloadRepository", "MediaStore insert returned null URI")
                return@withContext false
            }

            // Stream file contents in 64 KB chunks to prevent loading large files into memory
            val success = try {
                context.contentResolver.openOutputStream(insertedUri)?.use { outStream ->
                    FileInputStream(sourceFile).use { inStream ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        var totalCopied = 0L
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            outStream.write(buffer, 0, bytesRead)
                            totalCopied += bytesRead
                        }
                        outStream.flush()
                        totalCopied > 0 && totalCopied == sourceFile.length()
                    }
                } ?: false
            } catch (copyEx: Exception) {
                Log.e("DownloadRepository", "Error streaming media to MediaStore output stream", copyEx)
                false
            }

            if (success) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(insertedUri, values, null, null)
                }
                Log.i("DownloadRepository", "Successfully exported ${sourceFile.name} to MediaStore: $insertedUri")
                return@withContext true
            } else {
                // Failed export: clean up pending / corrupt MediaStore record so no ghost entries remain
                try {
                    context.contentResolver.delete(insertedUri, null, null)
                } catch (cleanupEx: Exception) {
                    Log.w("DownloadRepository", "Failed to clean up aborted MediaStore entry: ${cleanupEx.message}")
                }
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("DownloadRepository", "Error exporting to MediaStore", e)
            insertedUri?.let { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (cleanupEx: Exception) {
                    Log.w("DownloadRepository", "Failed to clean up aborted MediaStore entry: ${cleanupEx.message}")
                }
            }
            false
        }
    }

    private fun resolveMimeType(file: File, isVideo: Boolean): String {
        val ext = file.extension.lowercase()
        return when {
            isVideo -> when (ext) {
                "mp4", "m4v" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "mov" -> "video/quicktime"
                "3gp" -> "video/3gpp"
                "avi" -> "video/x-msvideo"
                "ts" -> "video/mp2t"
                else -> "video/mp4"
            }
            else -> when (ext) {
                "mp3" -> "audio/mpeg"
                "m4a", "aac" -> "audio/mp4"
                "ogg", "oga" -> "audio/ogg"
                "opus" -> "audio/opus"
                "flac" -> "audio/flac"
                "wav" -> "audio/wav"
                else -> "audio/mpeg"
            }
        }
    }
}
