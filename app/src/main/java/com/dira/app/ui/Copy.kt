package com.dira.app.ui

/** SW/EN copy for Phase 1 — mirrors values / values-sw without forcing locale restart. */
data class DiraCopy(
    val consentTitle: String,
    val consentBody: String,
    val consentAccept: String,
    val homeTitle: String,
    val homeTagline: String,
    val homeHelp: String,
    val watching: String,
    val stop: String,
    val sessionCleared: String,
    val askHint: String,
    val fakeStep: String,
    val demoModule: String,
)

fun copy(useSwahili: Boolean): DiraCopy = if (useSwahili) DiraCopy(
    consentTitle = "Kabla Dira iweze kukusaidia",
    consentBody = "Dira ni msaidizi wa muda, siyo kinasa skrini.\n\n" +
        "Ukibonyeza Saidia, tunaweza kuona skrini yako tu ili kuonyesha hatua inayofuata. " +
        "Hatuhifadhi skrini za benki wala serikali. Unaweza Kubonyeza Simamisha wakati wowote — " +
        "kisha kikao kinafutwa.",
    consentAccept = "Nimeelewa — endelea",
    homeTitle = "Dira",
    homeTagline = "Dira yako unapokwama kwenye programu",
    homeHelp = "Nisaidie kwenye skrini hii",
    watching = "Inatazama…",
    stop = "Acha kutazama",
    sessionCleared = "Kikao kimefutwa.",
    askHint = "Uliza unachotaka kufanya…",
    fakeStep = "Ifuatayo: bonyeza kidhibiti kilichoangaziwa",
    demoModule = "Moduli ya onyesho (bado hakuna TRA/benki)",
) else DiraCopy(
    consentTitle = "Before Dira can help",
    consentBody = "Dira is a temporary helper, not a recorder.\n\n" +
        "When you tap Help, we may look at your screen only to show the next step. " +
        "We do not keep bank or government screens. You can Stop anytime — then the session is cleared.",
    consentAccept = "I understand — continue",
    homeTitle = "Dira",
    homeTagline = "Your compass when software gets stuck",
    homeHelp = "Help me on this screen",
    watching = "Watching…",
    stop = "Stop watching",
    sessionCleared = "Session cleared.",
    askHint = "Ask what you want to do…",
    fakeStep = "Next: tap the highlighted control",
    demoModule = "Demo module (no real TRA/bank yet)",
)
