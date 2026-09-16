package com.dira.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dira.app.ui.screens.ConsentScreen
import com.dira.app.ui.screens.GuideScreen
import com.dira.app.ui.screens.HomeScreen

enum class DiraRoute { Consent, Home, Guide }

/**
 * Phase 1 shell: Consent → Home → Guide (Watching / Stop / fake pointer).
 * No MediaProjection yet; no hardcoded TRA/bank modules.
 */
@Composable
fun DiraApp() {
    var route by remember { mutableStateOf(DiraRoute.Consent) }
    var useSwahili by remember { mutableStateOf(false) }
    var clearedBanner by remember { mutableStateOf(false) }

    when (route) {
        DiraRoute.Consent -> ConsentScreen(
            useSwahili = useSwahili,
            onToggleLanguage = { useSwahili = !useSwahili },
            onAccept = { route = DiraRoute.Home },
        )
        DiraRoute.Home -> HomeScreen(
            useSwahili = useSwahili,
            onToggleLanguage = { useSwahili = !useSwahili },
            showCleared = clearedBanner,
            onDismissCleared = { clearedBanner = false },
            onHelp = {
                clearedBanner = false
                route = DiraRoute.Guide
            },
        )
        DiraRoute.Guide -> GuideScreen(
            useSwahili = useSwahili,
            onStop = {
                // Phase 1: "clear session buffer" = leave guide; banner confirms wipe.
                clearedBanner = true
                route = DiraRoute.Home
            },
        )
    }
}
