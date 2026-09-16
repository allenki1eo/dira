package com.dira.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
    onDismissCleared: () -> Unit,
    onHelp: () -> Unit,
) {
    val c = copy(useSwahili)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
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
        if (showCleared) {
            Card(onClick = onDismissCleared, modifier = Modifier.fillMaxWidth()) {
                Text(
                    c.sessionCleared,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier = Modifier.height(24.dp))
        Button(onClick = onHelp, modifier = Modifier.fillMaxWidth()) {
            Text(c.homeHelp)
        }
    }
}
