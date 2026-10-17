package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TargetTrackingActivityScreen(
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
    var feedbackText by remember { mutableStateOf("Hold your finger on the moving target to track it.") }

    var targetOffsetX by remember { mutableFloatStateOf(0f) }
    var targetOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice) {
            trialStartTime = System.currentTimeMillis()
        }
    }

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 8 of 13: Target Tracking",
            subtitle = "Follow the moving target with your finger."
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                feedbackText = "Keep tracking the moving target..."
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
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                }

                AdaptIQProgressIndicator(
                    progress = currentTrial.toFloat() / totalTrials.toFloat()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceElevatedVariant, shape = RoundedCornerShape(20.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                val duration = if (trialStartTime > 0) System.currentTimeMillis() - trialStartTime else 0L
                                feedbackText = "✓ Tracking Completed!"
                                if (!isPractice) {
                                    onLogEvent("track", "target_tracking", "successful", currentTrial, duration, "offset_x=$targetOffsetX")
                                }

                                MainScope().launch {
                                    delay(700)
                                    if (currentTrial < totalTrials) {
                                        currentTrial++
                                        targetOffsetX = 0f
                                        targetOffsetY = 0f
                                        feedbackText = "Keep tracking the moving target..."
                                    } else {
                                        isCompleted = true
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                targetOffsetX += dragAmount.x * 0.5f
                                targetOffsetY += dragAmount.y * 0.5f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .offset { IntOffset(targetOffsetX.roundToInt(), targetOffsetY.roundToInt()) }
                        .clip(CircleShape)
                        .background(AccentTeal)
                        .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            AdaptIQSurfaceCard(
                borderColor = BorderSubtle,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = feedbackText,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            AdaptIQSurfaceCard(
                borderColor = StatusSuccess.copy(alpha = 0.5f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "✓ Target Tracking Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "All $totalTrials tracking trials recorded successfully.",
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
