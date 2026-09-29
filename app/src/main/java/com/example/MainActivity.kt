package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.AppPremiumBackgroundCanvas
import com.example.ui.components.StreamCleanBottomNav
import com.example.ui.components.StreamCleanTab
import com.example.ui.localization.LocalAppStrings
import com.example.ui.localization.getAppStrings
import com.example.ui.screens.ChromeBrowserScreen
import com.example.ui.screens.DownloadHomeScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.LocalStreamCleanDark
import com.example.ui.theme.StreamCleanTheme
import com.example.ui.viewmodel.MainViewModel
import dev.chrisbanes.haze.HazeState

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val appearance by viewModel.appearance.collectAsState()
            val language by viewModel.language.collectAsState()

            val isDarkTheme = when (appearance) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            val appStrings = getAppStrings(language)

            CompositionLocalProvider(
                LocalAppStrings provides appStrings
            ) {
                StreamCleanTheme(darkTheme = isDarkTheme) {
                    StreamCleanMainApp(viewModel = viewModel)
                }
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
    val isDark = LocalStreamCleanDark.current
    val hazeState = remember { HazeState() }

    // Handle back button to return to home tab when in other tabs
    androidx.activity.compose.BackHandler(enabled = currentTab != StreamCleanTab.DOWNLOAD && playingVideo == null && socialUrl == null && !showingPrivateFiles) {
        viewModel.selectTab(StreamCleanTab.DOWNLOAD)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        AppPremiumBackgroundCanvas(isDark = isDark)

        // Show Social WebView if active
        if (socialUrl != null) {
            ChromeBrowserScreen(
                viewModel = viewModel,
                initialUrl = socialUrl,
                onBack = { viewModel.closeSocialWebView() },
                hazeState = hazeState,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Scaffold(
                bottomBar = {
                    // Hide bottom nav when video player is full screen
                    if (playingVideo == null) {
                        StreamCleanBottomNav(
                            currentTab = currentTab,
                            onTabSelected = { tab -> viewModel.selectTab(tab) },
                            hazeState = hazeState
                        )
                    }
                },
                containerColor = Color.Transparent
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "tab_crossfade_transition"
                    ) { tab ->
                        when (tab) {
                            StreamCleanTab.DOWNLOAD -> DownloadHomeScreen(
                                viewModel = viewModel,
                                hazeState = hazeState
                            )
                            StreamCleanTab.DOWNLOADS -> DownloadsScreen(
                                viewModel = viewModel,
                                hazeState = hazeState
                            )
                            StreamCleanTab.BROWSER -> ChromeBrowserScreen(
                                viewModel = viewModel,
                                hazeState = hazeState
                            )
                            StreamCleanTab.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                hazeState = hazeState
                            )
                        }
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
