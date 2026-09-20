package com.dira.app.guide

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * HTTPS (or debug HTTP) client for guide-server.
 * POST {base}/v1/guide with JSON: question, moduleId, language, imageBase64?, sanitizeNote?
 * Expects JSON: instructionEn, instructionSw, pointX, pointY, done?
 *
 * The OpenRouter API key stays on the server — never in this APK.
 */
class HttpGuideClient(
    private val baseUrl: String,
) : GuideApiClient {

    override suspend fun requestGuidance(request: GuideRequest): GuideResponse =
        withContext(Dispatchers.IO) {
            val root = baseUrl.trimEnd('/')
            val url = URL("$root/v1/guide")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                // Vision models can be slow on the free tier.
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
                    request.imageJpeg?.let {
                        put("imageBase64", Base64.getEncoder().encodeToString(it))
                    }
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                if (code !in 200..299) {
                    error("Guide API HTTP $code: $text")
                }
                val json = JSONObject(text)
                GuideResponse(
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
