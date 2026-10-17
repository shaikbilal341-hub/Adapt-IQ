package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
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

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 11 of 13: Navigation Task",
            subtitle = "Navigate through the simulated app interface to reach the target screen."
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
                        text = "Goal: '$currentGoal'",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                containerColor = SurfaceElevatedVariant,
                borderColor = BorderSubtle
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Simulated Header Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceElevated, shape = RoundedCornerShape(10.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Simulated Interface • Screen: $currentScreen",
                            color = AccentTeal,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentScreen == "Home") {
                        AdaptIQSecondaryButton(
                            text = "Open Settings",
                            onClick = { currentScreen = "Settings"; checkNavigation("Settings", currentGoal) }
                        )
                        AdaptIQSecondaryButton(
                            text = "Open Help",
                            onClick = { currentScreen = "Help"; checkNavigation("Help", currentGoal) }
                        )
                        AdaptIQSecondaryButton(
                            text = "Open Messages",
                            onClick = { currentScreen = "Messages"; checkNavigation("Messages", currentGoal) }
                        )
                        AdaptIQSecondaryButton(
                            text = "Open Profile",
                            onClick = { currentScreen = "Profile"; checkNavigation("Profile", currentGoal) }
                        )
                    } else {
                        AdaptIQPrimaryButton(
                            text = "← Back to Home",
                            onClick = { currentScreen = "Home" }
                        )
                    }
                }
            }

            if (feedbackText.isNotBlank()) {
                AdaptIQSurfaceCard(
                    borderColor = if (feedbackText.startsWith("✓")) StatusSuccess.copy(alpha = 0.5f) else BorderSubtle,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = feedbackText,
                        color = if (feedbackText.startsWith("✓")) StatusSuccess else TextPrimary,
                        fontWeight = FontWeight.Medium,
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
                        text = "✓ Navigation Task Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "All $totalTrials navigation goals achieved successfully.",
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
