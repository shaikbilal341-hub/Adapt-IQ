package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs

private data class GestureTask(
    val id: String,
    val title: String,
    val instruction: String,
    val actionLabel: String
)

@Composable
fun GestureActivityScreen(
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
    val gestureTasks = remember {
        listOf(
            GestureTask("swipe_up", "Swipe Up", "Swipe from the bottom toward the top.", "swipe_up"),
            GestureTask("swipe_down", "Swipe Down", "Swipe from the top toward the bottom.", "swipe_down"),
            GestureTask("swipe_left", "Swipe Left", "Swipe from right toward the left.", "swipe_left"),
            GestureTask("swipe_right", "Swipe Right", "Swipe from left toward the right.", "swipe_right"),
            GestureTask("double_tap", "Double Tap", "Quickly tap twice inside the box.", "double_tap")
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isPractice by remember { mutableStateOf(true) }
    var attemptId by remember { mutableIntStateOf(1) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isAllCompleted by remember { mutableStateOf(false) }

    val currentTask = gestureTasks.getOrNull(currentStepIndex)

    fun handleGestureDetected(detectedAction: String, touchDetails: String, durationMs: Long) {
        val expectedAction = currentTask?.actionLabel ?: return

        if (detectedAction == expectedAction) {
            isSuccess = true
            feedbackMessage = "✓ Detected Successfully!"

            if (!isPractice) {
                onLogEvent(
                    expectedAction,
                    detectedAction,
                    "successful",
                    attemptId,
                    durationMs,
                    touchDetails
                )
            }
        } else {
            isSuccess = false
            feedbackMessage = "Not detected. Expected $expectedAction, but detected $detectedAction."

            if (!isPractice) {
                onLogEvent(
                    expectedAction,
                    detectedAction,
                    "incorrect",
                    attemptId,
                    durationMs,
                    touchDetails
                )
            }
            attemptId++
        }
    }

    LaunchedEffect(isSuccess, currentStepIndex) {
        if (isSuccess) {
            delay(800)
            isSuccess = false
            feedbackMessage = null
            attemptId = 1

            if (currentStepIndex < gestureTasks.size - 1) {
                currentStepIndex++
            } else {
                isAllCompleted = true
            }
        }
    }

    AdaptIQScreenContainer(scrollable = true) {
        AdaptIQSectionHeader(
            title = "Activity 5 of 13: Swipe / Gesture",
            subtitle = "Perform each guided gesture inside the touch area below."
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                currentStepIndex = 0
                attemptId = 1
                feedbackMessage = null
                isSuccess = false
                isAllCompleted = false
            }
        )

        if (!isAllCompleted && currentTask != null) {
            // Task Progress
            val progress = ((currentStepIndex + 1).toFloat() / gestureTasks.size.toFloat())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gesture ${currentStepIndex + 1} of ${gestureTasks.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                }
                AdaptIQProgressIndicator(
                    progress = progress
                )
            }

            // Task Instructions
            AdaptIQSurfaceCard(
                containerColor = SurfaceElevatedVariant,
                borderColor = AccentBlue.copy(alpha = 0.3f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = currentTask.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                    Text(
                        text = currentTask.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Visual Demonstration
            VisualDemonstration(
                action = currentTask.actionLabel,
                title = "Demonstration"
            )

            // Touch Action Area
            var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
            var dragEndOffset by remember { mutableStateOf(Offset.Zero) }
            var dragStartTime by remember { mutableLongStateOf(0L) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        color = if (isSuccess) StatusSuccess.copy(alpha = 0.2f) else SurfaceElevatedVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .pointerInput(currentStepIndex) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                handleGestureDetected(
                                    detectedAction = "double_tap",
                                    touchDetails = "x=${offset.x.toInt()},y=${offset.y.toInt()}",
                                    durationMs = 150L
                                )
                            },
                            onTap = { offset ->
                                if (currentTask.actionLabel != "double_tap") {
                                    handleGestureDetected(
                                        detectedAction = "tap",
                                        touchDetails = "x=${offset.x.toInt()},y=${offset.y.toInt()}",
                                        durationMs = 50L
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(currentStepIndex) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStartOffset = offset
                                dragStartTime = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                val durationMs = System.currentTimeMillis() - dragStartTime
                                val dx = dragEndOffset.x - dragStartOffset.x
                                val dy = dragEndOffset.y - dragStartOffset.y
                                val minSwipeDistance = 60f

                                val detectedAction = when {
                                    abs(dx) > abs(dy) && dx > minSwipeDistance -> "swipe_right"
                                    abs(dx) > abs(dy) && dx < -minSwipeDistance -> "swipe_left"
                                    abs(dy) > abs(dx) && dy > minSwipeDistance -> "swipe_down"
                                    abs(dy) > abs(dx) && dy < -minSwipeDistance -> "swipe_up"
                                    else -> "tap"
                                }

                                handleGestureDetected(
                                    detectedAction = detectedAction,
                                    touchDetails = "dx=${dx.toInt()},dy=${dy.toInt()}",
                                    durationMs = durationMs
                                )
                            },
                            onDrag = { change, _ ->
                                dragEndOffset = change.position
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isSuccess) "✓ SUCCESS" else "PERFORM ACTION HERE",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSuccess) StatusSuccess else TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (currentTask.actionLabel == "double_tap") "Double tap rapidly inside this box" else "Swipe in the indicated direction inside this box",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Feedback Banner
            feedbackMessage?.let { msg ->
                AdaptIQSurfaceCard(
                    borderColor = if (isSuccess) StatusSuccess.copy(alpha = 0.5f) else StatusError.copy(alpha = 0.5f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg,
                        color = if (isSuccess) StatusSuccess else StatusError,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (isAllCompleted) {
            AdaptIQSurfaceCard(
                borderColor = StatusSuccess.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✓ All Gestures Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have completed all 5 required gestures.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            AdaptIQPrimaryButton(
                text = "Continue to Session Dashboard",
                onClick = onActivityCompleted
            )
        }

        AdaptIQOutlinedButton(
            text = "Back",
            onClick = onBack
        )
    }
}
