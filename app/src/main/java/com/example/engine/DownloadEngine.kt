package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.local.DownloadEntity
import com.example.data.repository.DownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class VideoFormatInfo(
    val formatId: String,
    val resolution: String,
    val note: String = "",
    val ext: String = "mp4",
    val filesizeApprox: String = "",
    val isAudio: Boolean = false,
    val isHdr: Boolean = false
)

data class VideoMetadata(
    val title: String,
    val duration: String,
    val durationSeconds: Int = 0,
    val thumbnailUrl: String?,
    val videoFormats: List<VideoFormatInfo> = emptyList(),
    val audioFormats: List<VideoFormatInfo> = emptyList()
) {
    val formats: List<VideoFormatInfo>
        get() = if (videoFormats.isNotEmpty()) videoFormats else audioFormats

    val hasSupportedVideoQuality: Boolean
        get() = videoFormats.isNotEmpty()

    constructor(
        title: String,
        duration: String,
        thumbnailUrl: String?,
        formats: List<VideoFormatInfo>
    ) : this(
        title = title,
        duration = duration,
        durationSeconds = 0,
        thumbnailUrl = thumbnailUrl,
        videoFormats = formats,
        audioFormats = emptyList()
    )
}

data class ActiveDownloadState(
    val id: Long = 0,
    val title: String = "",
    val fileName: String = "",
    val progress: Int = 0,
    val speed: String = "",
    val eta: String = "",
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String = "",
    val thumbnailUrl: String? = null,
    val duration: String = "",
    val formattedSize: String = "",
    val filePath: String = "",
    val mediaType: String = "VIDEO",
    val queueIndex: Int = 0,
    val queueTotal: Int = 0
)

class DownloadEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val mutex = Mutex()
    private val downloadChannel = Channel<DownloadTask>(Channel.UNLIMITED)

    private val _activeDownload = MutableStateFlow<ActiveDownloadState?>(null)
    val activeDownload: StateFlow<ActiveDownloadState?> = _activeDownload.asStateFlow()

    private var currentProcessId: String? = null
    private var isCancelled = false

    private val initMutex = Mutex()
    @Volatile
    private var isEngineInitialized = false

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private suspend fun ensureEngineInitialized() = withContext(Dispatchers.IO) {
        if (isEngineInitialized) return@withContext
        initMutex.withLock {
            if (isEngineInitialized) return@withContext
            try {
                Log.i(TAG, "Initializing YoutubeDL and FFmpeg native runtimes...")
                YoutubeDL.getInstance().init(context.applicationContext)
                try {
                    com.yausername.ffmpeg.FFmpeg.getInstance().init(context.applicationContext)
                } catch (t: Throwable) {
                    Log.w(TAG, "FFmpeg initialization notice: ${t.message}")
                }

                try {
                    Log.i(TAG, "Updating yt-dlp executable to latest stable release...")
                    val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel.STABLE)
                    Log.i(TAG, "yt-dlp update status: $status")
                } catch (ut: Throwable) {
                    Log.w(TAG, "yt-dlp update attempt notice: ${ut.message}")
                }

                isEngineInitialized = true
                Log.i(TAG, "YoutubeDL and FFmpeg initialized successfully")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to initialize YoutubeDL backend", e)
                throw e
            }
        }
    }

    private data class DownloadTask(
        val entityId: Long,
        val url: String,
        val title: String,
        val mediaType: String,
        val resolution: String,
        val formatId: String,
        val audioBitrate: String,
        val thumbnailUrl: String?,
        val duration: String,
        val queueIndex: Int = 0,
        val queueTotal: Int = 0,
        val repository: DownloadRepository
    )

    init {
        scope.launch {
            for (task in downloadChannel) {
                mutex.withLock {
                    processDownloadTask(task)
                }
            }
        }
    }

    /**
     * Check if device is under critical memory pressure (Feature 3)
     */
    fun isMemoryCriticallyLow(): Boolean {
        val am = activityManager ?: return false
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory || memoryInfo.availMem < (250L * 1024 * 1024)
    }

    suspend fun fetchFormats(url: String): VideoMetadata = withContext(Dispatchers.IO) {
        val sanitized = sanitizeUrlInput(url)

        // 1. Direct media links (.mp4, .mp3, etc.)
        if (isDirectMediaUrl(sanitized)) {
            val directMeta = probeDirectMediaUrl(sanitized)
            if (directMeta != null) {
                return@withContext directMeta
            }
        }

        // 2. Real extraction via YoutubeDL
        ensureEngineInitialized()
        Log.d(TAG, "Fetching real video info using YoutubeDL for: $sanitized")

        val request = YoutubeDLRequest(sanitized).apply {
            addOption("--no-warnings")
            addOption("--no-update")
            addOption("--no-check-certificates")
            if (isDailymotionUrl(sanitized)) {
                addOption("--referer", "https://www.dailymotion.com/")
                addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            } else if (isBilibiliUrl(sanitized)) {
                addOption("--referer", "https://www.bilibili.com/")
                addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            }
        }

        val videoInfo: VideoInfo = try {
            YoutubeDL.getInstance().getInfo(request)
        } catch (e: Throwable) {
            Log.e(TAG, "YoutubeDL getInfo failed for $sanitized: ${e.message}")
            throw e
        }

        var title = videoInfo.title?.takeIf { it.isNotBlank() }
        var thumb = videoInfo.thumbnail?.takeIf { it.isNotBlank() }

        // Supplemental oEmbed lookup if needed
        if ((title == null || thumb == null) && isYouTubeUrl(sanitized)) {
            val supplemental = fetchOEmbedSupplemental(sanitized)
            if (supplemental != null) {
                if (title == null && supplemental.first.isNotBlank()) {
                    title = supplemental.first
                }
                if (thumb == null && supplemental.second.isNotBlank()) {
                    thumb = supplemental.second
                }
            }
        }

        val finalTitle = title ?: generateFallbackTitle(sanitized)
        val durationSecs = videoInfo.duration
        val durationStr = formatDuration(durationSecs)

        // Parse video formats strictly applying the 144p to 720p policy
        val videoFormats = parseVideoFormats(videoInfo.formats, durationSecs)
        // Parse audio formats without the 144p-720p restriction
        val audioFormats = parseAudioFormats(videoInfo.formats, durationSecs)

        Log.d(TAG, "Fetched real formats via YoutubeDL: '$finalTitle', duration: $durationStr, video formats (144p-720p): ${videoFormats.size}, audio formats: ${audioFormats.size}")

        return@withContext VideoMetadata(
            title = finalTitle,
            duration = durationStr,
            durationSeconds = durationSecs,
            thumbnailUrl = thumb,
            videoFormats = videoFormats,
            audioFormats = audioFormats
        )
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtu.be", ignoreCase = true) || url.contains("youtube.com", ignoreCase = true)
    }

    private fun isDailymotionUrl(url: String): Boolean {
        return url.contains("dailymotion.com", ignoreCase = true) || url.contains("dai.ly", ignoreCase = true)
    }

    private fun isBilibiliUrl(url: String): Boolean {
        return url.contains("bilibili.com", ignoreCase = true) || url.contains("b23.tv", ignoreCase = true)
    }

    private fun fetchOEmbedSupplemental(url: String): Pair<String, String>? {
        return try {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
            val connection = URL(oembedUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
            connection.connect()

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(jsonStr)
                val title = json.optString("title")
                val thumb = json.optString("thumbnail_url")
                Pair(title, thumb)
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun probeDirectMediaUrl(url: String): VideoMetadata? {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
            }
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) return null

            val contentType = connection.contentType?.lowercase() ?: ""
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                return null
            }

            val length = connection.contentLengthLong
            val rawName = url.substringAfterLast("/").substringBefore("?").ifEmpty { "downloaded_media" }
            val cleanExt = rawName.substringAfterLast(".", "mp4").lowercase()
            val isAudio = cleanExt in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus") || contentType.contains("audio")

            val sizeStr = if (length > 0) "~${formatFileSize(length)}" else ""
            val fmt = VideoFormatInfo(
                formatId = "direct",
                resolution = if (isAudio) "Direct Audio Stream" else "Direct Video Stream (720p)",
                note = if (length > 0) formatFileSize(length) else "Direct Stream",
                ext = cleanExt,
                filesizeApprox = sizeStr,
                isAudio = isAudio
            )

            VideoMetadata(
                title = rawName,
                duration = "",
                durationSeconds = 0,
                thumbnailUrl = null,
                videoFormats = if (!isAudio) listOf(fmt) else emptyList(),
                audioFormats = if (isAudio) listOf(fmt) else emptyList()
            )
        } catch (e: Throwable) {
            null
        }
    }

    fun enqueueDownload(
        entityId: Long,
        url: String,
        title: String,
        mediaType: String,
        resolution: String,
        formatId: String = "",
        audioBitrate: String = "192kbps",
        thumbnailUrl: String? = null,
        duration: String = "",
        queueIndex: Int = 0,
        queueTotal: Int = 0,
        repository: DownloadRepository
    ) {
        val task = DownloadTask(
            entityId = entityId,
            url = url,
            title = title,
            mediaType = mediaType,
            resolution = resolution,
            formatId = formatId,
            audioBitrate = audioBitrate,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            queueIndex = queueIndex,
            queueTotal = queueTotal,
            repository = repository
        )
        downloadChannel.trySend(task)
    }

    private suspend fun processDownloadTask(task: DownloadTask) = withContext(Dispatchers.IO) {
        val downloadDir = getDownloadDirectory()
        val extension = if (task.mediaType == "AUDIO") "mp3" else "mp4"
        val safeTitle = sanitizeFileName(task.title).take(60)
        val safeFileName = "${safeTitle}_${System.currentTimeMillis() % 100000}.$extension"
        val outputFile = File(downloadDir, safeFileName)
        val processId = "dl_${task.entityId}_${System.currentTimeMillis()}"
        currentProcessId = processId
        isCancelled = false

        // Cache thumbnail locally so offline viewing in Downloads tab has reliable original art
        val localThumbPath = cacheThumbnailLocally(task.thumbnailUrl, safeTitle)

        _activeDownload.value = ActiveDownloadState(
            id = task.entityId,
            title = task.title,
            fileName = safeFileName,
            progress = 0,
            speed = "Starting...",
            eta = "",
            isRunning = true,
            isCompleted = false,
            thumbnailUrl = localThumbPath ?: task.thumbnailUrl,
            duration = task.duration,
            mediaType = task.mediaType,
            queueIndex = task.queueIndex,
            queueTotal = task.queueTotal
        )

        try {
            val sanitizedUrl = sanitizeUrlInput(task.url)

            if (isDirectMediaUrl(sanitizedUrl) || task.formatId == "direct") {
                Log.i(TAG, "Starting direct media download for: $sanitizedUrl")
                val directSuccess = runDirectDownload(sanitizedUrl, outputFile)
                if (!directSuccess && !isCancelled) {
                    markTaskFailed(task, safeFileName, "Direct stream download failed")
                    return@withContext
                }
            } else {
                ensureEngineInitialized()
                Log.d(TAG, "Executing YoutubeDL download for: $sanitizedUrl with formatId: ${task.formatId}")

                val request = YoutubeDLRequest(sanitizedUrl).apply {
                    addOption("-o", outputFile.absolutePath)
                    addOption("--no-mtime")
                    addOption("--no-playlist")
                    addOption("--no-part")
                    addOption("--no-warnings")
                    addOption("--no-update")
                    addOption("--no-check-certificates")
                    if (isDailymotionUrl(sanitizedUrl)) {
                        addOption("--referer", "https://www.dailymotion.com/")
                        addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    } else if (isBilibiliUrl(sanitizedUrl)) {
                        addOption("--referer", "https://www.bilibili.com/")
                        addOption("--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    } else {
                        // Concurrency optimization for high download speed without server throttling
                        addOption("-N", "4")
                    }
                    addOption("--buffer-size", "64k")
                    addOption("--retries", "3")
                    addOption("--socket-timeout", "15")

                    if (task.mediaType == "AUDIO") {
                        addOption("-f", "bestaudio/best")
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                        val bitrate = task.audioBitrate.replace("kbps", "").trim().ifEmpty { "192" }
                        addOption("--audio-quality", "${bitrate}K")
                    } else {
                        val fmt = task.formatId
                        val selector = if (fmt.isNotEmpty() && fmt != "direct") {
                            if (fmt.contains("+") || fmt.contains("/")) {
                                fmt
                            } else if (fmt.all { it.isDigit() }) {
                                "$fmt+bestaudio/best"
                            } else if (fmt.contains("p")) {
                                val h = fmt.replace("p", "").replace(" HDR", "").trim()
                                "bestvideo[height<=$h]+bestaudio/best[height<=$h]/best"
                            } else if (fmt == "HIGH") {
                                "bestvideo[height<=720]+bestaudio/best[height<=720]/best"
                            } else if (fmt == "LOW") {
                                "bestvideo[height<=360]+bestaudio/best[height<=360]/best"
                            } else {
                                "$fmt+bestaudio/best"
                            }
                        } else {
                            val h = Regex("\\d+").find(task.resolution)?.value ?: "720"
                            "bestvideo[height<=$h]+bestaudio/best[height<=$h]/best"
                        }
                        addOption("-f", selector)
                        addOption("--merge-output-format", "mp4")
                    }
                }

                YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                    if (!isCancelled) {
                        val p = progress.toInt().coerceIn(0, 100)
                        val spd = extractSpeed(line)
                        _activeDownload.value = _activeDownload.value?.copy(
                            progress = p,
                            speed = if (p >= 100) "Processing..." else spd,
                            eta = if (etaInSeconds > 0 && p < 100) "${etaInSeconds}s" else ""
                        )
                    }
                }
            }

            if (isCancelled) {
                handleCancellation(task, downloadDir, safeFileName)
                return@withContext
            }

            // Identify actual output file created
            val actualFile = if (outputFile.exists() && outputFile.length() > 0) {
                outputFile
            } else {
                val possibleFiles = downloadDir.listFiles()?.filter {
                    it.name.startsWith(safeTitle) && it.length() > 0 &&
                    !it.name.endsWith(".part") && !it.name.endsWith(".ytdl")
                }
                possibleFiles?.maxByOrNull { it.lastModified() } ?: outputFile
            }

            // Strict validation of the real downloaded media
            val isValid = validateDownloadedMedia(actualFile, task.mediaType)

            if (isValid) {
                val fileSize = actualFile.length()
                val formattedSize = formatFileSize(fileSize)
                val finalThumb = localThumbPath ?: task.thumbnailUrl

                val updatedEntity = DownloadEntity(
                    id = task.entityId,
                    url = task.url,
                    title = task.title,
                    fileName = actualFile.name,
                    filePath = actualFile.absolutePath,
                    fileSizeBytes = fileSize,
                    formattedSize = formattedSize,
                    duration = task.duration,
                    thumbnailUri = finalThumb,
                    mediaType = task.mediaType,
                    resolution = task.resolution,
                    status = "COMPLETED",
                    progress = 100,
                    relativeDate = "Today",
                    timestamp = System.currentTimeMillis()
                )
                task.repository.updateDownload(updatedEntity)
                Log.i(TAG, "Download finished successfully and verified: ${actualFile.name} ($formattedSize)")

                // Update active state to successful completed state with checkmark info
                _activeDownload.value = ActiveDownloadState(
                    id = task.entityId,
                    title = task.title,
                    fileName = actualFile.name,
                    progress = 100,
                    speed = "",
                    eta = "",
                    isRunning = false,
                    isCompleted = true,
                    isFailed = false,
                    thumbnailUrl = finalThumb,
                    duration = task.duration,
                    formattedSize = formattedSize,
                    filePath = actualFile.absolutePath,
                    mediaType = task.mediaType,
                    queueIndex = task.queueIndex,
                    queueTotal = task.queueTotal
                )
            } else {
                Log.e(TAG, "Output validation failed for ${task.title}. Marking as FAILED.")
                if (actualFile.exists() && actualFile.length() == 0L) {
                    actualFile.delete()
                }
                markTaskFailed(task, safeFileName, "Validation failed: output media is invalid or empty")
            }
        } catch (e: Exception) {
            if (isCancelled) {
                handleCancellation(task, downloadDir, safeFileName)
                return@withContext
            }
            Log.e(TAG, "Download execution failed for ${task.title}", e)
            markTaskFailed(task, safeFileName, e.message ?: "Download execution failed")
        } finally {
            cleanupTempFiles(downloadDir, safeFileName)
            currentProcessId = null
        }
    }

    fun dismissActiveCard() {
        _activeDownload.value = null
    }

    private suspend fun markTaskFailed(task: DownloadTask, fileName: String, errorReason: String) {
        val failedEntity = DownloadEntity(
            id = task.entityId,
            url = task.url,
            title = task.title,
            fileName = fileName,
            filePath = "",
            fileSizeBytes = 0L,
            formattedSize = "0 MB",
            duration = task.duration,
            thumbnailUri = task.thumbnailUrl,
            mediaType = task.mediaType,
            resolution = task.resolution,
            status = "FAILED",
            progress = 0,
            relativeDate = "Today",
            timestamp = System.currentTimeMillis()
        )
        task.repository.updateDownload(failedEntity)
        _activeDownload.value = ActiveDownloadState(
            id = task.entityId,
            title = task.title,
            fileName = fileName,
            progress = 0,
            isRunning = false,
            isCompleted = false,
            isFailed = true,
            errorMessage = errorReason,
            thumbnailUrl = task.thumbnailUrl,
            duration = task.duration,
            mediaType = task.mediaType,
            queueIndex = task.queueIndex,
            queueTotal = task.queueTotal
        )
        Log.w(TAG, "Task marked as FAILED for ${task.title}: $errorReason")
    }

    private suspend fun handleCancellation(task: DownloadTask, downloadDir: File, safeFileName: String) {
        cleanupTempFiles(downloadDir, safeFileName)
        val file = File(downloadDir, safeFileName)
        if (file.exists()) file.delete()
        task.repository.deleteDownload(
            DownloadEntity(id = task.entityId, title = task.title, fileName = safeFileName)
        )
        _activeDownload.value = null
    }

    private suspend fun runDirectDownload(urlStr: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                Log.e(TAG, "Direct download rejected: invalid URL scheme: $urlStr")
                return@withContext false
            }

            var currentUrl = urlStr
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
                }
                connection.connect()
                val responseCode = connection.responseCode

                if (responseCode in 301..308) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = URL(url, location).toExternalForm()
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }

                if (responseCode !in 200..299) {
                    Log.e(TAG, "Direct download HTTP error response code: $responseCode")
                    return@withContext false
                }
                break
            }

            val conn = connection ?: return@withContext false
            val contentType = conn.contentType?.lowercase() ?: ""
            if (contentType.contains("text/html") || contentType.contains("application/xhtml") || contentType.contains("application/json")) {
                Log.e(TAG, "Direct download rejected: non-media Content-Type ($contentType)")
                return@withContext false
            }

            val totalLength = conn.contentLengthLong
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var lastUpdate = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled) {
                            targetFile.delete()
                            return@withContext false
                        }
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 350) {
                            val durationSec = (now - lastUpdate) / 1000.0
                            val speedBps = if (durationSec > 0) (bytesSinceLastUpdate / durationSec).toLong() else 0L
                            val speedStr = formatSpeed(speedBps)
                            val prog = if (totalLength > 0) {
                                ((downloadedBytes * 100) / totalLength).toInt().coerceIn(0, 99)
                            } else 0
                            val etaStr = if (speedBps > 0 && totalLength > downloadedBytes) {
                                "${(totalLength - downloadedBytes) / speedBps}s"
                            } else ""

                            _activeDownload.value = _activeDownload.value?.copy(
                                progress = prog,
                                speed = speedStr,
                                eta = etaStr
                            )
                            lastUpdate = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                }
            }

            val valid = targetFile.exists() && targetFile.length() > 0 && (totalLength <= 0 || targetFile.length() == totalLength)
            if (!valid && targetFile.exists()) {
                targetFile.delete()
            }
            return@withContext valid
        } catch (e: Exception) {
            Log.e(TAG, "Direct download failed", e)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            return@withContext false
        } finally {
            try {
                connection?.disconnect()
            } catch (ignored: Throwable) {}
        }
    }

    private fun validateDownloadedMedia(file: File, expectedMediaType: String): Boolean {
        if (!file.exists() || !file.isFile || file.length() <= 0) {
            Log.e(TAG, "Media validation failed: file does not exist or is empty (${file.absolutePath})")
            return false
        }

        val ext = file.extension.lowercase()
        val validVideoExts = setOf("mp4", "mkv", "webm", "m4v", "mov", "avi", "3gp")
        val validAudioExts = setOf("mp3", "m4a", "aac", "ogg", "opus", "wav", "flac")

        if (expectedMediaType == "AUDIO" && ext !in validAudioExts) {
            Log.e(TAG, "Media validation failed: expected audio container but got .$ext")
            return false
        }
        if (expectedMediaType == "VIDEO" && ext !in validVideoExts) {
            Log.e(TAG, "Media validation failed: expected video container but got .$ext")
            return false
        }

        try {
            val sampleSize = minOf(file.length(), 512L).toInt()
            val headerBytes = ByteArray(sampleSize)
            FileInputStream(file).use { it.read(headerBytes) }
            val headerStr = String(headerBytes, Charsets.UTF_8).lowercase()

            if (headerStr.contains("<html") ||
                headerStr.contains("<!doctype") ||
                headerStr.contains("{\"error\"") ||
                headerStr.contains("sign in to confirm") ||
                headerStr.contains("video unavailable") ||
                headerStr.contains("streamclean media placeholder")
            ) {
                Log.e(TAG, "Media validation failed: file contains error/text payload")
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Media validation header check exception: ${e.message}")
        }

        return true
    }

    private fun cacheThumbnailLocally(remoteUrl: String?, safeTitle: String): String? {
        if (remoteUrl.isNullOrBlank() || !remoteUrl.startsWith("http")) return null
        return try {
            val thumbDir = File(context.filesDir, "thumbnails")
            if (!thumbDir.exists()) thumbDir.mkdirs()
            val thumbFile = File(thumbDir, "${safeTitle}_${System.currentTimeMillis() % 10000}.jpg")
            val conn = URL(remoteUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(thumbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (thumbFile.exists() && thumbFile.length() > 0) {
                    thumbFile.absolutePath
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache thumbnail locally: ${e.message}")
            null
        }
    }

    private fun isDirectMediaUrl(url: String): Boolean {
        val clean = url.substringBefore("?").lowercase()
        return clean.endsWith(".mp4") || clean.endsWith(".mp3") || clean.endsWith(".m4a") ||
               clean.endsWith(".webm") || clean.endsWith(".mkv") || clean.endsWith(".aac") ||
               clean.endsWith(".wav") || clean.endsWith(".ogg") || clean.endsWith(".flac")
    }

    fun cancelActiveDownload() {
        isCancelled = true
        currentProcessId?.let { pid ->
            try {
                YoutubeDL.getInstance().destroyProcessById(pid)
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying YoutubeDL process: ${e.message}")
            }
        }
        _activeDownload.value = null
    }

    fun pauseForLowMemory() {
        Log.w(TAG, "Emergency Low Memory: Cancelling active download to prevent OOM")
        cancelActiveDownload()
        System.gc()
    }

    private fun cleanupTempFiles(directory: File, baseName: String) {
        try {
            val prefix = baseName.substringBeforeLast(".")
            directory.listFiles()?.forEach { file ->
                if (file.name.endsWith(".part") || file.name.endsWith(".ytdl") ||
                    (file.name.startsWith(prefix) && file.name.contains(".temp"))) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning temp files", e)
        }
    }

    private fun getDownloadDirectory(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "StreamClean")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(60)
    }

    private fun sanitizeUrlInput(url: String): String {
        return url.substringBefore("?si=").substringBefore("&si=").trim()
    }

    private fun generateFallbackTitle(url: String): String {
        return try {
            val host = URL(url).host.replace("www.", "")
            val path = URL(url).path.trim('/')
            if (path.isNotEmpty()) {
                val lastSegment = path.substringAfterLast("/")
                if (lastSegment.length in 3..40) lastSegment else "$host media"
            } else {
                "Media Download"
            }
        } catch (e: Exception) {
            "Media Download"
        }
    }

    private fun extractSpeed(line: String): String {
        val speedMatch = Regex("(\\d+\\.?\\d*\\s*[KkMmGg]i?B/s)").find(line)
        return speedMatch?.value ?: ""
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        val mb = bytesPerSec.toDouble() / (1024 * 1024)
        return if (mb >= 1.0) {
            String.format("%.1f MB/s", mb)
        } else {
            val kb = bytesPerSec.toDouble() / 1024
            String.format("%.0f KB/s", kb)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return if (mb >= 1) {
            String.format("%.1f MB", mb)
        } else {
            val kb = bytes.toDouble() / 1024
            String.format("%.1f KB", kb)
        }
    }

    /**
     * Parse video formats strictly adhering to the 144p-720p policy:
     * - ONLY show REAL available formats in the range: 144p, 240p, 360p, 480p, 720p.
     * - Maximum allowed video quality: 720p. NEVER offer 1080p, 1440p, 2160p / 4K.
     * - Minimum allowed video quality: 144p.
     * - Include HDR variants only if real HDR exists at that resolution.
     * - Accurately calculate/estimate real file size.
     */
    private fun parseVideoFormats(formats: List<VideoFormat>?, durationSecs: Int): List<VideoFormatInfo> {
        if (formats.isNullOrEmpty()) {
            return emptyList()
        }

        // Filter video formats strictly between 144p and 720p (inclusive)
        val videoStreams = formats.filter { fmt ->
            val hasVideo = (fmt.vcodec != null && fmt.vcodec != "none") || fmt.height > 0
            hasVideo && fmt.height in 144..720
        }

        if (videoStreams.isEmpty()) {
            return emptyList()
        }

        // Map height to standard display categories
        fun mapToStandardHeight(h: Int): Int {
            return when {
                h in 680..720 -> 720
                h in 440..500 -> 480
                h in 320..380 -> 360
                h in 200..260 -> 240
                h in 140..160 -> 144
                else -> h
            }
        }

        // Group by (StandardHeight, isHDR)
        val grouped = videoStreams.groupBy { fmt ->
            val stdH = mapToStandardHeight(fmt.height)
            val isHdr = (fmt.formatNote?.contains("HDR", ignoreCase = true) == true) ||
                        (fmt.vcodec?.contains("hdr", ignoreCase = true) == true) ||
                        (fmt.format?.contains("HDR", ignoreCase = true) == true)
            Pair(stdH, isHdr)
        }

        val result = mutableListOf<VideoFormatInfo>()

        // Sort descending by height, then standard first, HDR second
        val sortedKeys = grouped.keys.sortedWith(
            compareByDescending<Pair<Int, Boolean>> { it.first }.thenBy { it.second }
        )

        for ((height, isHdr) in sortedKeys) {
            val list = grouped[Pair(height, isHdr)] ?: continue
            val best = list.maxByOrNull {
                if (it.fileSize > 0) it.fileSize else it.fileSizeApproximate
            } ?: list.first()

            // Calculate real / accurately estimated size
            val sizeBytes = when {
                best.fileSize > 0 -> best.fileSize
                best.fileSizeApproximate > 0 -> best.fileSizeApproximate
                best.tbr > 0 && durationSecs > 0 -> ((best.tbr.toDouble() * 1000.0 / 8.0) * durationSecs).toLong()
                best.abr > 0 && durationSecs > 0 -> ((best.abr.toDouble() * 1000.0 / 8.0) * durationSecs).toLong()
                durationSecs > 0 -> {
                    // Standard bitrate estimate for resolution
                    val estKbps = when (height) {
                        720 -> 1800
                        480 -> 900
                        360 -> 500
                        240 -> 280
                        144 -> 150
                        else -> 700
                    }
                    (durationSecs.toLong() * estKbps * 1000L) / 8L
                }
                else -> 0L
            }

            val sizeStr = if (sizeBytes > 0) "~${formatFileSize(sizeBytes)}" else ""

            val resBase = when (height) {
                720 -> "720p (HD)"
                480 -> "480p (SD)"
                360 -> "360p (Low)"
                240 -> "240p (Economy)"
                144 -> "144p (Very Low)"
                else -> "${height}p"
            }

            val resLabel = if (isHdr) "$resBase HDR" else resBase
            val note = if (isHdr) "HDR High Dynamic Range" else (best.formatNote ?: if (height >= 720) "Recommended" else "Standard")
            val fmtId = best.formatId ?: "${height}p"

            result.add(
                VideoFormatInfo(
                    formatId = fmtId,
                    resolution = resLabel,
                    note = note,
                    ext = best.ext ?: "mp4",
                    filesizeApprox = sizeStr,
                    isAudio = false,
                    isHdr = isHdr
                )
            )
        }

        return result
    }

    /**
     * Audio formats: NO 144p-720p restriction.
     * Shows actual audio options derived from source stream or standard high-quality MP3 conversions.
     */
    private fun parseAudioFormats(formats: List<VideoFormat>?, durationSecs: Int): List<VideoFormatInfo> {
        val audioStreams = formats?.filter {
            (it.acodec != null && it.acodec != "none") || (it.vcodec == "none" && it.abr > 0)
        }

        if (formats != null && audioStreams.isNullOrEmpty() && formats.none { it.vcodec != "none" }) {
            return emptyList()
        }

        val standardBitrates = listOf(
            Triple("320kbps", "320 kbps (Ultra High Quality)", "Studio Master • MP3"),
            Triple("256kbps", "256 kbps (High Quality)", "Crystal Clear • MP3"),
            Triple("192kbps", "192 kbps (Standard Quality)", "Recommended • MP3"),
            Triple("128kbps", "128 kbps (Medium Quality)", "Data Saver • MP3"),
            Triple("64kbps", "64 kbps (Low Quality)", "Ultra Compact • MP3")
        )

        return standardBitrates.map { (bitrateKey, title, note) ->
            val kbps = bitrateKey.replace("kbps", "").toIntOrNull() ?: 192
            val approxBytes = if (durationSecs > 0) {
                (durationSecs.toLong() * kbps * 1000L) / 8L
            } else {
                (200L * kbps * 1000L) / 8L
            }
            VideoFormatInfo(
                formatId = bitrateKey,
                resolution = title,
                note = note,
                ext = "mp3",
                filesizeApprox = "~${formatFileSize(approxBytes)}",
                isAudio = true
            )
        }
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format("%02d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    companion object {
        private const val TAG = "DownloadEngine"

        @Volatile
        private var instance: DownloadEngine? = null

        fun getInstance(context: Context): DownloadEngine {
            return instance ?: synchronized(this) {
                val inst = DownloadEngine(context.applicationContext)
                instance = inst
                inst
            }
        }
    }
}
