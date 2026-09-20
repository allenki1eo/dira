package com.dira.app.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Phase 2 pointer overlay — position comes from guide response fractions.
 * Optional box is a tight highlight around the same control.
 */
@Composable
fun FakePointerOverlay(
    xFraction: Float = 0.72f,
    yFraction: Float = 0.38f,
    boxX: Float = Float.NaN,
    boxY: Float = Float.NaN,
    boxW: Float = Float.NaN,
    boxH: Float = Float.NaN,
    label: String = "",
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val x = xFraction.coerceIn(0f, 1f)
        val y = yFraction.coerceIn(0f, 1f)
        val center = Offset(size.width * x, size.height * y)
        val accent = Color(0xFFE53935)
        val gold = Color(0xFFF4D35E)
        val hasBox = boxW.isFinite() && boxH.isFinite() && boxW >= 0.02f && boxH >= 0.015f &&
            boxX.isFinite() && boxY.isFinite()
        val left = if (hasBox) size.width * boxX.coerceIn(0f, 1f) else center.x - size.width * 0.08f
        val top = if (hasBox) size.height * boxY.coerceIn(0f, 1f) else center.y - size.height * 0.035f
        val width = if (hasBox) size.width * boxW else size.width * 0.16f
        val height = if (hasBox) size.height * boxH else size.height * 0.07f
        drawRoundRect(
            color = accent.copy(alpha = 0.18f),
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(16f, 16f),
        )
        drawRoundRect(
            color = accent,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4f),
        )
        val tick = 18f
        drawLine(gold, Offset(left, top), Offset(left + tick, top), strokeWidth = 6f)
        drawLine(gold, Offset(left, top), Offset(left, top + tick), strokeWidth = 6f)
        drawLine(gold, Offset(left + width, top), Offset(left + width - tick, top), strokeWidth = 6f)
        drawLine(gold, Offset(left + width, top), Offset(left + width, top + tick), strokeWidth = 6f)
        drawCircle(color = accent.copy(alpha = 0.25f), radius = 40f, center = center)
        drawCircle(color = accent, radius = 22f, center = center, style = Stroke(width = 5f))
        drawCircle(color = accent, radius = 7f, center = center)
        if (label.isNotBlank()) {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = 28f
            }
            drawContext.canvas.nativeCanvas.drawText(label, left + 8f, (top - 10f).coerceAtLeast(28f), paint)
        }
    }
}
