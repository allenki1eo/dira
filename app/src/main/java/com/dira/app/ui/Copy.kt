package com.dira.app.ui

/** SW/EN copy — consent, watching, stop, session cleared. Generic UI coach only. */
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
    val sessionTimedOut: String,
    val askHint: String,
    val fakeStep: String,
    val demoModule: String,
    val guideStep: String,
    val guideSource: String,
    val sessionTimer: String,
    val projectionHint: String,
    val serverUrlHint: String,
    val tapMapLabel: String,
)

fun copy(useSwahili: Boolean): DiraCopy = if (useSwahili) DiraCopy(
    consentTitle = "Kabla Dira iweze kukusaidia",
    consentBody = "Dira ni msaidizi wa muda wa UI wa Android, siyo kinasa skrini.\n\n" +
        "Ukibonyeza Saidia, tunaomba ruhusa ya kuona skrini yako tu ili kuonyesha hatua inayofuata. " +
        "Fremu zinakaa kwenye kumbukumbu — hazihifadhiwi. " +
        "Unaweza kubonyeza Acha wakati wowote — kisha kikao kinafutwa. " +
        "Kikao pia kitaisha baada ya dakika 5.",
    consentAccept = "Nimeelewa — endelea",
    homeTitle = "Dira",
    homeTagline = "Dira yako unapokwama kwenye programu yoyote",
    homeHelp = "Nisaidie kwenye skrini hii",
    watching = "Inatazama…",
    stop = "Acha kutazama",
    sessionCleared = "Kikao kimefutwa.",
    sessionTimedOut = "Muda wa kikao umeisha (dakika 5).",
    askHint = "Uliza unachotaka kufanya…",
    fakeStep = "Ifuatayo: bonyeza kidhibiti kilichoonyeshwa",
    demoModule = "Mwongozo wa UI wa Android (programu yoyote)",
    guideStep = "Pata hatua",
    guideSource = "Chanzo",
    sessionTimer = "Iliyobaki: %d:%02d",
    projectionHint = "Utapata dirisha la mfumo la kushiriki skrini. Hakuna Accessibility, anwani, SMS, wala hifadhi.",
    serverUrlHint = "URL ya seva ya mwongozo (acha tupu = onyesho)",
    tapMapLabel = "Sehemu ya kubonyeza kwenye skrini yako",
) else DiraCopy(
    consentTitle = "Before Dira can help",
    consentBody = "Dira is a temporary Android UI helper, not a recorder.\n\n" +
        "When you tap Help, we ask for screen capture permission only to show the next step. " +
        "Frames stay in memory — they are not kept. " +
        "You can Stop anytime — then the session is cleared. " +
        "Sessions also end after 5 minutes.",
    consentAccept = "I understand — continue",
    homeTitle = "Dira",
    homeTagline = "Your compass when any app gets stuck",
    homeHelp = "Help me on this screen",
    watching = "Watching…",
    stop = "Stop watching",
    sessionCleared = "Session cleared.",
    sessionTimedOut = "Session timed out (5 minutes).",
    askHint = "Ask what you want to do…",
    fakeStep = "Next: tap the highlighted control",
    demoModule = "Generic Android UI coach (any app)",
    guideStep = "Guide step",
    guideSource = "Source",
    sessionTimer = "Left: %d:%02d",
    projectionHint = "You’ll get the system screen-share prompt. No Accessibility, contacts, SMS, or storage.",
    serverUrlHint = "Guide server URL (blank = mock)",
    tapMapLabel = "Tap target on your screen",
)
