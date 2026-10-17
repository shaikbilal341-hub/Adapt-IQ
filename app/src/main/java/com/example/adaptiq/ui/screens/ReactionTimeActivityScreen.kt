package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ReactionTimeActivityScreen(
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
    val totalTrials = 5
    var currentTrial by remember { mutableIntStateOf(1) }
    var isPractice by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }

    // States: WAITING_DELAY, READY_TO_TAP, TAP_RECORDED
    var trialState by remember { mutableStateOf("IDLE") }
    var signalStartTime by remember { mutableLongStateOf(0L) }
    var reactionTimeMs by remember { mutableLongStateOf(0L) }
    var feedbackText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    suspend fun startTrial() {
        trialState = "WAITING_DELAY"
        feedbackText = "Wait for green signal..."
        val randomDelay = Random.nextLong(1200L, 3000L)
        delay(randomDelay)
        trialState = "READY_TO_TAP"
        signalStartTime = System.currentTimeMillis()
        feedbackText = "TAP NOW!"
    }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice && !isCompleted) {
            startTrial()
        }
    }

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 3 of 13: Reaction Time",
            subtitle = "Tap the box as quickly as possible when it turns green."
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                feedbackText = ""
                coroutineScope.launch { startTrial() }
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
                        text = "${(currentTrial.toFloat() / totalTrials.toFloat() * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold
                    )
                }

                AdaptIQProgressIndicator(
                    progress = currentTrial.toFloat() / totalTrials.toFloat()
                )
            }

            val boxColor = when (trialState) {
                "READY_TO_TAP" -> StatusSuccess
                "TAP_RECORDED" -> SurfaceElevatedVariant
                "WAITING_DELAY" -> Color(0xFF6B1D2F)
                else -> SurfaceElevated
            }

            val boxBorder = when (trialState) {
                "READY_TO_TAP" -> AccentTeal
                "TAP_RECORDED" -> AccentBlue
                "WAITING_DELAY" -> StatusError
                else -> BorderSubtle
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(boxColor)
                    .border(2.dp, boxBorder, RoundedCornerShape(20.dp))
                    .clickable(enabled = trialState == "WAITING_DELAY" || trialState == "READY_TO_TAP") {
                        if (trialState == "WAITING_DELAY") {
                            feedbackText = "Too early! Wait for green."
                            if (!isPractice) {
                                onLogEvent("tap", "premature_tap", "incorrect", currentTrial, 0L, "status=premature")
                            }
                        } else if (trialState == "READY_TO_TAP") {
                            val rt = System.currentTimeMillis() - signalStartTime
                            reactionTimeMs = rt
                            trialState = "TAP_RECORDED"
                            feedbackText = "Reaction Time: ${rt}ms"

                            if (!isPractice) {
                                onLogEvent("tap", "reaction_tap", "successful", currentTrial, rt, "reaction_ms=$rt")
                            }

                            coroutineScope.launch {
                                delay(900)
                                if (currentTrial < totalTrials) {
                                    currentTrial++
                                    startTrial()
                                } else {
                                    isCompleted = true
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = feedbackText,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            AdaptIQSurfaceCard(
                borderColor = StatusSuccess.copy(alpha = 0.5f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "✓ Reaction Time Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "All $totalTrials reaction trials recorded successfully.",
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
