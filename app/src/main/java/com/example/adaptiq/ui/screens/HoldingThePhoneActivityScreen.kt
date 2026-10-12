package com.example.adaptiq.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.delay

@Composable
fun HoldingThePhoneActivityScreen(
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
    val trialDurationSeconds = 15
    var isPractice by remember { mutableStateOf(true) }
    var secondsRemaining by remember { mutableIntStateOf(trialDurationSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var trialStartTime by remember { mutableLongStateOf(0L) }

    // Countdown timer for active holding trial
    LaunchedEffect(isRunning) {
        if (isRunning) {
            trialStartTime = SystemClock.elapsedRealtime()
            while (secondsRemaining > 0 && isRunning) {
                delay(1000)
                if (isRunning) {
                    secondsRemaining--
                }
            }
            if (secondsRemaining <= 0 && isRunning) {
                isRunning = false
                isCompleted = true
                val duration = SystemClock.elapsedRealtime() - trialStartTime
                if (!isPractice) {
                    onLogEvent(
                        "hold_phone_steady",
                        "held_steady",
                        "successful",
                        1,
                        duration,
                        "duration_s=$trialDurationSeconds"
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Activity 1 of 13: Holding the Phone",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Hold your phone steady in a natural sitting position to record baseline stability.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                secondsRemaining = trialDurationSeconds
                isRunning = true
                isCompleted = false
            }
        )

        if (!isCompleted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRunning) "⏱️ 00:%02d".format(secondsRemaining) else "Ready",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isRunning) "Keep the phone steady and relaxed..." else "Tap 'Start Official Collection' above to begin baseline recording.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!isRunning && isPractice) {
                Button(
                    onClick = {
                        isPractice = false
                        secondsRemaining = trialDurationSeconds
                        isRunning = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Practice / Stance Calibration", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("✓ Holding the Phone Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Baseline sensor calibration recorded successfully.")
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
