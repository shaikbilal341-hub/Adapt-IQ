package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay

private data class MixedTask(
    val id: String,
    val title: String,
    val instruction: String,
    val expectedAction: String
)

@Composable
fun MixedInteractionScreen(
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
    val tasks = remember {
        listOf(
            MixedTask("task_1", "Task 1: Open Target", "Find and tap the highlighted target card below.", "tap"),
            MixedTask("task_2", "Task 2: Scroll Down", "Scroll down through the section to reveal item details.", "scroll_down"),
            MixedTask("task_3", "Task 3: Swipe Left", "Swipe left across the card to view the next card.", "swipe_left"),
            MixedTask("task_4", "Task 4: Double Tap", "Double tap the highlighted target item.", "double_tap"),
            MixedTask("task_5", "Task 5: Type Word", "Type the word 'AdaptIQ' into the text input field below.", "typing_completed")
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var isPractice by remember { mutableStateOf(true) }
    var attemptId by remember { mutableIntStateOf(1) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var isAllCompleted by remember { mutableStateOf(false) }

    val currentTask = tasks.getOrNull(currentStepIndex)

    fun handleTaskPerformed(detectedAction: String, touchInfo: String) {
        val expectedAction = currentTask?.expectedAction ?: return

        if (detectedAction == expectedAction) {
            isSuccess = true
            feedbackMessage = "✓ Task Completed!"

            if (!isPractice) {
                onLogEvent(expectedAction, detectedAction, "successful", attemptId, 0L, touchInfo)
            }
        } else {
            isSuccess = false
            feedbackMessage = "Incorrect action. Expected $expectedAction, detected $detectedAction."

            if (!isPractice) {
                onLogEvent(expectedAction, detectedAction, "incorrect", attemptId, 0L, touchInfo)
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

            if (currentStepIndex < tasks.size - 1) {
                currentStepIndex++
            } else {
                isAllCompleted = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mixed Interaction",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Perform natural smartphone tasks in sequence to collect realistic interaction patterns.",
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
            val progress = ((currentStepIndex + 1).toFloat() / tasks.size.toFloat())
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Task ${currentStepIndex + 1} of ${tasks.size}",
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

            // Task Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentTask.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentTask.instruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Task Specific UI Target Area
            when (currentTask.expectedAction) {
                "tap" -> {
                    Button(
                        onClick = { handleTaskPerformed("tap", "target_button_tap") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("TAP THIS HIGHLIGHTED TARGET", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                "scroll_down" -> {
                    Button(
                        onClick = { handleTaskPerformed("scroll_down", "simulated_scroll_down") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("SWIPE / SCROLL DOWN HERE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                "swipe_left" -> {
                    Button(
                        onClick = { handleTaskPerformed("swipe_left", "simulated_swipe_left") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("SWIPE LEFT ACROSS THIS CARD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }

                "double_tap" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        handleTaskPerformed("double_tap", "double_tap_target")
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DOUBLE TAP THIS CARD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                "typing_completed" -> {
                    var inputText by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { newValue ->
                            inputText = newValue
                            if (!isPractice) {
                                onLogEvent("key_press", "key_press", "successful", attemptId, 0L, "len=${newValue.length}")
                            }
                            if (newValue.trim().equals("AdaptIQ", ignoreCase = true)) {
                                handleTaskPerformed("typing_completed", "text_matched")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Type 'AdaptIQ'") },
                        singleLine = true
                    )
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
                        modifier = Modifier.padding(12.dp),
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
                        text = "✓ All Mixed Tasks Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have completed all smartphone interaction tasks.",
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
