package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StreamCleanApplication
import com.example.data.local.DownloadEntity
import com.example.data.preferences.SettingsManager
import com.example.data.repository.DownloadRepository
import com.example.engine.ActiveDownloadState
import com.example.engine.DownloadEngine
import com.example.engine.VideoFormatInfo
import com.example.engine.VideoMetadata
import com.example.service.DownloadService
import com.example.ui.components.StreamCleanTab
import com.example.util.DeviceRamState
import com.example.util.DeviceStatusMonitor
import com.example.util.NetworkSpeedState
import com.example.util.StatusLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StreamCleanApplication
    private val repository: DownloadRepository = app.repository
    private val settingsManager: SettingsManager = app.settingsManager
    private val downloadEngine: DownloadEngine = DownloadEngine.getInstance(application)
    private val deviceStatusMonitor = DeviceStatusMonitor(application)

    private val _currentTab = MutableStateFlow(StreamCleanTab.DOWNLOAD)
    val currentTab: StateFlow<StreamCleanTab> = _currentTab.asStateFlow()

    // Real search / URL input state (starts empty in production)
    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _selectedFormatTab = MutableStateFlow("MP4 Video") // "MP4 Video" or "MP3 Audio"
    val selectedFormatTab: StateFlow<String> = _selectedFormatTab.asStateFlow()

    val activeDownload: StateFlow<ActiveDownloadState?> = downloadEngine.activeDownload

    private val _filterMediaType = MutableStateFlow("VIDEO") // "VIDEO" or "AUDIO"
    val filterMediaType: StateFlow<String> = _filterMediaType.asStateFlow()

    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(allDownloads, _filterMediaType) { list, filter ->
        list.filter { it.mediaType.equals(filter, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active video being played in in-app ExoPlayer
    private val _playingVideo = MutableStateFlow<DownloadEntity?>(null)
    val playingVideo: StateFlow<DownloadEntity?> = _playingVideo.asStateFlow()

    // Real video preview state (fetched from URL)
    private val _videoPreview = MutableStateFlow<VideoMetadata?>(null)
    val videoPreview: StateFlow<VideoMetadata?> = _videoPreview.asStateFlow()

    // Extracted URL matching current cached preview
    private var lastExtractedUrl: String = ""

    // Format selection dialog state
    private val _formatDialogMetadata = MutableStateFlow<VideoMetadata?>(null)
    val formatDialogMetadata: StateFlow<VideoMetadata?> = _formatDialogMetadata.asStateFlow()

    private val _isLoadingFormats = MutableStateFlow(false)
    val isLoadingFormats: StateFlow<Boolean> = _isLoadingFormats.asStateFlow()

    // Real-time RAM & Network Speed States (Features 1 & 2)
    private val _ramState = MutableStateFlow(DeviceRamState())
    val ramState: StateFlow<DeviceRamState> = _ramState.asStateFlow()

    private val _networkSpeedState = MutableStateFlow(NetworkSpeedState())
    val networkSpeedState: StateFlow<NetworkSpeedState> = _networkSpeedState.asStateFlow()

    // Low Memory Warning Dialog state (Feature 3)
    private val _showLowMemoryDialog = MutableStateFlow(false)
    val showLowMemoryDialog: StateFlow<Boolean> = _showLowMemoryDialog.asStateFlow()
    private var pendingFormatAfterMemoryWarning: VideoFormatInfo? = null

    // Multiple URL Download Flow (Features 4, 5, 6, 7, 8)
    private val _showMultipleUrlDialog = MutableStateFlow(false)
    val showMultipleUrlDialog: StateFlow<Boolean> = _showMultipleUrlDialog.asStateFlow()

    private val _multipleUrls = MutableStateFlow(listOf("", "")) // Minimum 2 URLs
    val multipleUrls: StateFlow<List<String>> = _multipleUrls.asStateFlow()

    private val _multipleFormatType = MutableStateFlow("MP4 Video") // "MP4 Video" or "MP3 Audio"
    val multipleFormatType: StateFlow<String> = _multipleFormatType.asStateFlow()

    private val _multipleQualityPreset = MutableStateFlow("HIGH") // "HIGH" (720p max / best audio) or "LOW" (144p-360p / standard audio)
    val multipleQualityPreset: StateFlow<String> = _multipleQualityPreset.asStateFlow()

    // Browser omnibox & navigation state
    private val _browserUrl = MutableStateFlow("https://www.google.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    // Social Media WebView state
    private val _socialUrl = MutableStateFlow<String?>(null)
    val socialUrl: StateFlow<String?> = _socialUrl.asStateFlow()

    fun openSocialUrl(url: String) {
        _socialUrl.value = url
    }

    fun closeSocialWebView() {
        _socialUrl.value = null
    }

    // Preferences
    val downloadLocation = settingsManager.downloadLocation
    val wifiOnly = settingsManager.wifiOnly
    val appearance = settingsManager.appearance
    val language = settingsManager.language
    val notificationsEnabled = settingsManager.notificationsEnabled

    init {
        // Lightweight periodic status sampler (every 2.5s) for RAM and Internet Throughput
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    _ramState.value = deviceStatusMonitor.getMemoryState()
                    _networkSpeedState.value = deviceStatusMonitor.sampleInternetSpeed()
                } catch (e: Exception) {
                    Log.w(TAG, "Error sampling device status: ${e.message}")
                }
                delay(2500)
            }
        }
    }

    fun selectTab(tab: StreamCleanTab) {
        _currentTab.value = tab
    }

    fun onUrlChanged(url: String) {
        _urlInput.value = url
        if (url != lastExtractedUrl) {
            _videoPreview.value = null
        }
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                _urlInput.value = text.trim()
                if (text.trim() != lastExtractedUrl) {
                    _videoPreview.value = null
                }
                Toast.makeText(context, "Link pasted!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
        }
    }

    fun selectFormatTab(type: String) {
        _selectedFormatTab.value = type
    }

    fun setFilterMediaType(type: String) {
        _filterMediaType.value = type
    }

    fun clearPreview() {
        _videoPreview.value = null
        lastExtractedUrl = ""
    }

    fun checkAndInitiateDownload(context: Context) {
        val rawUrl = _urlInput.value.trim()
        if (rawUrl.isEmpty()) {
            Toast.makeText(context, "Please enter or paste a video link", Toast.LENGTH_SHORT).show()
            return
        }

        // Sanitize URL before passing to YoutubeDL engine
        val sanitizedUrl = sanitizeUrl(rawUrl)
        Log.d(TAG, "Initiating video fetch. Raw: $rawUrl -> Sanitized: $sanitizedUrl")

        // Check Wi-Fi only restriction if enabled
        if (wifiOnly.value && !isConnectedToWifi(context)) {
            Toast.makeText(context, "Wi-Fi Only is enabled. Connect to Wi-Fi to download.", Toast.LENGTH_LONG).show()
            return
        }

        // Reuse already extracted metadata if safe to prevent duplicate network latency
        val currentPreview = _videoPreview.value
        if (currentPreview != null && sanitizedUrl == lastExtractedUrl) {
            handleMetadataResult(currentPreview, context)
            return
        }

        // Fetch format profiles for resolution/bitrate selection using real YoutubeDL engine
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingFormats.value = true
            try {
                val metadata = repository.fetchVideoInfo(sanitizedUrl)
                lastExtractedUrl = sanitizedUrl
                _videoPreview.value = metadata
                withContext(Dispatchers.Main) {
                    handleMetadataResult(metadata, context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch video info for URL: $sanitizedUrl", e)
                withContext(Dispatchers.Main) {
                    val msg = when {
                        e.message?.contains("Sign in to confirm", ignoreCase = true) == true ->
                            "This video requires authentication or token verification."
                        e.message?.contains("Video unavailable", ignoreCase = true) == true ->
                            "Video is unavailable or private."
                        else ->
                            e.localizedMessage ?: "Failed to extract video info. Please check the URL."
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } finally {
                _isLoadingFormats.value = false
            }
        }
    }

    private fun handleMetadataResult(metadata: VideoMetadata, context: Context) {
        val isAudio = _selectedFormatTab.value == "MP3 Audio"
        if (!isAudio) {
            if (metadata.videoFormats.isEmpty()) {
                // Safety policy requirement: Show clear safety message when no 144p-720p quality exists
                Toast.makeText(
                    context,
                    "Download unavailable for this video.\nFor your device safety, this video does not provide a supported quality between 144p and 720p.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        } else {
            if (metadata.audioFormats.isEmpty() && metadata.videoFormats.isEmpty()) {
                Toast.makeText(context, "No downloadable audio stream found for this link.", Toast.LENGTH_LONG).show()
                return
            }
        }
        _formatDialogMetadata.value = metadata
    }

    fun dismissFormatDialog() {
        _formatDialogMetadata.value = null
    }

    fun confirmDownload(format: VideoFormatInfo) {
        val metadata = _formatDialogMetadata.value ?: _videoPreview.value ?: return
        val rawUrl = _urlInput.value.trim()
        if (rawUrl.isEmpty()) return

        // Check memory pressure safety (Feature 3)
        if (_ramState.value.statusLevel == StatusLevel.RED && format.resolution.contains("720") && !format.isAudio) {
            pendingFormatAfterMemoryWarning = format
            _showLowMemoryDialog.value = true
            _formatDialogMetadata.value = null
            return
        }

        executeSingleDownload(format, metadata, rawUrl)
    }

    private fun executeSingleDownload(format: VideoFormatInfo, metadata: VideoMetadata, rawUrl: String) {
        val sanitizedUrl = sanitizeUrl(rawUrl)
        val isAudio = _selectedFormatTab.value == "MP3 Audio" || format.isAudio
        val mediaType = if (isAudio) "AUDIO" else "VIDEO"
        val extension = if (isAudio) "mp3" else "mp4"
        val title = metadata.title.ifEmpty { "StreamClean Download" }
        val fileName = "${title.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.$extension"

        _formatDialogMetadata.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val entity = DownloadEntity(
                url = sanitizedUrl,
                title = title,
                fileName = fileName,
                filePath = "",
                fileSizeBytes = 0L,
                formattedSize = format.filesizeApprox.ifEmpty { "Calculating..." },
                duration = metadata.duration,
                thumbnailUri = metadata.thumbnailUrl,
                mediaType = mediaType,
                resolution = format.resolution,
                status = "DOWNLOADING",
                progress = 0,
                relativeDate = "Today",
                timestamp = System.currentTimeMillis()
            )
            val id = repository.insertDownload(entity)

            // Start foreground service on main thread
            withContext(Dispatchers.Main) {
                DownloadService.start(getApplication())
            }

            // Enqueue to engine with real format ID and parameters
            downloadEngine.enqueueDownload(
                entityId = id,
                url = sanitizedUrl,
                title = title,
                mediaType = mediaType,
                resolution = format.resolution,
                formatId = format.formatId,
                audioBitrate = if (isAudio) format.formatId else "192kbps",
                thumbnailUrl = metadata.thumbnailUrl,
                duration = metadata.duration,
                queueIndex = 1,
                queueTotal = 1,
                repository = repository
            )
        }
    }

    // Memory Warning Dialog Actions (Feature 3)
    fun dismissLowMemoryDialog() {
        _showLowMemoryDialog.value = false
        pendingFormatAfterMemoryWarning = null
    }

    fun chooseLowerQualityFromWarning() {
        _showLowMemoryDialog.value = false
        // Reopen format dialog allowing user to choose lower quality (e.g. 360p / 240p / 144p)
        _videoPreview.value?.let { meta ->
            _formatDialogMetadata.value = meta
        }
    }

    fun continueDownloadDespiteMemoryWarning() {
        _showLowMemoryDialog.value = false
        val format = pendingFormatAfterMemoryWarning ?: return
        val metadata = _videoPreview.value ?: return
        val rawUrl = _urlInput.value.trim()
        if (rawUrl.isNotEmpty()) {
            executeSingleDownload(format, metadata, rawUrl)
        }
    }

    // Multiple URL Dialog Operations (Features 4, 5, 6, 7, 8)
    fun openMultipleUrlDialog() {
        if (_multipleUrls.value.size < 2) {
            _multipleUrls.value = listOf("", "")
        }
        _showMultipleUrlDialog.value = true
    }

    fun closeMultipleUrlDialog() {
        _showMultipleUrlDialog.value = false
    }

    fun addMultipleUrlRow() {
        val current = _multipleUrls.value
        if (current.size < 12) {
            _multipleUrls.value = current + ""
        }
    }

    fun removeMultipleUrlRow(index: Int) {
        val current = _multipleUrls.value.toMutableList()
        if (current.size > 2 && index in current.indices) {
            current.removeAt(index)
            _multipleUrls.value = current
        }
    }

    fun updateMultipleUrl(index: Int, value: String) {
        val current = _multipleUrls.value.toMutableList()
        if (index in current.indices) {
            current[index] = value
            _multipleUrls.value = current
        }
    }

    fun setMultipleFormatType(type: String) {
        _multipleFormatType.value = type
    }

    fun setMultipleQualityPreset(preset: String) {
        _multipleQualityPreset.value = preset
    }

    /**
     * Identifies indices of duplicate URLs (normalized comparison, Feature 6).
     */
    fun getDuplicateIndices(): Set<Int> {
        val list = _multipleUrls.value
        val duplicates = mutableSetOf<Int>()
        val seen = mutableMapOf<String, Int>()

        list.forEachIndexed { index, raw ->
            val normalized = sanitizeUrl(raw).trim().lowercase()
            if (normalized.isNotEmpty()) {
                if (seen.containsKey(normalized)) {
                    duplicates.add(seen[normalized]!!)
                    duplicates.add(index)
                } else {
                    seen[normalized] = index
                }
            }
        }
        return duplicates
    }

    fun confirmMultipleDownload(context: Context) {
        val urls = _multipleUrls.value.map { it.trim() }.filter { it.isNotEmpty() }
        if (urls.size < 2) {
            Toast.makeText(context, "Please enter at least 2 valid links", Toast.LENGTH_SHORT).show()
            return
        }

        // Check for duplicates
        val duplicateIndices = getDuplicateIndices()
        if (duplicateIndices.isNotEmpty()) {
            Toast.makeText(context, "Please remove duplicate links before continuing", Toast.LENGTH_LONG).show()
            return
        }

        if (wifiOnly.value && !isConnectedToWifi(context)) {
            Toast.makeText(context, "Wi-Fi Only is enabled. Connect to Wi-Fi to download.", Toast.LENGTH_LONG).show()
            return
        }

        _showMultipleUrlDialog.value = false
        val isAudio = _multipleFormatType.value == "MP3 Audio"
        val mediaType = if (isAudio) "AUDIO" else "VIDEO"
        val preset = _multipleQualityPreset.value
        val formatId = if (isAudio) {
            if (preset == "HIGH") "320kbps" else "128kbps"
        } else {
            if (preset == "HIGH") "HIGH" else "LOW"
        }
        val resolution = if (isAudio) {
            if (preset == "HIGH") "Audio (High Quality)" else "Audio (Standard Quality)"
        } else {
            if (preset == "HIGH") "720p (High Quality)" else "360p (Low Quality)"
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                DownloadService.start(getApplication())
                Toast.makeText(context, "Queued ${urls.size} downloads sequentially", Toast.LENGTH_SHORT).show()
            }

            urls.forEachIndexed { index, rawUrl ->
                val sanitizedUrl = sanitizeUrl(rawUrl)
                val safeTitle = "Download_${index + 1}"
                val ext = if (isAudio) "mp3" else "mp4"
                val fileName = "${safeTitle}_${System.currentTimeMillis() % 100000}.$ext"

                val entity = DownloadEntity(
                    url = sanitizedUrl,
                    title = "Queued link ${index + 1}",
                    fileName = fileName,
                    filePath = "",
                    fileSizeBytes = 0L,
                    formattedSize = "Queued",
                    duration = "",
                    thumbnailUri = null,
                    mediaType = mediaType,
                    resolution = resolution,
                    status = "DOWNLOADING",
                    progress = 0,
                    relativeDate = "Today",
                    timestamp = System.currentTimeMillis() + index
                )
                val id = repository.insertDownload(entity)

                downloadEngine.enqueueDownload(
                    entityId = id,
                    url = sanitizedUrl,
                    title = "Batch Download ${index + 1} of ${urls.size}",
                    mediaType = mediaType,
                    resolution = resolution,
                    formatId = formatId,
                    audioBitrate = if (isAudio) (if (preset == "HIGH") "320" else "128") else "192",
                    thumbnailUrl = null,
                    duration = "",
                    queueIndex = index + 1,
                    queueTotal = urls.size,
                    repository = repository
                )
            }
        }
    }

    fun cancelActiveDownload() {
        downloadEngine.cancelActiveDownload()
        DownloadService.stop(getApplication())
    }

    fun dismissActiveCard() {
        downloadEngine.dismissActiveCard()
    }

    fun deleteDownload(item: DownloadEntity) {
        viewModelScope.launch {
            repository.deleteDownload(item)
        }
    }

    fun exportToGallery(item: DownloadEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.exportToGallery(item)
            onResult(result)
        }
    }

    fun playVideo(item: DownloadEntity) {
        if (item.filePath.isNotEmpty()) {
            val file = java.io.File(item.filePath)
            if (!file.exists() || file.length() <= 0) {
                Toast.makeText(getApplication(), "Media file not found or empty on storage", Toast.LENGTH_SHORT).show()
                return
            }
        }
        _playingVideo.value = item
    }

    fun closePlayer() {
        _playingVideo.value = null
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
    }

    fun downloadFromBrowser(url: String) {
        _urlInput.value = url
        _videoPreview.value = null
        lastExtractedUrl = ""
        _currentTab.value = StreamCleanTab.DOWNLOAD
    }

    fun setWifiOnly(enabled: Boolean) {
        settingsManager.setWifiOnly(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsManager.setNotificationsEnabled(enabled)
    }

    fun setAppearance(theme: String) {
        settingsManager.setAppearance(theme)
    }

    fun setLanguage(lang: String) {
        settingsManager.setLanguage(lang)
    }

    fun setDownloadLocation(loc: String) {
        settingsManager.setDownloadLocation(loc)
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    companion object {
        private const val TAG = "DownloadViewModel"

        /**
         * Sanitizes URLs before passing to yt-dlp by stripping ONLY tracking
         * parameters like 'si', preserving video IDs, timestamps, playlists, etc.
         */
        fun sanitizeUrl(url: String): String {
            val trimmed = url.trim()
            if (trimmed.isEmpty()) return ""
            return try {
                val uri = Uri.parse(trimmed)
                if (uri.scheme == null || !uri.isHierarchical) {
                    return trimmed
                }
                val queryNames = uri.queryParameterNames
                if (!queryNames.contains("si")) {
                    return trimmed
                }
                val builder = uri.buildUpon().clearQuery()
                for (param in queryNames) {
                    if (param != "si") {
                        val values = uri.getQueryParameters(param)
                        for (v in values) {
                            builder.appendQueryParameter(param, v)
                        }
                    }
                }
                val cleaned = builder.build().toString()
                Log.d(TAG, "URL Sanitization: '$trimmed' -> '$cleaned'")
                cleaned
            } catch (e: Exception) {
                trimmed
            }
        }
    }
}
