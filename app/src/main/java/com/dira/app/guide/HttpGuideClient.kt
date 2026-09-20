package com.dira.app.guide

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * HTTPS (or debug HTTP) client for guide-server.
 * The OpenRouter API key stays on the server — never in this APK.
 */
class HttpGuideClient(
    private val baseUrl: String,
) : GuideApiClient {

    override suspend fun requestGuidance(request: GuideRequest): GuideResponse =
        withContext(Dispatchers.IO) {
            var lastError: Exception? = null
            repeat(2) { attempt ->
                try {
                    return@withContext postOnce(request)
                } catch (e: Exception) {
                    lastError = e
                    val retryable = e.message?.contains("HTTP 502") == true ||
                        e.message?.contains("HTTP 503") == true ||
                        e.message?.contains("HTTP 504") == true
                    if (!retryable || attempt == 1) throw e
                    delay(1_800)
                }
            }
            throw lastError ?: error("Guide request failed")
        }

    private fun postOnce(request: GuideRequest): GuideResponse {
        val root = baseUrl.trimEnd('/')
        val url = URL("$root/v1/guide")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = JSONObject().apply {
                put("question", request.question)
                put("moduleId", request.moduleId)
                put("language", request.language)
                request.sanitizeNote?.let { put("sanitizeNote", it) }
                request.uiTree?.let { put("uiTree", it) }
                request.imageJpeg?.let {
                    put("imageBase64", Base64.getEncoder().encodeToString(it))
                }
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                error(friendlyHttpError(code, text))
            }
            val json = JSONObject(text)
            return GuideResponse(
                step = GuideStep(
                    instructionEn = json.optString("instructionEn", json.optString("instruction", "")),
                    instructionSw = json.optString("instructionSw", json.optString("instructionEn", "")),
                    pointXFraction = json.optDouble("pointX", 0.5).toFloat().coerceIn(0f, 1f),
                    pointYFraction = json.optDouble("pointY", 0.5).toFloat().coerceIn(0f, 1f),
                    done = json.optBoolean("done", false),
                ),
                source = "api",
            )
        } finally {
            conn.disconnect()
        }
    }
}

internal fun friendlyHttpError(code: Int, raw: String): String {
    val parsed = raw.trim().let { body ->
        if (body.startsWith("{")) {
            runCatching { JSONObject(body) }.getOrNull()
        } else {
            null
        }
    }
    val title = parsed?.optString("title").orEmpty()
    val detail = parsed?.optString("detail").orEmpty()
    val cloudflare = title.contains("Cloudflare", ignoreCase = true) ||
        detail.contains("Cloudflare", ignoreCase = true) ||
        raw.contains("trycloudflare", ignoreCase = true)
    return when {
        code == 502 && cloudflare ->
            "Guide tunnel is down (Cloudflare 502). Wait a minute, check the tunnel, then tap the bubble again."
        code == 502 ->
            "Guide server returned 502 (bad gateway). Wait a minute and try again."
        code == 503 || code == 504 ->
            "Guide server is busy ($code). Wait a moment and try again."
        code == 413 ->
            "That screenshot was too large. Try again."
        else -> {
            val short = (parsed?.optString("error").takeUnless { it.isNullOrBlank() }
                ?: detail.takeIf { it.isNotBlank() }
                ?: title.takeIf { it.isNotBlank() }
                ?: raw.replace("\n", " ").take(160))
            "Guide API HTTP $code: $short"
        }
    }
}
