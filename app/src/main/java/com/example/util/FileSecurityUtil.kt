package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/**
 * Utility providing strict filesystem security, filename sanitization,
 * canonical path verification, and secure FileProvider share intent creation.
 */
object FileSecurityUtil {

    private const val TAG = "FileSecurityUtil"
    private const val MAX_FILENAME_LENGTH = 60
    private const val DEFAULT_FALLBACK_NAME = "media_download"

    // Reserved filesystem device names (case-insensitive) on FAT/exFAT/Windows compatibility layers
    private val RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * Sanitizes a raw title or filename to eliminate path traversal (../, ..\\),
     * absolute path prefixes, control characters, filesystem illegal characters,
     * reserved device names, and unexpected separators.
     */
    fun sanitizeFileName(rawName: String): String {
        if (rawName.isBlank()) {
            return DEFAULT_FALLBACK_NAME
        }

        // 1. Remove absolute path roots and path separators (/ and \)
        var cleaned = rawName
            .replace('\\', '_')
            .replace('/', '_')

        // 2. Remove all control characters (\u0000 to \u001F and \u007F)
        cleaned = cleaned.replace(Regex("[\\p{Cntrl}]"), "")

        // 3. Remove filesystem-prohibited and dangerous characters (< > : " | ? * ~ ;)
        cleaned = cleaned.replace(Regex("[<>:\"|?*~;]"), "_")

        // 4. Specifically prevent directory traversal sequences (..)
        while (cleaned.contains("..")) {
            cleaned = cleaned.replace("..", "_")
        }

        // 5. Replace any remaining non-standard characters with safe underscores
        // Retain alphanumeric, standard unicode letters, hyphens, underscores, and dots
        cleaned = cleaned.replace(Regex("[^a-zA-Z0-9._\\-\\p{L}]"), "_")

        // 6. Collapse consecutive underscores and dots
        cleaned = cleaned.replace(Regex("_{2,}"), "_")
        cleaned = cleaned.replace(Regex("\\.{2,}"), ".")

        // 7. Strip leading and trailing dots, underscores, dashes, and whitespace
        cleaned = cleaned.trim('.', '_', '-', ' ')

        // 8. If cleaned string is empty or matches reserved name, safeguard it
        if (cleaned.isBlank()) {
            return DEFAULT_FALLBACK_NAME
        }

        val baseNameWithoutExt = cleaned.substringBeforeLast(".")
        val upperBase = baseNameWithoutExt.uppercase(Locale.US)
        if (RESERVED_NAMES.contains(upperBase)) {
            cleaned = "safe_$cleaned"
        }

        // 9. Enforce maximum character length to prevent filesystem path length limits
        if (cleaned.length > MAX_FILENAME_LENGTH) {
            val dotIndex = cleaned.lastIndexOf('.')
            cleaned = if (dotIndex > 0 && dotIndex > cleaned.length - 6) {
                val ext = cleaned.substring(dotIndex)
                val base = cleaned.substring(0, dotIndex).take(MAX_FILENAME_LENGTH - ext.length)
                "${base}$ext"
            } else {
                cleaned.take(MAX_FILENAME_LENGTH)
            }
        }

        return cleaned.ifBlank { DEFAULT_FALLBACK_NAME }
    }

    /**
     * Returns the approved directory for normal downloaded media:
     * <ExternalFilesDir>/Download/StreamClean
     */
    fun getApprovedDownloadDirectory(context: Context): File {
        val baseDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Download")
        val dir = File(baseDownloadDir, "StreamClean")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns the approved directory for private/vault media:
     * <ExternalFilesDir>/private
     */
    fun getApprovedPrivateDirectory(context: Context): File {
        val dir = context.getExternalFilesDir("private") ?: File(context.filesDir, "private")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks whether a file's canonical path resides strictly within the app's approved
     * download or private media directories, protecting against path traversal and unauthorized
     * access to internal app files (databases, shared preferences, code caches).
     */
    fun isPathInApprovedDirectory(
        context: Context,
        file: File,
        allowPrivate: Boolean = true
    ): Boolean {
        return try {
            val canonicalTarget = file.canonicalFile

            // Explicitly reject internal sensitive directories: databases, shared_prefs, internal root
            val appDataDir = context.applicationInfo.dataDir
            if (appDataDir != null) {
                val dbDir = File(appDataDir, "databases").canonicalFile
                val spDir = File(appDataDir, "shared_prefs").canonicalFile
                if (isSubpath(dbDir, canonicalTarget) || isSubpath(spDir, canonicalTarget)) {
                    Log.w(TAG, "Access rejected: target resides in private app database/shared_prefs directory")
                    return false
                }
            }

            // Approved download directories
            val approvedDownloadDir = getApprovedDownloadDirectory(context).canonicalFile
            val baseDownloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.canonicalFile

            if (isSubpath(approvedDownloadDir, canonicalTarget)) return true
            if (baseDownloadDir != null && isSubpath(baseDownloadDir, canonicalTarget)) return true

            // Approved private vault directory
            if (allowPrivate) {
                val approvedPrivateDir = getApprovedPrivateDirectory(context).canonicalFile
                if (isSubpath(approvedPrivateDir, canonicalTarget)) return true
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking canonical path for ${file.path}", e)
            false
        }
    }

    private fun isSubpath(parent: File, child: File): Boolean {
        val parentPath = parent.canonicalPath
        val childPath = child.canonicalPath
        return childPath == parentPath || childPath.startsWith(parentPath + File.separator)
    }

    /**
     * Validates that a file is legitimate, existing, non-empty, and strictly inside
     * an approved directory for sharing via FileProvider.
     * Throws SecurityException if validation fails.
     */
    fun validateShareableFile(context: Context, file: File): File {
        if (!file.exists() || !file.isFile || file.length() <= 0) {
            throw IllegalArgumentException("File does not exist or is empty: ${file.path}")
        }

        val canonicalFile = file.canonicalFile

        // Reject if outside approved directories
        if (!isPathInApprovedDirectory(context, canonicalFile, allowPrivate = true)) {
            Log.e(TAG, "Security alert: Attempted to share unauthorized file: ${canonicalFile.canonicalPath}")
            throw SecurityException("Security violation: Target file is outside approved media directories")
        }

        return canonicalFile
    }

    /**
     * Generates a safe share Intent with explicit URI permissions granted via ClipData
     * and FLAG_GRANT_READ_URI_PERMISSION.
     */
    fun createShareIntent(
        context: Context,
        file: File,
        title: String,
        mediaType: String? = null
    ): Intent {
        val validFile = validateShareableFile(context, file)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            validFile
        )

        val isVideo = mediaType == "VIDEO" ||
                validFile.name.endsWith(".mp4", ignoreCase = true) ||
                validFile.name.endsWith(".mkv", ignoreCase = true) ||
                validFile.name.endsWith(".webm", ignoreCase = true)

        val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
