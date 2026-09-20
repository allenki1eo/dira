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
    val overlayActive: String,
    val overlayHelp: String,
)

fun copy(useSwahili: Boolean): DiraCopy = if (useSwahili) DiraCopy(
    consentTitle = "Kabla Dira iweze kukusaidia",
    consentBody = "Dira ni msaidizi wa muda wa UI wa Android, siyo kinasa skrini.\n\n" +
        "Ukibonyeza Saidia, tunaomba: (1) kuchora juu ya programu zingine — kiputo na kielekezi; " +
        "(2) kushiriki skrini kwa fremu za muda; (3) maikrofoni kwa sauti, hiari. " +
        "Fremu zinakaa kwenye kumbukumbu. Acha wakati wowote; kikao kinaisha baada ya dakika 5.",
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
    projectionHint = "Kwanza ruhusa ya kiputo juu ya programu, kisha kushiriki skrini. Fungua Gmail au programu yoyote — bonyeza kiputo kuandika au kusema.",
    serverUrlHint = "URL ya seva ya mwongozo (acha tupu = onyesho)",
    tapMapLabel = "Sehemu ya kubonyeza kwenye skrini yako",
    overlayActive = "Kiputo kiko hai. Fungua programu unayotaka, kisha bonyeza kiputo cha D.",
    overlayHelp = "Tutachora kiputo juu ya programu zingine. Bonyeza ili kuandika au kushiriki sauti — kisha kielekezi kitaonyesha mahali pa kubonyeza.",
) else DiraCopy(
    consentTitle = "Before Dira can help",
    consentBody = "Dira is a temporary Android UI helper, not a recorder.\n\n" +
        "When you tap Help we ask to: (1) draw over other apps — a bubble and a pointer; " +
        "(2) capture the screen for short-lived frames; (3) use the mic for voice, optional. " +
        "Frames stay in memory. Stop anytime; sessions also end after 5 minutes.",
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
    projectionHint = "You'll first allow a bubble over other apps, then screen share. Open Gmail (or any app) and tap the bubble to type or speak.",
    serverUrlHint = "Guide server URL (blank = mock)",
    tapMapLabel = "Tap target on your screen",
    overlayActive = "The bubble is live. Open the app you need, then tap the D bubble.",
    overlayHelp = "We'll draw a bubble over other apps. Tap it to type or send a voice note — then a pointer shows where to tap.",
)
