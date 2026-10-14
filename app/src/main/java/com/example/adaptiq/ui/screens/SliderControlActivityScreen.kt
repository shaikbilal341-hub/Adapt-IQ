package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SliderControlActivityScreen(
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
    val targetValues = listOf(50f, 80f, 25f)
    var currentTrial by remember { mutableIntStateOf(1) }
    var isPractice by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    var trialStartTime by remember { mutableLongStateOf(0L) }
    var feedbackText by remember { mutableStateOf("") }

    val currentTarget = targetValues.getOrElse(currentTrial - 1) { 50f }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice) {
            trialStartTime = System.currentTimeMillis()
            sliderValue = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Activity 9 of 13: Slider Control",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Move the slider to match the target value indicated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                sliderValue = 0f
            }
        )

        if (!isCompleted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Trial $currentTrial of $totalTrials", fontWeight = FontWeight.Bold)
                Text(text = "Target Value: ${currentTarget.toInt()}")
            }

            LinearProgressIndicator(
                progress = { currentTrial.toFloat() / totalTrials.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Current Value: ${sliderValue.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..100f,
                        steps = 100,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val duration = if (trialStartTime > 0) System.currentTimeMillis() - trialStartTime else 0L
                            val error = abs(sliderValue - currentTarget)
                            val success = error <= 3f

                            feedbackText = if (success) "✓ Target matched!" else "Close! Error: ${error.toInt()}"

                            if (!isPractice) {
                                onLogEvent(
                                    "slider_target=${currentTarget.toInt()}",
                                    "slider_value=${sliderValue.toInt()}",
                                    if (success) "successful" else "incorrect",
                                    currentTrial,
                                    duration,
                                    "error=${error.toInt()}"
                                )
                            }

                            MainScope().launch {
                                delay(800)
                                if (currentTrial < totalTrials) {
                                    currentTrial++
                                    sliderValue = 0f
                                    feedbackText = ""
                                } else {
                                    isCompleted = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Value", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (feedbackText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackText,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Slider Control Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All $totalTrials slider control trials completed successfully.")
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
