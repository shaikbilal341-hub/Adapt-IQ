package com.example.adaptiq.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.delay
import kotlin.random.Random

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Activity 2 of 13: Tap Precision",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tap the circular targets as they appear on the screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Interactive Target Box Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
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
                            .background(MaterialTheme.colorScheme.primary)
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
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            feedbackText?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
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
                    Text("✓ Tap Precision Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("All $totalTrials precision trials recorded successfully.")
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
