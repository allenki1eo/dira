package com.dira.app.session

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dira.app.BuildConfig
import com.dira.app.capture.SanitizeStub
import com.dira.app.capture.ScreenCaptureService
import com.dira.app.capture.SessionFrameBuffer
import com.dira.app.guide.GuideApiClient
import com.dira.app.guide.GuideClientFactory
import com.dira.app.guide.GuideRequest
import com.dira.app.guide.MockGuideClient
import com.dira.app.modules.DemoModulePack
import com.dira.app.overlay.CoachBus
import com.dira.app.overlay.OverlaySessionState
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
    val pointX: Float = 0.50f,
    val pointY: Float = 0.50f,
    val question: String = "",
    val error: String? = null,
    val sessionCleared: Boolean = false,
    val timedOut: Boolean = false,
    val guideSource: String = "mock",
    val guideApiBase: String = "",
    val remainingMs: Long = SESSION_TIMEOUT_MS,
    val overlayMode: Boolean = false,
    val boxX: Float = Float.NaN,
    val boxY: Float = Float.NaN,
    val boxW: Float = Float.NaN,
    val boxH: Float = Float.NaN,
    val targetLabel: String = "",
) {
    companion object {
        const val SESSION_TIMEOUT_MS = 5 * 60 * 1000L
    }
}

/**
 * Orchestrates Help → capture → downscale → guide → overlay target.
 * Session timeout 5 min + buffer wipe. Frames never leave RAM except the
 * in-flight POST to guide-server when a base URL is set.
 */
class GuideSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var client: GuideApiClient
    private val _state: MutableStateFlow<GuideUiState>
    val state: StateFlow<GuideUiState>

    private var timeoutJob: Job? = null
    private var tickerJob: Job? = null
    private var sessionStartedAt: Long = 0L

    init {
        val saved = prefs.getString(KEY_GUIDE_BASE, "") ?: ""
        val initialBase = saved.ifBlank { BuildConfig.GUIDE_API_BASE }
        client = GuideClientFactory.create(initialBase)
        _state = MutableStateFlow(
            GuideUiState(
                guideSource = if (GuideClientFactory.isMockMode(initialBase)) "mock" else "api",
                guideApiBase = GuideClientFactory.resolvedBase(initialBase),
            ),
        )
        state = _state.asStateFlow()
        viewModelScope.launch {
            var overlaySeenActive = false
            CoachBus.state.collect { overlay ->
                if (overlay.active) overlaySeenActive = true
                if (overlaySeenActive && !overlay.active && _state.value.overlayMode && _state.value.watching) {
                    overlaySeenActive = false
                    timeoutJob?.cancel()
                    tickerJob?.cancel()
                    timeoutJob = null
                    tickerJob = null
                    _state.value = GuideUiState(
                        watching = false,
                        sessionCleared = true,
                        overlayMode = false,
                        guideSource = if (GuideClientFactory.isMockMode(_state.value.guideApiBase)) "mock" else "api",
                        guideApiBase = _state.value.guideApiBase,
                    )
                } else if (_state.value.overlayMode && overlay.active) {
                    _state.update {
                        it.copy(
                            loading = overlay.loading,
                            instruction = overlay.instruction.ifBlank { it.instruction },
                            error = overlay.error,
                        )
                    }
                }
            }
        }
    }

    fun updateGuideBase(url: String) {
        if (_state.value.watching) return
        val trimmed = url.trim()
        prefs.edit().putString(KEY_GUIDE_BASE, trimmed).apply()
        client = GuideClientFactory.create(trimmed)
        _state.update {
            it.copy(
                guideApiBase = GuideClientFactory.resolvedBase(trimmed),
                guideSource = if (GuideClientFactory.isMockMode(trimmed)) "mock" else "api",
            )
        }
    }

    fun onCaptureStarted(useSwahili: Boolean, overlayMode: Boolean = false) {
        (client as? MockGuideClient)?.reset()
        SessionFrameBuffer.shared.clear()
        sessionStartedAt = System.currentTimeMillis()
        val mock = GuideClientFactory.isMockMode(_state.value.guideApiBase)
        _state.update {
            it.copy(
                watching = true,
                loading = false,
                error = null,
                sessionCleared = false,
                timedOut = false,
                overlayMode = overlayMode,
                instruction = if (overlayMode) {
                    if (useSwahili) {
                        "Kiputo cha Dira kiko juu ya programu zingine. Fungua programu, kisha bonyeza kiputo."
                    } else {
                        "The Dira bubble is over other apps. Open the app you need, then tap the bubble."
                    }
                } else if (useSwahili) {
                    if (mock) {
                        "Inatazama… uliza swali au bonyeza Pata hatua."
                    } else {
                        "Inatazama… fungua programu unayotaka msaada, kisha bonyeza Pata hatua."
                    }
                } else {
                    if (mock) {
                        "Watching… ask a question or tap Guide step."
                    } else {
                        "Watching… switch to the app you need help with, then tap Guide step."
                    }
                },
                remainingMs = GuideUiState.SESSION_TIMEOUT_MS,
            )
        }
        if (overlayMode) {
            CoachBus.publish(
                OverlaySessionState(
                    active = true,
                    instruction = _state.value.instruction,
                ),
            )
        }
        startTimeoutWatch()
        if (!overlayMode && mock) {
            viewModelScope.launch {
                delay(600)
                if (_state.value.watching) {
                    requestStep(useSwahili = useSwahili, questionOverride = "")
                }
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
                    // Drop pixels from buffer after encode (privacy: minimize retention).
                    SessionFrameBuffer.shared.clear()
                }
                val response = client.requestGuidance(
                    GuideRequest(
                        question = questionOverride ?: _state.value.question,
                        moduleId = DemoModulePack.pack.id,
                        language = if (useSwahili) "sw" else "en",
                        imageJpeg = jpeg,
                        sanitizeNote = sanitizeNote,
                        uiTree = com.dira.app.a11y.DiraTreeService.dumpForegroundTree(),
                    ),
                )
                val step = response.step
                val box = step.resolvedBox()
                _state.update {
                    it.copy(
                        loading = false,
                        instruction = step.chipText(useSwahili),
                        pointX = step.pointXFraction.coerceIn(0f, 1f),
                        pointY = step.pointYFraction.coerceIn(0f, 1f),
                        boxX = box[0],
                        boxY = box[1],
                        boxW = box[2],
                        boxH = box[3],
                        targetLabel = step.targetLabel,
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
        val base = _state.value.guideApiBase
        _state.value = GuideUiState(
            watching = false,
            sessionCleared = showCleared,
            timedOut = _state.value.timedOut,
            guideSource = if (GuideClientFactory.isMockMode(base)) "mock" else "api",
            guideApiBase = base,
            overlayMode = false,
        )
    }

    fun consumeClearedFlag() {
        _state.update { it.copy(sessionCleared = false, timedOut = false) }
    }

    /** Overlay permission arrived after an in-app session had already started. */
    fun promoteToOverlay(useSwahili: Boolean) {
        if (!_state.value.watching || _state.value.overlayMode) return
        val instruction = if (useSwahili) {
            "Kiputo cha Dira kiko juu ya programu zingine. Fungua programu, kisha bonyeza kiputo."
        } else {
            "The Dira bubble is over other apps. Open the app you need, then tap the bubble."
        }
        _state.update {
            it.copy(overlayMode = true, instruction = instruction, error = null)
        }
        CoachBus.publish(OverlaySessionState(active = true, instruction = instruction))
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

    private fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 82): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    override fun onCleared() {
        if (_state.value.watching && !_state.value.overlayMode) {
            ScreenCaptureService.stop(getApplication())
            SessionFrameBuffer.shared.clear()
        }
        super.onCleared()
    }

    companion object {
        private const val PREFS = "dira_guide"
        private const val KEY_GUIDE_BASE = "guide_api_base"
    }
}
