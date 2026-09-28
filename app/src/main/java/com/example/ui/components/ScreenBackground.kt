package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * Reusable background wrapper that displays a background image/drawable
 * scaled to crop across the full screen dimensions, with foreground UI rendered on top.
 * Supports HazeState registration for backdrop glassmorphism / blur effects.
 */
@Composable
fun ScreenBackground(
    @DrawableRes backgroundResId: Int,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    content: @Composable () -> Unit
) {
    val boxModifier = if (hazeState != null) {
        modifier.fillMaxSize().hazeSource(state = hazeState)
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = boxModifier
    ) {
        Image(
            painter = painterResource(id = backgroundResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}
