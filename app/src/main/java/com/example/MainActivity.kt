package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.components.StreamCleanBottomNav
import com.example.ui.components.StreamCleanTab
import com.example.ui.screens.ChromeBrowserScreen
import com.example.ui.screens.DownloadHomeScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.StreamCleanTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val appearance by viewModel.appearance.collectAsState()
            val isDarkTheme = when (appearance) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            StreamCleanTheme(darkTheme = isDarkTheme) {
                StreamCleanMainApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val tab = intent?.getStringExtra(EXTRA_NAVIGATE_TAB)
        if (tab == "DOWNLOADS") {
            viewModel.selectTab(StreamCleanTab.DOWNLOADS)
        }
    }

    companion object {
        const val EXTRA_NAVIGATE_TAB = "extra_navigate_tab"
    }
}

@Composable
fun StreamCleanMainApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val playingVideo by viewModel.playingVideo.collectAsState()
    val socialUrl by viewModel.socialUrl.collectAsState()
    val showingPrivateFiles by viewModel.showingPrivateFiles.collectAsState()

    // Handle back button to return to home tab when in other tabs
    androidx.activity.compose.BackHandler(enabled = currentTab != StreamCleanTab.DOWNLOAD && playingVideo == null && socialUrl == null && !showingPrivateFiles) {
        viewModel.selectTab(StreamCleanTab.DOWNLOAD)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Show Social WebView if active
        if (socialUrl != null) {
            ChromeBrowserScreen(
                viewModel = viewModel,
                initialUrl = socialUrl,
                onBack = { viewModel.closeSocialWebView() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Scaffold(
                bottomBar = {
                    // Hide bottom nav when video player is full screen
                    if (playingVideo == null) {
                        StreamCleanBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { tab -> viewModel.selectTab(tab) }
                        )
                    }
                },
                containerColor = AppBackground
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        StreamCleanTab.DOWNLOAD -> DownloadHomeScreen(viewModel = viewModel)
                        StreamCleanTab.DOWNLOADS -> DownloadsScreen(viewModel = viewModel)
                        StreamCleanTab.BROWSER -> ChromeBrowserScreen(viewModel = viewModel)
                        StreamCleanTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Fullscreen In-App Video Player Overlay
        playingVideo?.let { video ->
            VideoPlayerScreen(
                video = video,
                onBack = { viewModel.closePlayer() }
            )
        }
    }
}
