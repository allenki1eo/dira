package com.dira.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dira.app.session.GuideUiState
import com.dira.app.ui.screens.ConsentScreen
import com.dira.app.ui.screens.GuideScreen
import com.dira.app.ui.screens.HomeScreen

enum class DiraRoute { Consent, Home, Guide }

/**
 * Consent → Home → overlay bubble (preferred) or in-app Guide fallback.
 */
@Composable
fun DiraApp(
    sessionState: GuideUiState,
    onHelp: (useSwahili: Boolean) -> Unit,
    onStop: () -> Unit,
    onAskGuide: (useSwahili: Boolean) -> Unit,
    onQuestionChange: (String) -> Unit,
    onGuideBaseChange: (String) -> Unit,
    onDismissCleared: () -> Unit,
) {
    var route by remember { mutableStateOf(DiraRoute.Consent) }
    var useSwahili by remember { mutableStateOf(false) }

    LaunchedEffect(sessionState.watching, sessionState.overlayMode) {
        route = when {
            sessionState.watching && !sessionState.overlayMode -> DiraRoute.Guide
            sessionState.watching && sessionState.overlayMode -> DiraRoute.Home
            route == DiraRoute.Guide -> DiraRoute.Home
            else -> route
        }
    }

    when (route) {
        DiraRoute.Consent -> ConsentScreen(
            useSwahili = useSwahili,
            onToggleLanguage = { useSwahili = !useSwahili },
            onAccept = { route = DiraRoute.Home },
        )
        DiraRoute.Home -> HomeScreen(
            useSwahili = useSwahili,
            onToggleLanguage = { useSwahili = !useSwahili },
            showCleared = sessionState.sessionCleared,
            timedOut = sessionState.timedOut,
            onDismissCleared = onDismissCleared,
            onHelp = { onHelp(useSwahili) },
            guideApiBase = sessionState.guideApiBase,
            onGuideBaseChange = onGuideBaseChange,
            overlayActive = sessionState.watching && sessionState.overlayMode,
            onStopOverlay = onStop,
            guideModeLabel = if (sessionState.guideSource == "mock") {
                if (useSwahili) "Hali ya onyesho (bila seva)" else "Mock guide (no backend)"
            } else {
                if (useSwahili) "Hali ya API" else "API guide mode"
            },
        )
        DiraRoute.Guide -> GuideScreen(
            useSwahili = useSwahili,
            state = sessionState,
            onStop = onStop,
            onAskGuide = { onAskGuide(useSwahili) },
            onQuestionChange = onQuestionChange,
        )
    }
}
