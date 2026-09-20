package com.dira.app.capture

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Shrink the in-memory session frame before it leaves the device.
 * Full-screen composition is kept so pointX/pointY still map to the capture.
 * No long-term storage — caller must recycle the result.
 */
object SanitizeStub {

    data class SanitizedFrame(
        val bitmap: Bitmap,
        val note: String = "downscale-only",
    )

    private const val MAX_EDGE = 1080

    fun sanitize(source: Bitmap): SanitizedFrame {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val scale = min(1f, MAX_EDGE.toFloat() / max(w, h).toFloat())
        if (scale >= 0.99f) {
            val copy = source.copy(Bitmap.Config.ARGB_8888, false) ?: source
            return SanitizedFrame(copy, note = "full-frame")
        }
        val outW = (w * scale).toInt().coerceAtLeast(1)
        val outH = (h * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, outW, outH, true)
        return SanitizedFrame(scaled, note = "downscale-${outW}x${outH}")
    }
}
