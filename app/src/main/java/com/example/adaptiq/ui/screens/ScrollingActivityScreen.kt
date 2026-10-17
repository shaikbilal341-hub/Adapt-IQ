package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.delay

private data class ScrollTask(
    val id: String,
    val title: String,
    val instruction: String,
    val actionLabel: String
)

@Composable
fun ScrollingActivityScreen(
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
    val scrollTasks = remember {
        listOf(
            ScrollTask("scroll_down", "Scroll Down", "Scroll down through the vertical list content.", "scroll_down"),
            ScrollTask("scroll_up", "Scroll Up", "Scroll back up toward the top of the list.", "scroll_up"),
            ScrollTask("scroll_left", "Scroll Left", "Scroll left across the horizontal card row.", "scroll_left"),
            ScrollTask("scroll_right", "Scroll Right", "Scroll right across the horizontal card row.", "scroll_right"),
            ScrollTask("double_tap", "Double Tap", "Double tap the highlighted card below.", "double_tap")
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isPractice by remember { mutableStateOf(true) }
    var attemptId by remember { mutableIntStateOf(1) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isAllCompleted by remember { mutableStateOf(false) }

    val currentTask = scrollTasks.getOrNull(currentStepIndex)

    fun handleScrollDetected(detectedAction: String, touchInfo: String) {
        val expectedAction = currentTask?.actionLabel ?: return

        if (detectedAction == expectedAction) {
            isSuccess = true
            feedbackMessage = "✓ Detected $detectedAction successfully!"

            if (!isPractice) {
                onLogEvent(
                    expectedAction,
                    detectedAction,
                    "successful",
                    attemptId,
                    0L,
                    touchInfo
                )
            }
        } else {
            isSuccess = false
            feedbackMessage = "Incorrect action. Expected $expectedAction, but detected $detectedAction."

            if (!isPractice) {
                onLogEvent(
                    expectedAction,
                    detectedAction,
                    "incorrect",
                    attemptId,
                    0L,
                    touchInfo
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

            if (currentStepIndex < scrollTasks.size - 1) {
                currentStepIndex++
            } else {
                isAllCompleted = true
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    // Monitor Vertical Scroll
    var lastVerticalOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress && currentTask != null) {
            val currentOffset = lazyListState.firstVisibleItemScrollOffset + (lazyListState.firstVisibleItemIndex * 200)
            val diff = currentOffset - lastVerticalOffset
            lastVerticalOffset = currentOffset

            if (diff > 15) {
                handleScrollDetected("scroll_down", "dy=$diff")
            } else if (diff < -15) {
                handleScrollDetected("scroll_up", "dy=$diff")
            }
        }
    }

    // Monitor Horizontal Scroll
    var lastHorizontalOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(horizontalScrollState.isScrollInProgress) {
        if (horizontalScrollState.isScrollInProgress && currentTask != null) {
            val currentOffset = horizontalScrollState.value
            val diff = currentOffset - lastHorizontalOffset
            lastHorizontalOffset = currentOffset

            if (diff > 15) {
                handleScrollDetected("scroll_right", "dx=$diff")
            } else if (diff < -15) {
                handleScrollDetected("scroll_left", "dx=$diff")
            }
        }
    }

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 6 of 13: Scrolling",
            subtitle = "Interact with the content below to perform each required scroll action."
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
            val progress = ((currentStepIndex + 1).toFloat() / scrollTasks.size.toFloat())
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scroll Task ${currentStepIndex + 1} of ${scrollTasks.size}",
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

            // Instructions
            AdaptIQSurfaceCard(
                containerColor = SurfaceElevatedVariant,
                borderColor = AccentBlue.copy(alpha = 0.3f),
                contentPadding = PaddingValues(14.dp)
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

            // Scrollable Area Content
            if (currentTask.actionLabel == "scroll_left" || currentTask.actionLabel == "scroll_right") {
                // Horizontal Scrollable Container
                AdaptIQSurfaceCard(
                    containerColor = SurfaceElevatedVariant,
                    borderColor = BorderSubtle,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Horizontal Scroll Container",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            (1..10).forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp, 80.dp)
                                        .background(SurfaceElevated, shape = RoundedCornerShape(12.dp))
                                        .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Card $index",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (currentTask.actionLabel == "double_tap") {
                // Double Tap Target Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            color = SurfaceElevatedVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(2.dp, AccentTeal, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    handleScrollDetected("double_tap", "double_tap_card")
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DOUBLE TAP THIS CARD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Vertical Scrollable List
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            color = SurfaceElevatedVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(20) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceElevated, RoundedCornerShape(10.dp))
                                .border(1.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Research Content Item #${index + 1}",
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Scroll Me",
                                    color = AccentTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                        text = "✓ All Scrolling Tasks Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have successfully completed all scrolling actions.",
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
