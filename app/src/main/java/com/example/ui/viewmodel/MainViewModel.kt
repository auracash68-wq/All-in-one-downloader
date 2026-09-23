package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StreamCleanApplication
    private val repository: DownloadRepository = app.repository
    private val settingsManager: SettingsManager = app.settingsManager
    private val downloadEngine: DownloadEngine = DownloadEngine.getInstance(application)

    private val _currentTab = MutableStateFlow(StreamCleanTab.DOWNLOAD)
    val currentTab: StateFlow<StreamCleanTab> = _currentTab.asStateFlow()

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

    fun checkAndInitiateDownload(context: Context) {
        val url = _urlInput.value.trim()
        if (url.isEmpty()) {
            Toast.makeText(context, "Please enter or paste a video link", Toast.LENGTH_SHORT).show()
            return
        }

        // Check Wi-Fi only restriction if enabled
        if (wifiOnly.value && !isConnectedToWifi(context)) {
            Toast.makeText(context, "Wi-Fi Only is enabled. Connect to Wi-Fi to download.", Toast.LENGTH_LONG).show()
            return
        }

        // Fetch format profiles for resolution/bitrate selection
        viewModelScope.launch {
            _isLoadingFormats.value = true
            try {
                val metadata = downloadEngine.fetchFormats(url)
                _formatDialogMetadata.value = metadata
            } catch (e: Exception) {
                // Fallback
                _formatDialogMetadata.value = VideoMetadata(
                    title = "StreamClean Media",
                    duration = "03:45",
                    thumbnailUrl = null,
                    formats = listOf(
                        VideoFormatInfo("720p", "720p (HD - Default)", "Standard quality", "mp4", "~35 MB"),
                        VideoFormatInfo("1080p", "1080p (Full HD)", "High quality", "mp4", "~70 MB")
                    )
                )
            } finally {
                _isLoadingFormats.value = false
            }
        }
    }

    fun dismissFormatDialog() {
        _formatDialogMetadata.value = null
    }

    fun confirmDownload(format: VideoFormatInfo) {
        val metadata = _formatDialogMetadata.value ?: return
        val url = _urlInput.value.trim().ifEmpty { "https://example.com/video" }
        val isAudio = _selectedFormatTab.value == "MP3 Audio"
        val mediaType = if (isAudio) "AUDIO" else "VIDEO"
        val extension = if (isAudio) "mp3" else "mp4"
        val title = metadata.title.ifEmpty { "StreamClean Download" }
        val fileName = "${title.replace(" ", "_")}.$extension"

        _formatDialogMetadata.value = null

        viewModelScope.launch {
            val entity = DownloadEntity(
                url = url,
                title = title,
                fileName = fileName,
                filePath = "",
                fileSizeBytes = 0L,
                formattedSize = format.filesizeApprox.ifEmpty { "42.8 MB" },
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

            // Start foreground service
            DownloadService.start(getApplication())

            // Enqueue to single-task engine
            downloadEngine.enqueueDownload(
                entityId = id,
                url = url,
                title = title,
                mediaType = mediaType,
                resolution = format.resolution,
                audioBitrate = "192kbps",
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
}
