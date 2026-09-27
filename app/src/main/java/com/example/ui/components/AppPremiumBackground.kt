package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Premium Minimal Abstract Organic Wave + Soft Green Gradient Background System.
 *
 * Implements a unified 6-layer lightweight vector background:
 * - Layer 1: Clean soft mint-tinted base
 * - Layer 2: Subtle multi-point radial and linear lighting glows
 * - Layer 3: Smooth organic flowing wave / ribbon shapes with translucent gradient fills
 * - Layer 4: Delicate curved accent strokes & harmonic bezier ribbons
 * - Layer 5: Subtle geometric dotted accents in calm negative-space regions
 * - Layer 6: Content placed strictly on top
 */
@Composable
fun AppPremiumBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AppPremiumBackgroundCanvas(
            modifier = Modifier.fillMaxSize(),
            isDark = isDark
        )
        content()
    }
}

@Composable
fun AppPremiumBackgroundCanvas(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        if (w <= 0f || h <= 0f) return@Canvas

        // ==========================================
        // LAYER 1: Base Color
        // ==========================================
        val baseColor = if (isDark) Color(0xFF121714) else Color(0xFFF6FAF7)
        drawRect(color = baseColor)

        // ==========================================
        // LAYER 2: Soft Multi-Point Radial & Linear Glows
        // ==========================================
        if (!isDark) {
            // Top-right ethereal emerald glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFD1FAE5).copy(alpha = 0.60f),
                        Color(0xFFE6F7ED).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.90f, h * 0.08f),
                    radius = w * 0.85f
                )
            )

            // Bottom-left soft mint glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFA7F3D0).copy(alpha = 0.35f),
                        Color(0xFFECFDF5).copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.10f, h * 0.82f),
                    radius = w * 0.75f
                )
            )

            // Center-right subtle ambient light
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE0F2E9).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.70f, h * 0.50f),
                    radius = w * 0.60f
                )
            )
        } else {
            // Dark mode ambient emerald glows
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF064E3B).copy(alpha = 0.25f),
                        Color(0xFF022C22).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.10f),
                    radius = w * 0.90f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF047857).copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.15f, h * 0.80f),
                    radius = w * 0.70f
                )
            )
        }

        // ==========================================
        // LAYER 3: Flowing Organic Wave Shapes (Layered Fills)
        // ==========================================

        // --- Wave Shape 1: Top-to-Mid Flowing Wave ---
        val wave1Path = Path().apply {
            moveTo(0f, h * 0.05f)
            cubicTo(
                w * 0.25f, h * 0.02f,
                w * 0.55f, h * 0.18f,
                w * 0.75f, h * 0.13f
            )
            cubicTo(
                w * 0.88f, h * 0.10f,
                w * 0.96f, h * 0.16f,
                w, h * 0.22f
            )
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }

        val wave1Brush = if (!isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF34D399).copy(alpha = 0.07f),
                    Color(0xFF10B981).copy(alpha = 0.035f),
                    Color(0xFF059669).copy(alpha = 0.015f)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h * 0.25f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = 0.05f),
                    Color(0xFF059669).copy(alpha = 0.02f),
                    Color.Transparent
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h * 0.25f)
            )
        }
        drawPath(path = wave1Path, brush = wave1Brush)

        // --- Wave Shape 2: Organic Mid-Body Ribbon ---
        val wave2Path = Path().apply {
            moveTo(0f, h * 0.38f)
            cubicTo(
                w * 0.20f, h * 0.32f,
                w * 0.40f, h * 0.48f,
                w * 0.70f, h * 0.42f
            )
            cubicTo(
                w * 0.85f, h * 0.39f,
                w * 0.95f, h * 0.45f,
                w, h * 0.52f
            )
            lineTo(w, h * 0.65f)
            cubicTo(
                w * 0.80f, h * 0.60f,
                w * 0.50f, h * 0.68f,
                w * 0.25f, h * 0.55f
            )
            cubicTo(
                w * 0.10f, h * 0.48f,
                w * 0.04f, h * 0.52f,
                0f, h * 0.54f
            )
            close()
        }

        val wave2Brush = if (!isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = 0.030f),
                    Color(0xFF059669).copy(alpha = 0.045f),
                    Color(0xFF34D399).copy(alpha = 0.020f)
                ),
                start = Offset(0f, h * 0.35f),
                end = Offset(w, h * 0.65f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = 0.035f),
                    Color(0xFF047857).copy(alpha = 0.020f),
                    Color.Transparent
                ),
                start = Offset(0f, h * 0.35f),
                end = Offset(w, h * 0.65f)
            )
        }
        drawPath(path = wave2Path, brush = wave2Brush)

        // --- Wave Shape 3: Bottom Elegant Swell ---
        val wave3Path = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(
                w * 0.30f, h * 0.70f,
                w * 0.60f, h * 0.86f,
                w, h * 0.74f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        val wave3Brush = if (!isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF059669).copy(alpha = 0.040f),
                    Color(0xFFA7F3D0).copy(alpha = 0.060f),
                    Color(0xFF34D399).copy(alpha = 0.025f)
                ),
                start = Offset(0f, h * 0.70f),
                end = Offset(w, h)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF065F46).copy(alpha = 0.05f),
                    Color(0xFF047857).copy(alpha = 0.02f),
                    Color.Transparent
                ),
                start = Offset(0f, h * 0.70f),
                end = Offset(w, h)
            )
        }
        drawPath(path = wave3Path, brush = wave3Brush)

        // ==========================================
        // LAYER 4: Delicate Translucent Curved Accent Lines
        // ==========================================

        // Upper subtle curved contour line
        val line1Path = Path().apply {
            moveTo(-w * 0.05f, h * 0.08f)
            cubicTo(
                w * 0.28f, h * 0.03f,
                w * 0.58f, h * 0.20f,
                w * 0.78f, h * 0.15f
            )
            cubicTo(
                w * 0.90f, h * 0.12f,
                w * 0.98f, h * 0.18f,
                w * 1.05f, h * 0.24f
            )
        }
        val line1Color = if (!isDark) Color(0xFF10B981).copy(alpha = 0.16f) else Color(0xFF34D399).copy(alpha = 0.14f)
        drawPath(
            path = line1Path,
            color = line1Color,
            style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Harmonic secondary contour line (offset)
        val line2Path = Path().apply {
            moveTo(0f, h * 0.11f)
            cubicTo(
                w * 0.32f, h * 0.07f,
                w * 0.62f, h * 0.23f,
                w * 0.82f, h * 0.19f
            )
            cubicTo(
                w * 0.92f, h * 0.16f,
                w * 0.98f, h * 0.22f,
                w * 1.05f, h * 0.28f
            )
        }
        val line2Color = if (!isDark) Color(0xFF059669).copy(alpha = 0.09f) else Color(0xFF10B981).copy(alpha = 0.08f)
        drawPath(
            path = line2Path,
            color = line2Color,
            style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
        )

        // Lower dynamic flowing ribbon line
        val line3Path = Path().apply {
            moveTo(-w * 0.05f, h * 0.76f)
            cubicTo(
                w * 0.28f, h * 0.68f,
                w * 0.58f, h * 0.84f,
                w * 0.88f, h * 0.72f
            )
            cubicTo(
                w * 0.96f, h * 0.68f,
                w * 1.02f, h * 0.74f,
                w * 1.08f, h * 0.78f
            )
        }
        val line3Color = if (!isDark) Color(0xFF10B981).copy(alpha = 0.14f) else Color(0xFF34D399).copy(alpha = 0.12f)
        drawPath(
            path = line3Path,
            color = line3Color,
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )

        // ==========================================
        // LAYER 5: Extremely Subtle Dotted Decorative Matrix
        // ==========================================
        val dotColor = if (!isDark) Color(0xFF059669).copy(alpha = 0.085f) else Color(0xFF34D399).copy(alpha = 0.075f)
        val dotRadius = 1.4.dp.toPx()
        val spacing = 11.dp.toPx()

        // Cluster 1: Upper right negative space (5 x 4 dots)
        val startX1 = w * 0.78f
        val startY1 = h * 0.045f
        for (col in 0 until 5) {
            for (row in 0 until 4) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(startX1 + col * spacing, startY1 + row * spacing)
                )
            }
        }

        // Cluster 2: Lower left negative space (4 x 3 dots)
        val startX2 = w * 0.07f
        val startY2 = h * 0.83f
        val dotColorSecondary = if (!isDark) Color(0xFF10B981).copy(alpha = 0.065f) else Color(0xFF059669).copy(alpha = 0.055f)
        for (col in 0 until 4) {
            for (row in 0 until 3) {
                drawCircle(
                    color = dotColorSecondary,
                    radius = dotRadius,
                    center = Offset(startX2 + col * spacing, startY2 + row * spacing)
                )
            }
        }
    }
}
