package com.dira.app.guide

import kotlinx.coroutines.delay

/**
 * Local/mock guide: generic Android UI heuristics (no TRA/bank/PEPMIS playbooks).
 * Used when GUIDE_API_BASE is empty or USE_MOCK_GUIDE=true.
 */
class MockGuideClient : GuideApiClient {

    private var stepIndex = 0

    override suspend fun requestGuidance(request: GuideRequest): GuideResponse {
        delay(280)
        val q = request.question.lowercase()

        val step = when {
            q.contains("done") || q.contains("maliza") || q.contains("finish") -> {
                stepIndex = 0
                GuideStep(
                    instructionEn = "You’re done. Tap Stop watching in Dira to clear this session.",
                    instructionSw = "Umemaliza. Bonyeza Acha kutazama katika Dira ili kufuta kikao.",
                    pointXFraction = 0.88f,
                    pointYFraction = 0.08f,
                    done = true,
                )
            }
            q.contains("back") || q.contains("rudi") || q.contains("home") -> {
                GuideStep(
                    instructionEn = "Use the system Back / gesture at the bottom of the screen to leave this page.",
                    instructionSw = "Tumia kitufe cha Rudi / ishara chini ya skrini kuondoka ukurasa huu.",
                    pointXFraction = 0.16f,
                    pointYFraction = 0.96f,
                )
            }
            q.contains("menu") || q.contains("more") || q.contains("settings") ||
                q.contains("mipangilio") || q.contains("menyu") -> {
                GuideStep(
                    instructionEn = "Tap the menu or overflow (⋮ / ☰) — usually top-right of the app bar.",
                    instructionSw = "Bonyeza menyu au overflow (⋮ / ☰) — mara nyingi juu-kulia ya upau wa programu.",
                    pointXFraction = 0.92f,
                    pointYFraction = 0.08f,
                )
            }
            q.contains("next") || q.contains("ifuata") || q.contains("continue") || q.contains("endelea") -> {
                stepIndex = (stepIndex + 1).coerceAtMost(2)
                stepForIndex(stepIndex)
            }
            stepIndex == 0 && q.isBlank() -> {
                stepIndex = 1
                stepForIndex(1)
            }
            else -> {
                if (stepIndex == 0) stepIndex = 1
                stepForIndex(stepIndex)
            }
        }

        return GuideResponse(step = step, source = "mock")
    }

    private fun stepForIndex(index: Int): GuideStep = when (index) {
        1 -> GuideStep(
            instructionEn = "Look at the current app. The next control is often a highlighted button toward the lower half of the screen.",
            instructionSw = "Angalia programu iliyo wazi. Kidhibiti kinachofuata mara nyingi ni kitufe kilichoangaziwa sehemu ya chini ya skrini.",
            pointXFraction = 0.50f,
            pointYFraction = 0.78f,
        )
        2 -> GuideStep(
            instructionEn = "If you see a primary action (Next, Continue, Save), tap that. Then ask again for the following step.",
            instructionSw = "Ukiiona hatua kuu (Ifuatayo, Endelea, Hifadhi), ibonyeze. Kisha uliza tena kwa hatua inayofuata.",
            pointXFraction = 0.50f,
            pointYFraction = 0.88f,
        )
        else -> GuideStep(
            instructionEn = "Ask what you want to do on this screen (for example “open settings” or “go back”).",
            instructionSw = "Uliza unachotaka kufanya kwenye skrini hii (k.m. “fungua mipangilio” au “rudi”).",
            pointXFraction = 0.50f,
            pointYFraction = 0.50f,
        )
    }

    fun reset() {
        stepIndex = 0
    }
}
