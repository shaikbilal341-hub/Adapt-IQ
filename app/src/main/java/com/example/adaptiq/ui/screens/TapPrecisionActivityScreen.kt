package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

@Composable
fun TapPrecisionActivityScreen(
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
    var trialStartTime by remember { mutableLongStateOf(0L) }
    var feedbackText by remember { mutableStateOf<String?>(null) }

    // Target configuration (deterministic random positions/sizes for validity)
    val targetSizes = listOf(64.dp, 48.dp, 36.dp, 56.dp, 40.dp)
    val currentSize = targetSizes.getOrElse(currentTrial - 1) { 48.dp }

    val xOffsets = listOf(0.2f, 0.7f, 0.4f, 0.8f, 0.3f)
    val yOffsets = listOf(0.3f, 0.6f, 0.8f, 0.4f, 0.5f)
    val currentX = xOffsets.getOrElse(currentTrial - 1) { 0.5f }
    val currentY = yOffsets.getOrElse(currentTrial - 1) { 0.5f }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice) {
            trialStartTime = System.currentTimeMillis()
        }
    }

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 2 of 13: Tap Precision",
            subtitle = "Tap the circular targets as they appear on the screen."
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                feedbackText = null
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

            // Interactive Target Box Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevatedVariant)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(currentSize)
                            .clip(CircleShape)
                            .background(AccentTeal)
                            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            .clickable {
                                val duration = if (trialStartTime > 0) System.currentTimeMillis() - trialStartTime else 0L
                                feedbackText = "✓ Target Hit! (${duration}ms)"

                                if (!isPractice) {
                                    onLogEvent(
                                        "tap",
                                        "tap",
                                        "successful",
                                        currentTrial,
                                        duration,
                                        "size=${currentSize.value},x_ratio=$currentX,y_ratio=$currentY"
                                    )
                                }

                                if (currentTrial < totalTrials) {
                                    currentTrial++
                                } else {
                                    isCompleted = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            feedbackText?.let {
                AdaptIQSurfaceCard(
                    borderColor = StatusSuccess.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = it,
                        color = StatusSuccess,
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
                        text = "✓ Tap Precision Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "All $totalTrials precision trials recorded successfully.",
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
