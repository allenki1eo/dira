package com.dira.app.overlay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.dira.app.a11y.DiraTreeService
import com.dira.app.capture.SanitizeStub
import com.dira.app.capture.ScreenCaptureService
import com.dira.app.capture.SessionFrameBuffer
import com.dira.app.guide.GuideClientFactory
import com.dira.app.guide.GuideRequest
import com.dira.app.modules.DemoModulePack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Overlay session owner: bubble + voice/type + pointer over other apps.
 * Lives with [ScreenCaptureService] so it survives leaving Dira.
 */
class CoachCoordinator(
    private val context: Context,
    private val useSwahili: Boolean,
    guideBase: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client = GuideClientFactory.create(guideBase)
    private val overlay = CoachOverlay(context, object : CoachOverlay.Callbacks {
        override fun onGuide(question: String) = requestGuide(question)
        override fun onStop() = stop()
        override fun onMic() = startVoice()
    })
    private var recognizer: SpeechRecognizer? = null
    private var timeoutJob: Job? = null
    private var active = true

    fun start() {
        overlay.attach()
        CoachBus.publish(
            OverlaySessionState(
                active = true,
                instruction = context.getString(
                    if (useSwahili) com.dira.app.R.string.overlay_ready_sw else com.dira.app.R.string.overlay_ready,
                ),
            ),
        )
        overlay.setInstruction(
            context.getString(
                if (useSwahili) com.dira.app.R.string.overlay_ready_sw else com.dira.app.R.string.overlay_ready,
            ),
        )
        timeoutJob = scope.launch {
            delay(SESSION_MS)
            Toast.makeText(context, context.getString(com.dira.app.R.string.session_timed_out), Toast.LENGTH_LONG).show()
            stop()
        }
    }

    fun destroy() {
        active = false
        timeoutJob?.cancel()
        recognizer?.destroy()
        recognizer = null
        overlay.destroy()
        CoachBus.clear()
    }

    private fun requestGuide(question: String) {
        if (!active) return
        scope.launch {
            overlay.setLoading(true)
            CoachBus.publish(CoachBus.state.value.copy(loading = true, error = null, active = true))
            overlay.hideChromeForCapture()
            delay(220)
            try {
                val frame = SessionFrameBuffer.shared.snapshot()
                var jpeg: ByteArray? = null
                var note: String? = null
                if (frame != null) {
                    val sanitized = SanitizeStub.sanitize(frame)
                    note = sanitized.note
                    jpeg = jpegBytes(sanitized.bitmap)
                    if (!sanitized.bitmap.isRecycled) sanitized.bitmap.recycle()
                    SessionFrameBuffer.shared.clear()
                }
                val tree = DiraTreeService.dumpForegroundTree()
                val response = client.requestGuidance(
                    GuideRequest(
                        question = question,
                        moduleId = DemoModulePack.pack.id,
                        language = if (useSwahili) "sw" else "en",
                        imageJpeg = jpeg,
                        sanitizeNote = note,
                        uiTree = tree,
                    ),
                )
                val step = response.step
                val text = if (useSwahili) step.instructionSw else step.instructionEn
                overlay.restoreChrome(showAsk = false)
                overlay.setLoading(false)
                overlay.setInstruction(text)
                overlay.showPointer(step.pointXFraction, step.pointYFraction)
                CoachBus.publish(
                    OverlaySessionState(
                        active = true,
                        loading = false,
                        instruction = text,
                    ),
                )
            } catch (e: Exception) {
                val msg = e.message ?: "Guide request failed"
                overlay.restoreChrome(showAsk = true)
                overlay.setLoading(false)
                overlay.setError(msg)
                CoachBus.publish(
                    OverlaySessionState(active = true, loading = false, error = msg),
                )
            }
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            overlay.setError(context.getString(com.dira.app.R.string.overlay_mic_needed))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            overlay.setError(context.getString(com.dira.app.R.string.overlay_voice_unavailable))
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(voiceListener())
            }
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                if (useSwahili) "sw-TZ" else Locale.getDefault().toLanguageTag(),
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        overlay.setListening(true)
        CoachBus.publish(CoachBus.state.value.copy(listening = true, active = true))
        recognizer?.startListening(intent)
    }

    private fun voiceListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            overlay.setListening(false)
        }
        override fun onError(error: Int) {
            overlay.setListening(false)
            CoachBus.publish(CoachBus.state.value.copy(listening = false, active = true))
            overlay.setError(context.getString(com.dira.app.R.string.overlay_voice_failed))
        }
        override fun onResults(results: Bundle?) {
            overlay.setListening(false)
            val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                overlay.setQuestion(spoken)
                requestGuide(spoken)
            }
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val spoken = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) overlay.setQuestion(spoken)
        }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun stop() {
        if (!active) return
        active = false
        Handler(Looper.getMainLooper()).post {
            destroy()
            ScreenCaptureService.stop(context)
        }
    }

    private fun jpegBytes(bitmap: Bitmap, quality: Int = 70): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    companion object {
        const val SESSION_MS = 5 * 60 * 1000L
    }
}
