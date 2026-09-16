package com.dira.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dira.app.session.GuideUiState
import com.dira.app.ui.copy
import com.dira.app.ui.overlay.FakePointerOverlay

@Composable
fun GuideScreen(
    useSwahili: Boolean,
    state: GuideUiState,
    onStop: () -> Unit,
    onAskGuide: () -> Unit,
    onQuestionChange: (String) -> Unit,
) {
    val c = copy(useSwahili)
    val minutes = (state.remainingMs / 60_000L).toInt()
    val seconds = ((state.remainingMs % 60_000L) / 1000L).toInt()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    c.watching,
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    c.sessionTimer.format(minutes, seconds),
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(c.stop)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF263238)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Demo UI", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(
                    c.demoModule,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 48.dp)
                        .height(48.dp)
                        .fillMaxWidth(0.45f)
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF546E7A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Submit", color = Color.White)
                }
            }
            // Overlay driven by guide response fractions (not hardcoded forever).
            FakePointerOverlay(
                xFraction = state.pointX,
                yFraction = state.pointY,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.instruction.ifBlank { c.fakeStep }, style = MaterialTheme.typography.titleMedium)
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(
                value = state.question,
                onValueChange = onQuestionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(c.askHint) },
                singleLine = true,
                enabled = !state.loading,
            )
            Button(
                onClick = onAskGuide,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.loading && state.watching,
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(c.guideStep)
                }
            }
            Text(
                "${c.guideSource}: ${state.guideSource}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
