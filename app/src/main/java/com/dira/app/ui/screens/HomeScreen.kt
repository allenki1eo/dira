package com.dira.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dira.app.ui.copy

@Composable
fun HomeScreen(
    useSwahili: Boolean,
    onToggleLanguage: () -> Unit,
    showCleared: Boolean,
    timedOut: Boolean = false,
    onDismissCleared: () -> Unit,
    onHelp: () -> Unit,
    guideModeLabel: String = "",
    guideApiBase: String = "",
    onGuideBaseChange: (String) -> Unit = {},
    overlayActive: Boolean = false,
    onStopOverlay: () -> Unit = {},
    uiTreeEnabled: Boolean = false,
    onEnableUiTree: () -> Unit = {},
) {
    val c = copy(useSwahili)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !useSwahili,
                onClick = { if (useSwahili) onToggleLanguage() },
                label = { Text("English") },
            )
            FilterChip(
                selected = useSwahili,
                onClick = { if (!useSwahili) onToggleLanguage() },
                label = { Text("Kiswahili") },
            )
        }
        Text(c.homeTitle, style = MaterialTheme.typography.displaySmall)
        Text(c.homeTagline, style = MaterialTheme.typography.bodyLarge)
        Text(
            c.demoModule,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (guideModeLabel.isNotBlank()) {
            Text(
                guideModeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        OutlinedTextField(
            value = guideApiBase,
            onValueChange = onGuideBaseChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("http://192.168.1.10:8787") },
            label = { Text(c.serverUrlHint) },
            singleLine = true,
        )
        if (showCleared) {
            Card(onClick = onDismissCleared, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(c.sessionCleared, style = MaterialTheme.typography.titleMedium)
                    if (timedOut) {
                        Text(c.sessionTimedOut, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Text(
            c.overlayHelp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        if (overlayActive) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.overlayActive, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onStopOverlay, modifier = Modifier.fillMaxWidth()) {
                        Text(c.stop)
                    }
                }
            }
        }
        Text(
            if (uiTreeEnabled) c.uiTreeOn else c.uiTreeOff,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (!uiTreeEnabled) {
            Button(onClick = onEnableUiTree, modifier = Modifier.fillMaxWidth()) {
                Text(c.uiTreeButton)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth(),
            enabled = !overlayActive,
        ) {
            Text(c.homeHelp)
        }
        Text(
            c.projectionHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
