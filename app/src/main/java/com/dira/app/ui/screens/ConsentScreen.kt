package com.dira.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dira.app.ui.copy

@Composable
fun ConsentScreen(
    useSwahili: Boolean,
    onToggleLanguage: () -> Unit,
    onAccept: () -> Unit,
) {
    val c = copy(useSwahili)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
        Text(c.consentTitle, style = MaterialTheme.typography.headlineSmall)
        Text(c.consentBody, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
            Text(c.consentAccept)
        }
    }
}
