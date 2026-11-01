package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DownloadedMediaEntity
import com.example.data.InstaDownloadDatabase
import com.example.model.InstagramMediaMetadata
import com.example.model.MediaItemDetail
import com.example.util.InstaDownloadManager
import com.example.util.InstagramScraperEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InAppToastEvent(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = InstaDownloadDatabase.getDatabase(application).downloadedMediaDao()
    private val scraperEngine = InstagramScraperEngine(application)
    private val downloadManager = InstaDownloadManager(application)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    val detectedClipboardUrl: StateFlow<String?> = _detectedClipboardUrl.asStateFlow()

    private val _currentMedia = MutableStateFlow<InstagramMediaMetadata?>(null)
    val currentMedia: StateFlow<InstagramMediaMetadata?> = _currentMedia.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedItemIds: StateFlow<Set<String>> = _downloadedItemIds.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _inAppToast = MutableStateFlow<InAppToastEvent?>(null)
    val inAppToast: StateFlow<InAppToastEvent?> = _inAppToast.asStateFlow()

    val downloads: StateFlow<List<DownloadedMediaEntity>> = db.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadCount: StateFlow<Int> = db.getDownloadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            downloads.collect { list ->
                _downloadedItemIds.value = list.map { it.id }.toSet()
            }
        }
    }

    fun onUrlChanged(newUrl: String) {
        _urlInput.value = newUrl
        if (newUrl.isNotBlank() && isInstagramUrl(newUrl)) {
            fetchMedia(newUrl)
        }
    }

    fun onClipboardDetected(url: String) {
        if (isInstagramUrl(url) && url != _urlInput.value) {
            _detectedClipboardUrl.value = url
        }
    }

    fun dismissClipboardPrompt() {
        _detectedClipboardUrl.value = null
    }

    fun applyClipboardUrl() {
        _detectedClipboardUrl.value?.let {
            _urlInput.value = it
            _detectedClipboardUrl.value = null
            fetchMedia(it)
        }
    }

    fun clearUrl() {
        _urlInput.value = ""
        _currentMedia.value = null
        _statusMessage.value = null
    }

    fun selectTab(index: Int) {
        _activeTab.value = index
    }

    fun dismissToast() {
        _inAppToast.value = null
    }

    fun fetchMedia(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Fetching media..."
            val result = scraperEngine.fetchMedia(trimmed)
            _isLoading.value = false
            if (result.isSuccess) {
                _currentMedia.value = result.getOrNull()
                _statusMessage.value = null
                _urlInput.value = ""
            } else {
                _statusMessage.value = "Unable to fetch post. Please check the URL."
            }
        }
    }

    fun downloadSlide(item: MediaItemDetail) {
        val media = _currentMedia.value ?: return
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0.1f
            val res = downloadManager.downloadItem(media, item) { prog ->
                _downloadProgress.value = prog
            }
            _isDownloading.value = false
            if (res.isSuccess) {
                res.getOrNull()?.let {
                    _downloadedItemIds.value = _downloadedItemIds.value + it.id
                }
                val msg = if (item.isVideo) "Video saved successfully" else "Photo saved successfully"
                _inAppToast.value = InAppToastEvent(msg)
                _statusMessage.value = null
            } else {
                _statusMessage.value = "Download failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun downloadAll() {
        val media = _currentMedia.value ?: return
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0.15f
            val results = downloadManager.downloadAll(media) { current, total ->
                _downloadProgress.value = current.toFloat() / total
            }
            _isDownloading.value = false
            _downloadedItemIds.value = _downloadedItemIds.value + results.map { it.id }

            val msg = if (results.size > 1) {
                "${results.size} items saved successfully"
            } else if (media.mediaItems.firstOrNull()?.isVideo == true) {
                "Video saved successfully"
            } else {
                "Photo saved successfully"
            }
            _inAppToast.value = InAppToastEvent(msg)
            _statusMessage.value = null
        }
    }

    fun deleteDownload(entity: DownloadedMediaEntity) {
        viewModelScope.launch {
            try {
                val file = java.io.File(entity.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
                db.deleteDownload(entity)
                _downloadedItemIds.value = _downloadedItemIds.value - entity.id
            } catch (_: Exception) { }
        }
    }

    private fun isInstagramUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com/p/") ||
                lower.contains("instagram.com/reel/") ||
                lower.contains("instagram.com/reels/") ||
                lower.contains("instagram.com/tv/") ||
                lower.contains("instagr.am/")
    }
}
