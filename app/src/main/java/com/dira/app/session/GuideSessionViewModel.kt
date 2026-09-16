package com.dira.app.session

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dira.app.capture.SanitizeStub
import com.dira.app.capture.ScreenCaptureService
import com.dira.app.capture.SessionFrameBuffer
import com.dira.app.guide.GuideApiClient
import com.dira.app.guide.GuideClientFactory
import com.dira.app.guide.GuideRequest
import com.dira.app.guide.GuideStep
import com.dira.app.guide.MockGuideClient
import com.dira.app.modules.DemoModulePack
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class GuideUiState(
    val watching: Boolean = false,
    val loading: Boolean = false,
    val instruction: String = "",
    val pointX: Float = 0.72f,
    val pointY: Float = 0.38f,
    val question: String = "",
    val error: String? = null,
    val sessionCleared: Boolean = false,
    val timedOut: Boolean = false,
    val guideSource: String = if (GuideClientFactory.isMockMode()) "mock" else "api",
    val remainingMs: Long = SESSION_TIMEOUT_MS,
) {
    companion object {
        const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    }
}

/**
 * Orchestrates Help → capture → sanitize → guide → overlay target.
 * Session timeout 5 min + buffer wipe.
 */
class GuideSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val client: GuideApiClient = GuideClientFactory.create()

    private val _state = MutableStateFlow(GuideUiState())
    val state: StateFlow<GuideUiState> = _state.asStateFlow()

    private var timeoutJob: Job? = null
    private var tickerJob: Job? = null
    private var sessionStartedAt: Long = 0L

    fun onCaptureStarted(useSwahili: Boolean) {
        (client as? MockGuideClient)?.reset()
        SessionFrameBuffer.shared.clear()
        sessionStartedAt = System.currentTimeMillis()
        _state.value = GuideUiState(
            watching = true,
            instruction = if (useSwahili) {
                "Inatazama… uliza swali au bonyeza Pata hatua."
            } else {
                "Watching… ask a question or tap Guide step."
            },
            guideSource = if (GuideClientFactory.isMockMode()) "mock" else "api",
            remainingMs = GuideUiState.SESSION_TIMEOUT_MS,
        )
        startTimeoutWatch()
        // Auto first heuristic step shortly after capture starts (mock-friendly).
        viewModelScope.launch {
            delay(600)
            if (_state.value.watching) {
                requestStep(useSwahili = useSwahili, questionOverride = "")
            }
        }
    }

    fun onQuestionChange(value: String) {
        _state.update { it.copy(question = value) }
    }

    fun requestStep(useSwahili: Boolean, questionOverride: String? = null) {
        if (!_state.value.watching || _state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val frame = SessionFrameBuffer.shared.snapshot()
                var jpeg: ByteArray? = null
                var sanitizeNote: String? = null
                if (frame != null) {
                    val sanitized = SanitizeStub.sanitize(frame)
                    sanitizeNote = sanitized.note
                    jpeg = bitmapToJpeg(sanitized.bitmap)
                    if (!sanitized.bitmap.isRecycled) sanitized.bitmap.recycle()
                    // Drop pixels from buffer after sanitize+encode (privacy: minimize retention).
                    SessionFrameBuffer.shared.clear()
                }
                val response = client.requestGuidance(
                    GuideRequest(
                        question = questionOverride ?: _state.value.question,
                        moduleId = DemoModulePack.pack.id,
                        language = if (useSwahili) "sw" else "en",
                        imageJpeg = jpeg,
                        sanitizeNote = sanitizeNote,
                    ),
                )
                val step = response.step
                _state.update {
                    it.copy(
                        loading = false,
                        instruction = stepText(step, useSwahili),
                        pointX = step.pointXFraction.coerceIn(0f, 1f),
                        pointY = step.pointYFraction.coerceIn(0f, 1f),
                        guideSource = response.source,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Guide request failed",
                    )
                }
            }
        }
    }

    fun stopSession(showCleared: Boolean = true) {
        timeoutJob?.cancel()
        tickerJob?.cancel()
        timeoutJob = null
        tickerJob = null
        ScreenCaptureService.stop(getApplication())
        SessionFrameBuffer.shared.clear()
        SessionFrameBuffer.resetShared()
        (client as? MockGuideClient)?.reset()
        _state.value = GuideUiState(
            watching = false,
            sessionCleared = showCleared,
            timedOut = _state.value.timedOut,
            guideSource = if (GuideClientFactory.isMockMode()) "mock" else "api",
        )
    }

    fun consumeClearedFlag() {
        _state.update { it.copy(sessionCleared = false, timedOut = false) }
    }

    private fun startTimeoutWatch() {
        timeoutJob?.cancel()
        tickerJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(GuideUiState.SESSION_TIMEOUT_MS)
            _state.update { it.copy(timedOut = true) }
            stopSession(showCleared = true)
        }
        tickerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - sessionStartedAt
                val left = (GuideUiState.SESSION_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
                _state.update { it.copy(remainingMs = left) }
                if (left == 0L) break
                delay(1_000)
            }
        }
    }

    private fun stepText(step: GuideStep, useSwahili: Boolean): String =
        if (useSwahili) step.instructionSw else step.instructionEn

    private fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 70): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    override fun onCleared() {
        if (_state.value.watching) {
            ScreenCaptureService.stop(getApplication())
            SessionFrameBuffer.shared.clear()
        }
        super.onCleared()
    }
}
