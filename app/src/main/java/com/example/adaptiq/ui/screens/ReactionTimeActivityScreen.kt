package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Activity 3 of 13: Reaction Time",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tap the box as quickly as possible when it turns green.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Trial $currentTrial of $totalTrials", fontWeight = FontWeight.Bold)
                Text(text = "${(currentTrial.toFloat() / totalTrials.toFloat() * 100).toInt()}%")
            }

            LinearProgressIndicator(
                progress = { currentTrial.toFloat() / totalTrials.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            val boxColor = when (trialState) {
                "READY_TO_TAP" -> Color(0xFF2E7D32)
                "TAP_RECORDED" -> Color(0xFF1976D2)
                else -> Color(0xFFD32F2F)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(boxColor, shape = RoundedCornerShape(16.dp))
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
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Reaction Time Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All $totalTrials reaction trials recorded successfully.")
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
