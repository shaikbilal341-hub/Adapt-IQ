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
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
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

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 9 of 13: Slider Control",
            subtitle = "Move the slider to match the target value indicated."
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trial $currentTrial of $totalTrials",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Target: ${currentTarget.toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                }

                AdaptIQProgressIndicator(
                    progress = currentTrial.toFloat() / totalTrials.toFloat()
                )
            }

            AdaptIQSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceElevatedVariant,
                borderColor = BorderSubtle
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Current Value: ${sliderValue.toInt()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..100f,
                        steps = 100,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentTeal,
                            activeTrackColor = AccentTeal,
                            inactiveTrackColor = BorderSubtle
                        )
                    )

                    AdaptIQPrimaryButton(
                        text = "Confirm Value",
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
                        }
                    )
                }
            }

            if (feedbackText.isNotBlank()) {
                AdaptIQSurfaceCard(
                    borderColor = if (feedbackText.startsWith("✓")) StatusSuccess.copy(alpha = 0.5f) else StatusWarning.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = feedbackText,
                        color = if (feedbackText.startsWith("✓")) StatusSuccess else StatusWarning,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            AdaptIQSurfaceCard(
                borderColor = StatusSuccess.copy(alpha = 0.5f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "✓ Slider Control Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "All $totalTrials slider control trials completed successfully.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AdaptIQPrimaryButton(
                text = "Continue to Next Activity",
                onClick = onActivityCompleted
            )
        }

        AdaptIQOutlinedButton(
            text = "Back",
            onClick = onBack
        )
    }
}
