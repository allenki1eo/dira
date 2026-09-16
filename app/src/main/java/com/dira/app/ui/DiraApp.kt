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
 * Phase 2: Consent → Home → (MediaProjection permission) → Guide
 * Overlay driven by guide targets; Stop / timeout clears session buffer.
 */
@Composable
fun DiraApp(
    sessionState: GuideUiState,
    onHelp: (useSwahili: Boolean) -> Unit,
    onStop: () -> Unit,
    onAskGuide: (useSwahili: Boolean) -> Unit,
    onQuestionChange: (String) -> Unit,
    onDismissCleared: () -> Unit,
) {
    var route by remember { mutableStateOf(DiraRoute.Consent) }
    var useSwahili by remember { mutableStateOf(false) }

    LaunchedEffect(sessionState.watching) {
        if (sessionState.watching) {
            route = DiraRoute.Guide
        } else if (route == DiraRoute.Guide) {
            route = DiraRoute.Home
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
