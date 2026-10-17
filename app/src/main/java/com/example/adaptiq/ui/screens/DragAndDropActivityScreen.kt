package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.math.roundToInt

@Composable
fun DragAndDropActivityScreen(
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
    var feedbackText by remember { mutableStateOf("Drag the blue shape into the target box.") }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentTrial, isPractice) {
        if (!isPractice) {
            trialStartTime = System.currentTimeMillis()
            offsetX = 0f
            offsetY = 0f
        }
    }

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 7 of 13: Drag & Drop",
            subtitle = "Drag the object into the destination target box."
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentTrial = 1
                isCompleted = false
                offsetX = 0f
                offsetY = 0f
                feedbackText = "Drag the shape into the target box."
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
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Draggable Shape
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .background(AccentTeal, shape = RoundedCornerShape(16.dp))
                            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
                                        val duration = if (trialStartTime > 0) System.currentTimeMillis() - trialStartTime else 0L
                                        val success = distance > 80f

                                        if (success) {
                                            feedbackText = "✓ Successfully Dropped!"
                                            if (!isPractice) {
                                                onLogEvent("drag", "drag_drop", "successful", currentTrial, duration, "distance=${distance.toInt()}")
                                            }

                                            MainScope().launch {
                                                delay(800)
                                                if (currentTrial < totalTrials) {
                                                    currentTrial++
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                    feedbackText = "Drag the shape into the target box."
                                                } else {
                                                    isCompleted = true
                                                }
                                            }
                                        } else {
                                            feedbackText = "Not far enough. Drop inside the target box."
                                            if (!isPractice) {
                                                onLogEvent("drag", "drag_drop", "incorrect", currentTrial, duration, "distance=${distance.toInt()}")
                                            }
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Drag Me", color = BackgroundDark, fontWeight = FontWeight.Bold)
                    }

                    // Target Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(SurfaceElevated, shape = RoundedCornerShape(14.dp))
                            .border(2.dp, AccentBlue.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TARGET DROP BOX",
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            letterSpacing = 1.sp
                        )
                    }
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
                        text = "✓ Drag & Drop Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "All $totalTrials trials completed successfully.",
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
