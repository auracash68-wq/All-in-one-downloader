package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Reusable background wrapper that displays a background image/drawable
 * scaled to crop across the full screen dimensions, with foreground UI rendered on top.
 */
@Composable
fun ScreenBackground(
    @DrawableRes backgroundResId: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
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
