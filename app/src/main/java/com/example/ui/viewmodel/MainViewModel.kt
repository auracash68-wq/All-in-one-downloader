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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StreamCleanApplication
    private val repository: DownloadRepository = app.repository
    private val settingsManager: SettingsManager = app.settingsManager
    private val downloadEngine: DownloadEngine = DownloadEngine.getInstance(application)

    private val _currentTab = MutableStateFlow(StreamCleanTab.DOWNLOAD)
    val currentTab: StateFlow<StreamCleanTab> = _currentTab.asStateFlow()

    // Search / URL input state (pre-filled for emulator testing without ?si= parameter)
    private val _urlInput = MutableStateFlow("https://youtu.be/ZxEArqHRAFI")
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

    // Format selection dialog state
    private val _formatDialogMetadata = MutableStateFlow<VideoMetadata?>(null)
    val formatDialogMetadata: StateFlow<VideoMetadata?> = _formatDialogMetadata.asStateFlow()

    private val _isLoadingFormats = MutableStateFlow(false)
    val isLoadingFormats: StateFlow<Boolean> = _isLoadingFormats.asStateFlow()

    // Browser omnibox & navigation state
    private val _browserUrl = MutableStateFlow("https://www.google.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    // Preferences
    val downloadLocation = settingsManager.downloadLocation
    val wifiOnly = settingsManager.wifiOnly
    val appearance = settingsManager.appearance
    val language = settingsManager.language
    val notificationsEnabled = settingsManager.notificationsEnabled

    fun selectTab(tab: StreamCleanTab) {
        _currentTab.value = tab
    }

    fun onUrlChanged(url: String) {
        _urlInput.value = url
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotEmpty()) {
                _urlInput.value = text.trim()
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

        // Fetch format profiles for resolution/bitrate selection using real YoutubeDL engine
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingFormats.value = true
            try {
                val metadata = repository.fetchVideoInfo(sanitizedUrl)
                if (metadata.videoFormats.isEmpty() && metadata.audioFormats.isEmpty()) {
                    throw IllegalStateException("No compatible media streams found for this link")
                }
                _videoPreview.value = metadata
                _formatDialogMetadata.value = metadata
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

    fun dismissFormatDialog() {
        _formatDialogMetadata.value = null
    }

    fun confirmDownload(format: VideoFormatInfo) {
        val metadata = _formatDialogMetadata.value ?: _videoPreview.value ?: return
        val rawUrl = _urlInput.value.trim().ifEmpty { "https://youtu.be/ZxEArqHRAFI" }
        val sanitizedUrl = sanitizeUrl(rawUrl)
        val isAudio = _selectedFormatTab.value == "MP3 Audio"
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
                repository = repository
            )
        }
    }

    fun cancelActiveDownload() {
        downloadEngine.cancelActiveDownload()
        DownloadService.stop(getApplication())
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
         * Sanitizes URLs before passing to yt-dlp by stripping ONLY the specific tracking
         * parameter 'si' if present, preserving video IDs, timestamps, playlists, and all other parameters.
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
