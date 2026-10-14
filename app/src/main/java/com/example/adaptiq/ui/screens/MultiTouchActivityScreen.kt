package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MultiTouchActivityScreen(
    onLogEvent: (
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String
    ) -> Unit,
    onActivityCompleted: () -> Unit,
    onBack: () -> Unit
) {
    val totalTrials = 3
    var currentTrial by remember { mutableIntStateOf(1) }
    var isPractice by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var trialStartTime by remember { mutableLongStateOf(0L) }
    var feedbackText by remember { mutableStateOf("Place two fingers on the box and pinch or spread.") }

    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice) {
            trialStartTime = System.currentTimeMillis()
            scale = 1f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Activity 12 of 13: Multi-Touch",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Perform a controlled two-finger pinch or spread gesture.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                scale = 1f
                feedbackText = "Perform pinch or spread gesture."
            }
        )

        if (!isCompleted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Trial $currentTrial of $totalTrials", fontWeight = FontWeight.Bold)
                Text(text = "Scale: ${String.format("%.2f", scale)}x")
            }

            LinearProgressIndicator(
                progress = { currentTrial.toFloat() / totalTrials.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale *= zoom
                            val duration = if (trialStartTime > 0) System.currentTimeMillis() - trialStartTime else 0L

                            if (abs(scale - 1f) > 0.3f) {
                                feedbackText = "✓ Multi-touch gesture detected!"
                                if (!isPractice) {
                                    onLogEvent("multi_touch", "pinch_spread", "successful", currentTrial, duration, "scale=$scale")
                                }

                                MainScope().launch {
                                    delay(800)
                                    if (currentTrial < totalTrials) {
                                        currentTrial++
                                        scale = 1f
                                        feedbackText = "Perform pinch or spread gesture."
                                    } else {
                                        isCompleted = true
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((120 * scale).coerceIn(60f, 220f).dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pinch / Spread",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedbackText,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Multi-Touch Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All $totalTrials multi-touch trials recorded successfully.")
                }
            }

            Button(
                onClick = onActivityCompleted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Next Activity", fontWeight = FontWeight.Bold)
            }
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
