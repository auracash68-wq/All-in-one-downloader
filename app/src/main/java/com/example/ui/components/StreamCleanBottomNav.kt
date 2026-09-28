package com.example.ui.components

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSurface
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

enum class StreamCleanTab(val label: String, val icon: ImageVector) {
    DOWNLOAD("Download", Icons.Outlined.FileDownload),
    DOWNLOADS("Downloads", Icons.Outlined.Folder),
    BROWSER("Browser", Icons.Outlined.Public),
    SETTINGS("Settings", Icons.Outlined.Settings)
}

/**
 * Calculates optimal blur radius based on device RAM to ensure 60fps performance
 * on 2GB/3GB entry-level devices.
 */
private fun getDeviceOptimalBlurRadius(context: Context): Dp {
    return try {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        if (totalRamGb < 4.0) 12.dp else 20.dp
    } catch (e: Exception) {
        12.dp
    }
}

/**
 * Floating Glassmorphism (Frosted Glass) Bottom Navigation Bar
 * with hardware-accelerated Haze effect, floating pill style, 1px light border,
 * scale bounce animation, and entry animation.
 */
@Composable
fun StreamCleanBottomNav(
    currentTab: StreamCleanTab,
    onTabSelected: (StreamCleanTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    val optimalBlurRadius = remember(context) { getDeviceOptimalBlurRadius(context) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cornerShape = RoundedCornerShape(24.dp)
    val glassBorderColor = Color.White.copy(alpha = 0.25f)
    val fallbackBackgroundColor = Color.White.copy(alpha = 0.65f)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 350)
        ) + fadeIn(animationSpec = tween(350)),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val hazeModifier = if (hazeState != null) {
            try {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        tint = HazeTint(Color.White.copy(alpha = 0.35f)),
                        blurRadius = optimalBlurRadius,
                        noiseFactor = 0.05f
                    )
                )
            } catch (e: Exception) {
                Modifier.background(fallbackBackgroundColor)
            }
        } else {
            Modifier.background(fallbackBackgroundColor)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = cornerShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(cornerShape)
                .then(hazeModifier)
                .background(Color.White.copy(alpha = 0.15f))
                .border(
                    width = 1.dp,
                    color = glassBorderColor,
                    shape = cornerShape
                )
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .padding(vertical = 6.dp, horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StreamCleanTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    val itemColor = if (isSelected) PrimaryGreen else TextSecondary.copy(alpha = 0.75f)

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.12f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                        label = "tab_scale_${tab.name}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 28.dp),
                                onClick = { onTabSelected(tab) }
                            )
                            .testTag("tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                // Subtle glow / pill effect beneath active icon
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            color = PrimaryGreen.copy(alpha = 0.12f),
                                            shape = CircleShape
                                        )
                                )
                            }
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = itemColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = itemColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
