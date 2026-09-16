package com.dira.app.guide

import kotlinx.coroutines.delay

/**
 * Local/mock guide: heuristics from question text + demo module layout.
 * Works with empty GUIDE_API_BASE or USE_MOCK_GUIDE=true.
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
                    instructionEn = "You’re done for this demo. Tap Stop watching to clear the session.",
                    instructionSw = "Umemaliza onyesho. Bonyeza Acha kutazama ili kufuta kikao.",
                    pointXFraction = 0.88f,
                    pointYFraction = 0.08f,
                    done = true,
                )
            }
            q.contains("submit") || q.contains("wasilisha") || q.contains("send") -> {
                stepIndex = 2
                GuideStep(
                    instructionEn = "Tap the Submit control on the right side of the demo panel.",
                    instructionSw = "Bonyeza kidhibiti Wasilisha upande wa kulia wa paneli ya onyesho.",
                    pointXFraction = 0.78f,
                    pointYFraction = 0.42f,
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
            instructionEn = "Next: tap the highlighted control in the demo UI.",
            instructionSw = "Ifuatayo: bonyeza kidhibiti kilichoangaziwa kwenye UI ya onyesho.",
            pointXFraction = 0.72f,
            pointYFraction = 0.38f,
        )
        2 -> GuideStep(
            instructionEn = "Almost there — confirm the gray action bar, then ask “submit”.",
            instructionSw = "Karibu — thibitisha upau wa kijivu, kisha uliza “wasilisha”.",
            pointXFraction = 0.55f,
            pointYFraction = 0.55f,
        )
        else -> GuideStep(
            instructionEn = "Ask what you want to do, or leave blank and we’ll point at the first control.",
            instructionSw = "Uliza unachotaka kufanya, au acha tupu — tutaonyesha kidhibiti cha kwanza.",
            pointXFraction = 0.50f,
            pointYFraction = 0.50f,
        )
    }

    fun reset() {
        stepIndex = 0
    }
}
