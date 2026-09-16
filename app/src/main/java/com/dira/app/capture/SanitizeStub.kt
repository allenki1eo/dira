package com.dira.app.capture

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * Phase 2 sanitize stub: crop a center region and paint placeholder redaction bars.
 * No real ML — stands in for on-device redact before any off-device send.
 * Output stays in memory; caller must recycle when done.
 */
object SanitizeStub {

    data class SanitizedFrame(
        val bitmap: Bitmap,
        val note: String = "stub-crop+redact",
    )

    fun sanitize(source: Bitmap): SanitizedFrame {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        // Prefer a cropped region (data minimization) — center 70% width / 60% height.
        val cropW = (w * 0.70f).toInt().coerceAtLeast(1)
        val cropH = (h * 0.60f).toInt().coerceAtLeast(1)
        val left = ((w - cropW) / 2).coerceAtLeast(0)
        val top = ((h - cropH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(source, left, top, cropW, cropH)

        val mutable = cropped.copy(Bitmap.Config.ARGB_8888, true)
        if (cropped !== source && cropped !== mutable) {
            cropped.recycle()
        }

        val canvas = Canvas(mutable)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        // Placeholder redaction bars (IDs / balances / OTPs later).
        val barH = (mutable.height * 0.08f).toInt().coerceAtLeast(8)
        canvas.drawRect(
            Rect(0, (mutable.height * 0.12f).toInt(), mutable.width, (mutable.height * 0.12f).toInt() + barH),
            paint,
        )
        canvas.drawRect(
            Rect(
                (mutable.width * 0.55f).toInt(),
                (mutable.height * 0.72f).toInt(),
                mutable.width,
                (mutable.height * 0.72f).toInt() + barH,
            ),
            paint,
        )
        return SanitizedFrame(mutable)
    }
}
