package com.dira.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Phase 2 pointer overlay — position comes from guide response fractions.
 * Defaults exist only as a fallback before the first guide step arrives.
 */
@Composable
fun FakePointerOverlay(
    xFraction: Float = 0.72f,
    yFraction: Float = 0.38f,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = xFraction.coerceIn(0f, 1f)
        val y = yFraction.coerceIn(0f, 1f)
        val center = Offset(size.width * x, size.height * y)
        val accent = Color(0xFFE53935)
        drawCircle(color = accent.copy(alpha = 0.25f), radius = 56f, center = center)
        drawCircle(color = accent, radius = 28f, center = center, style = Stroke(width = 6f))
        drawCircle(color = accent, radius = 8f, center = center)
    }
}
