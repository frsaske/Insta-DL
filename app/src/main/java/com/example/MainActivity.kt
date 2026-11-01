package com.example

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.BottomWhiteToastHost
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.InstaDLTheme
import com.example.ui.theme.PureBlack

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            InstaDLTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkClipboardForInstagramLink()
    }

    private fun checkClipboardForInstagramLink() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0)?.text?.toString() ?: ""
                viewModel.onClipboardDetected(text)
            }
        } catch (_: Exception) { }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val detectedClipboardUrl by viewModel.detectedClipboardUrl.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val currentMedia by viewModel.currentMedia.collectAsStateWithLifecycle()
    val downloadedItemIds by viewModel.downloadedItemIds.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadCount by viewModel.downloadCount.collectAsStateWithLifecycle()
    val inAppToast by viewModel.inAppToast.collectAsStateWithLifecycle()

    // Handle back button: return to Home tab if on sub-screens
    BackHandler(enabled = activeTab != 0) {
        viewModel.selectTab(0)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack),
        containerColor = PureBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavBar(
                selectedTab = activeTab,
                downloadCount = downloadCount,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> HomeScreen(
                            urlInput = urlInput,
                            onUrlChanged = { viewModel.onUrlChanged(it) },
                            onClearUrl = { viewModel.clearUrl() },
                            detectedClipboardUrl = detectedClipboardUrl,
                            onApplyClipboard = { viewModel.applyClipboardUrl() },
                            onDismissClipboard = { viewModel.dismissClipboardPrompt() },
                            isLoading = isLoading,
                            statusMessage = statusMessage,
                            currentMedia = currentMedia,
                            downloadedItemIds = downloadedItemIds,
                            isDownloading = isDownloading,
                            downloadProgress = downloadProgress,
                            downloads = downloads,
                            onDownload = { viewModel.downloadAll() },
                            onDownloadSlide = { viewModel.downloadSlide(it) },
                            onNavigateToSettings = { viewModel.selectTab(2) },
                            onNavigateToDownloads = { viewModel.selectTab(1) }
                        )
                        1 -> DownloadsScreen(
                            downloads = downloads,
                            onDeleteDownload = { viewModel.deleteDownload(it) },
                            onNavigateHome = { viewModel.selectTab(0) }
                        )
                        2 -> SettingsScreen()
                    }
                }
            }

            // In-App Crisp Bottom White Toast Notification
            BottomWhiteToastHost(
                toastEvent = inAppToast,
                onDismiss = { viewModel.dismissToast() }
            )
        }
    }
}
