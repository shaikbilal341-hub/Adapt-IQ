package com.example.adaptiq.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

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
    val context = LocalContext.current
    val trialDurationSeconds = 15
    var isPractice by remember { mutableStateOf(true) }
    var secondsRemaining by remember { mutableIntStateOf(trialDurationSeconds) }
    var isRunning by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var trialStartTime by remember { mutableLongStateOf(0L) }

    // Live sensor stability metrics
    var currentStabilityPct by remember { mutableFloatStateOf(95f) }
    var stabilitySamplesCount by remember { mutableIntStateOf(0) }
    var stabilitySum by remember { mutableFloatStateOf(0f) }

    // Register live sensor listener to calculate device tremor / stability index
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]
                val magnitude = sqrt((ax * ax) + (ay * ay) + (az * az))
                // Ideal rest gravity is ~9.81 m/s^2; tremor is deviation from 9.81
                val deviation = abs(magnitude - 9.80665f)
                val stability = (100f - (deviation * 15f)).coerceIn(40f, 99f)
                currentStabilityPct = stability

                if (isRunning) {
                    stabilitySamplesCount++
                    stabilitySum += stability
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Countdown timer for active holding trial
    LaunchedEffect(isRunning) {
        if (isRunning) {
            trialStartTime = SystemClock.elapsedRealtime()
            stabilitySamplesCount = 0
            stabilitySum = 0f

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
                val meanStability = if (stabilitySamplesCount > 0) stabilitySum / stabilitySamplesCount else currentStabilityPct

                if (!isPractice) {
                    onLogEvent(
                        "hold_phone_steady",
                        "held_steady",
                        "successful",
                        1,
                        duration,
                        String.format(Locale.US, "duration_s=%d,stability_pct=%.1f", trialDurationSeconds, meanStability)
                    )
                }
            }
        }
    }

    AdaptIQScreenContainer(scrollable = false) {
        AdaptIQSectionHeader(
            title = "Activity 1 of 13: Holding the Phone",
            subtitle = "Hold your phone steady in a natural sitting position to calibrate baseline posture and sensor stability."
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
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                containerColor = SurfaceElevatedVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRunning) "⏱️ 00:%02d".format(secondsRemaining) else if (isPractice) "Practice Mode" else "Ready",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isRunning) {
                            "Keep the phone steady and relaxed in your hand..."
                        } else if (isPractice) {
                            "Try holding your phone in your usual grip. Observe your live stability gauge below, then tap 'Start Data Collection' above when ready."
                        } else {
                            "Tap 'Start Official Collection' above to begin baseline recording."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Live Stability Gauge Card
                    AdaptIQSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceElevated,
                        borderColor = BorderSubtle
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Device Stability",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "%.1f%%".format(currentStabilityPct),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentStabilityPct > 85f) StatusSuccess else StatusWarning
                                )
                            }

                            AdaptIQProgressIndicator(
                                progress = (currentStabilityPct / 100f).coerceIn(0f, 1f),
                                color = if (currentStabilityPct > 85f) StatusSuccess else StatusWarning,
                                height = 10.dp
                            )

                            Text(
                                text = if (currentStabilityPct > 88f) "🟢 Steady Hold" else "🟡 Motion Detected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (!isRunning && isPractice) {
                AdaptIQSecondaryButton(
                    text = "Start Practice Hold Timer (15s)",
                    onClick = {
                        secondsRemaining = trialDurationSeconds
                        isRunning = true
                    }
                )
            }
        } else {
            AdaptIQSurfaceCard(
                borderColor = StatusSuccess.copy(alpha = 0.5f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "✓ Holding the Phone Completed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    val finalStability = if (stabilitySamplesCount > 0) stabilitySum / stabilitySamplesCount else currentStabilityPct
                    Text(
                        text = "Baseline posture and sensor stability recorded successfully (Mean stability: %.1f%%).".format(finalStability),
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
