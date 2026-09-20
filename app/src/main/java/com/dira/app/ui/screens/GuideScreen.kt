package com.dira.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dira.app.R
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.dira_watching_mark),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(end = 8.dp),
                )
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
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxHeight(),
            ) {
                Text(
                    c.tapMapLabel,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF263238)),
                ) {
                    // Pointer fractions are 0–1 of the captured screen, shown on this map.
                    FakePointerOverlay(
                        xFraction = state.pointX,
                        yFraction = state.pointY,
                        boxX = state.boxX,
                        boxY = state.boxY,
                        boxW = state.boxW,
                        boxH = state.boxH,
                        label = state.targetLabel,
                    )
                }
            }
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
