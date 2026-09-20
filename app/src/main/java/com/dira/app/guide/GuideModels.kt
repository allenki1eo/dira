package com.dira.app.guide

/**
 * Guide request/response. Point targets are normalized fractions [0,1]
 * of the full screen (overlay / MediaProjection). Optional box is the
 * tight highlight around the same control.
 */
data class GuideRequest(
    val question: String,
    val moduleId: String,
    val language: String,
    /** Optional JPEG bytes of the current frame — omitted in pure mock heuristics. */
    val imageJpeg: ByteArray? = null,
    val sanitizeNote: String? = null,
    /** Optional accessibility node dump (read-only tree, not auto-tap). */
    val uiTree: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GuideRequest) return false
        return question == other.question &&
            moduleId == other.moduleId &&
            language == other.language &&
            sanitizeNote == other.sanitizeNote &&
            uiTree == other.uiTree &&
            (imageJpeg.contentEquals(other.imageJpeg))
    }

    override fun hashCode(): Int {
        var result = question.hashCode()
        result = 31 * result + moduleId.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + (sanitizeNote?.hashCode() ?: 0)
        result = 31 * result + (uiTree?.hashCode() ?: 0)
        result = 31 * result + (imageJpeg?.contentHashCode() ?: 0)
        return result
    }
}

data class GuideStep(
    val instructionEn: String,
    val instructionSw: String,
    val pointXFraction: Float,
    val pointYFraction: Float,
    val done: Boolean = false,
    val spokenEn: String = "",
    val spokenSw: String = "",
    val appGuess: String = "",
    val targetLabel: String = "",
    val boxXFraction: Float = Float.NaN,
    val boxYFraction: Float = Float.NaN,
    val boxWFraction: Float = Float.NaN,
    val boxHFraction: Float = Float.NaN,
    val confidence: Float = 0.55f,
) {
    fun instruction(useSwahili: Boolean): String =
        if (useSwahili) instructionSw else instructionEn

    fun spoken(useSwahili: Boolean): String {
        val spoken = if (useSwahili) spokenSw.ifBlank { instructionSw } else spokenEn.ifBlank { instructionEn }
        return spoken.ifBlank { instruction(useSwahili) }
    }

    fun chipText(useSwahili: Boolean): String {
        val body = instruction(useSwahili)
        val head = listOf(appGuess, targetLabel).filter { it.isNotBlank() }.joinToString(" · ")
        return if (head.isBlank()) body else "$head\n$body"
    }

    /** Tight highlight [x, y, w, h] in 0–1. Synthesizes a small box around the point if missing. */
    fun resolvedBox(): FloatArray {
        if (
            boxWFraction.isFinite() && boxWFraction >= 0.02f &&
            boxHFraction.isFinite() && boxHFraction >= 0.015f &&
            boxXFraction.isFinite() && boxYFraction.isFinite()
        ) {
            val x = boxXFraction.coerceIn(0f, 1f)
            val y = boxYFraction.coerceIn(0f, 1f)
            val w = boxWFraction.coerceIn(0.02f, (1f - x).coerceAtLeast(0.02f))
            val h = boxHFraction.coerceIn(0.015f, (1f - y).coerceAtLeast(0.015f))
            return floatArrayOf(x, y, w, h)
        }
        val w = 0.16f
        val h = 0.07f
        val x = (pointXFraction - w / 2f).coerceIn(0f, 1f - w)
        val y = (pointYFraction - h / 2f).coerceIn(0f, 1f - h)
        return floatArrayOf(x, y, w, h)
    }
}

data class GuideResponse(
    val step: GuideStep,
    val source: String,
)
