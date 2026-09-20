package com.dira.app.guide

/**
 * Guide request/response. Point targets are normalized fractions [0,1]
 * of the full screen (overlay / MediaProjection).
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
)

data class GuideResponse(
    val step: GuideStep,
    val source: String,
)
