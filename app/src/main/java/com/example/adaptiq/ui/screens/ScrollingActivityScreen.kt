package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
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
import com.example.adaptiq.ui.components.PracticeHeader
import com.example.adaptiq.ui.components.VisualDemonstration
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Activity 6 of 13: Scrolling",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Interact with the content below to perform each required scroll action.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scroll Task ${currentStepIndex + 1} of ${scrollTasks.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = currentTask.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentTask.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Horizontal Scroll Container",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizontalScrollState),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            (1..10).forEach { index ->
                                Card(
                                    modifier = Modifier
                                        .size(110.dp, 80.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Card $index",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(20) { index ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Research Content Item #${index + 1}", fontWeight = FontWeight.Medium)
                                Text("Scroll Me", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Feedback Banner
            feedbackMessage?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = msg,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (isAllCompleted) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✓ All Scrolling Tasks Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have successfully completed all scrolling actions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onActivityCompleted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue to Session Dashboard", fontWeight = FontWeight.Bold)
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
