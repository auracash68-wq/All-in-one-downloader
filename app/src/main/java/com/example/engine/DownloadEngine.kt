package com.example.engine

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
    val isAudio: Boolean = false
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
    val thumbnailUrl: String? = null
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

    suspend fun fetchFormats(url: String): VideoMetadata = withContext(Dispatchers.IO) {
        val sanitized = sanitizeUrlInput(url)

        // 1. Direct media links (.mp4, .mp3, etc.) - fast probing without yt-dlp
        if (isDirectMediaUrl(sanitized)) {
            val directMeta = probeDirectMediaUrl(sanitized)
            if (directMeta != null) {
                return@withContext directMeta
            }
        }

        // 2. Real extraction via YoutubeDL
        ensureEngineInitialized()
        Log.d(TAG, "Fetching video info using YoutubeDL for: $sanitized")

        val request = YoutubeDLRequest(sanitized).apply {
            addOption("--no-warnings")
            addOption("--no-update")
            addOption("--no-check-certificates")
        }

        val videoInfo: VideoInfo = try {
            YoutubeDL.getInstance().getInfo(request)
        } catch (e: Throwable) {
            Log.e(TAG, "YoutubeDL getInfo failed for $sanitized: ${e.message}")
            throw e
        }

        var title = videoInfo.title?.takeIf { it.isNotBlank() }
        var thumb = videoInfo.thumbnail?.takeIf { it.isNotBlank() }

        // If title or thumbnail are missing, try oEmbed as supplemental metadata only
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

        val videoFormats = parseVideoFormats(videoInfo.formats)
        val audioFormats = parseAudioFormats(videoInfo.formats, durationSecs)

        if (videoFormats.isEmpty() && audioFormats.isEmpty()) {
            throw YoutubeDLException("No downloadable video or audio streams found for URL: $sanitized")
        }

        Log.d(TAG, "Fetched real formats via YoutubeDL: $finalTitle, duration: $durationStr, video: ${videoFormats.size}, audio: ${audioFormats.size}")

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
                resolution = if (isAudio) "Direct Audio Stream" else "Direct Video Stream",
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

        _activeDownload.value = ActiveDownloadState(
            id = task.entityId,
            title = task.title,
            fileName = safeFileName,
            progress = 0,
            speed = "Starting...",
            eta = "",
            isRunning = true,
            thumbnailUrl = task.thumbnailUrl
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
                                val h = fmt.replace("p", "").trim()
                                "bestvideo[height<=$h]+bestaudio/best[height<=$h]/best"
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
                            speed = spd,
                            eta = if (etaInSeconds > 0) "${etaInSeconds}s" else ""
                        )
                    }
                }
            }

            if (isCancelled) {
                handleCancellation(task, downloadDir, safeFileName)
                return@withContext
            }

            // Identify the actual output file created (yt-dlp may merge or adjust extension)
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

                val updatedEntity = DownloadEntity(
                    id = task.entityId,
                    url = task.url,
                    title = task.title,
                    fileName = actualFile.name,
                    filePath = actualFile.absolutePath,
                    fileSizeBytes = fileSize,
                    formattedSize = formattedSize,
                    duration = task.duration,
                    thumbnailUri = task.thumbnailUrl,
                    mediaType = task.mediaType,
                    resolution = task.resolution,
                    status = "COMPLETED",
                    progress = 100,
                    relativeDate = "Today",
                    timestamp = System.currentTimeMillis()
                )
                task.repository.updateDownload(updatedEntity)
                Log.i(TAG, "Download finished successfully and verified: ${actualFile.name} ($formattedSize)")
            } else {
                Log.e(TAG, "Output validation failed for ${task.title}. Marking as FAILED.")
                if (actualFile.exists() && actualFile.length() == 0L) {
                    actualFile.delete()
                }
                markTaskFailed(task, safeFileName, "Validation failed: output media stream is invalid or empty")
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
            _activeDownload.value = null
            currentProcessId = null
        }
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
        Log.w(TAG, "Task marked as FAILED for ${task.title}: $errorReason")
    }

    private suspend fun handleCancellation(task: DownloadTask, downloadDir: File, safeFileName: String) {
        cleanupTempFiles(downloadDir, safeFileName)
        val file = File(downloadDir, safeFileName)
        if (file.exists()) file.delete()
        task.repository.deleteDownload(
            DownloadEntity(id = task.entityId, title = task.title, fileName = safeFileName)
        )
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

    private fun parseVideoFormats(formats: List<VideoFormat>?): List<VideoFormatInfo> {
        if (formats.isNullOrEmpty()) {
            return emptyList()
        }

        val videoFormats = formats.filter {
            (it.vcodec != null && it.vcodec != "none") || it.height > 0
        }

        if (videoFormats.isEmpty()) {
            return emptyList()
        }

        val groupedByHeight = videoFormats
            .filter { it.height > 0 }
            .groupBy { it.height }
            .toSortedMap(compareByDescending { it })

        val result = mutableListOf<VideoFormatInfo>()

        for ((height, list) in groupedByHeight) {
            val best = list.maxByOrNull {
                val s = if (it.fileSize > 0) it.fileSize else it.fileSizeApproximate
                s
            } ?: list.first()

            val sizeBytes = if (best.fileSize > 0) best.fileSize else best.fileSizeApproximate
            val sizeStr = if (sizeBytes > 0) "~${formatFileSize(sizeBytes)}" else ""

            val resLabel = when (height) {
                2160 -> "4K (2160p)"
                1440 -> "2K (1440p)"
                1080 -> "1080p (Full HD)"
                720 -> "720p (HD)"
                480 -> "480p (SD)"
                360 -> "360p (Low)"
                240 -> "240p (Economy)"
                144 -> "144p (Very Low)"
                else -> "${height}p"
            }

            val note = best.formatNote ?: if (height >= 720) "High Quality" else "Standard"
            val fmtId = best.formatId ?: "${height}p"

            result.add(
                VideoFormatInfo(
                    formatId = fmtId,
                    resolution = resLabel,
                    note = note,
                    ext = best.ext ?: "mp4",
                    filesizeApprox = sizeStr,
                    isAudio = false
                )
            )
        }

        return result
    }

    private fun parseAudioFormats(formats: List<VideoFormat>?, durationSecs: Int): List<VideoFormatInfo> {
        val audioStreams = formats?.filter {
            (it.acodec != null && it.acodec != "none") || (it.vcodec == "none" && it.abr > 0)
        }

        // If no audio streams found and no video formats found, audio cannot be extracted
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
