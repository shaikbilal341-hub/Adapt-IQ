package com.example.adaptiq.ui.screens

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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NavigationTaskActivityScreen(
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
    val targetDestinations = listOf("Settings", "Help", "Profile")
    var currentTrial by remember { mutableIntStateOf(1) }
    var isPractice by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("Home") }
    var trialStartTime by remember { mutableLongStateOf(0L) }
    var feedbackText by remember { mutableStateOf("") }

    val currentGoal = targetDestinations.getOrElse(currentTrial - 1) { "Settings" }

    fun checkNavigation(navigatedTo: String, goal: String) {
        val duration = if (trialStartTime > 0) System.currentTimeMillis() - trialStartTime else 0L
        val success = navigatedTo == goal

        feedbackText = if (success) "✓ Goal reached ($currentGoal)!" else "Navigated to $navigatedTo. Keep searching."

        if (!isPractice) {
            onLogEvent(
                "navigate_to_$goal",
                "navigated_$navigatedTo",
                if (success) "successful" else "incorrect",
                currentTrial,
                duration,
                "screen=$navigatedTo"
            )
        }

        if (success) {
            MainScope().launch {
                delay(900)
                if (currentTrial < totalTrials) {
                    currentTrial++
                    currentScreen = "Home"
                    feedbackText = "Goal: Open '${targetDestinations.getOrElse(currentTrial - 1) { "Settings" }}'"
                } else {
                    isCompleted = true
                }
            }
        }
    }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice) {
            trialStartTime = System.currentTimeMillis()
            currentScreen = "Home"
            feedbackText = "Goal: Open '$currentGoal'"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Activity 11 of 13: Navigation Task",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Navigate through the simulated app interface to reach the target screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                currentScreen = "Home"
                feedbackText = "Goal: Open '$currentGoal'"
            }
        )

        if (!isCompleted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Trial $currentTrial of $totalTrials", fontWeight = FontWeight.Bold)
                Text(text = "Goal: Open $currentGoal")
            }

            LinearProgressIndicator(
                progress = { currentTrial.toFloat() / totalTrials.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Simulated Header Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Simulated App • Current Screen: $currentScreen",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentScreen == "Home") {
                        Button(
                            onClick = { currentScreen = "Settings"; checkNavigation("Settings", currentGoal) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Settings")
                        }
                        Button(
                            onClick = { currentScreen = "Help"; checkNavigation("Help", currentGoal) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Help")
                        }
                        Button(
                            onClick = { currentScreen = "Messages"; checkNavigation("Messages", currentGoal) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Messages")
                        }
                        Button(
                            onClick = { currentScreen = "Profile"; checkNavigation("Profile", currentGoal) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Profile")
                        }
                    } else {
                        Button(
                            onClick = { currentScreen = "Home" },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("← Back to Home")
                        }
                    }
                }
            }

            if (feedbackText.isNotBlank()) {
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
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Navigation Task Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All $totalTrials navigation goals achieved successfully.")
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
