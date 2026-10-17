package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyStudyInfoScreen(
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Privacy & Study Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Adapt-IQ Cognitive-Motor Adaptive UI Intelligence Study",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Study Objectives & Academic Protocol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adapt-IQ is an academic research platform studying touchscreen interaction dynamics, precision motor coordination, tremor stability, visual search latency, and steering reflexes across 13 standardized smartphone activities. Your contribution enables the modeling of assistive, user-adaptive interfaces.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "2. Strict Zero Personal Identifiable Information (Zero PII)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your privacy is guaranteed by design. Adapt-IQ collects strictly pseudonymous research data:\n" +
                                "• No participant names, phone numbers, or email addresses\n" +
                                "• No hardware serial numbers, IMEI, or Android ID\n" +
                                "• No advertising identifiers or device fingerprints\n" +
                                "• No GPS or fine location coordinates\n" +
                                "• No typed text content: Keystroke evaluations record only anonymous timing intervals and tap coordinates; actual typed characters are never stored or transmitted.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "3. Data Collection Scope & Sensor Recording",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "During activities, the application logs:\n" +
                                "• Interaction telemetry: touch coordinates, attempt durations, target hit/miss outcomes, and response latencies\n" +
                                "• Inertial sensors: standard IMU sensors (accelerometer, gyroscope, proximity) at standard sampling rates to assess device holding stability and orientation\n" +
                                "• Demographics: completed age in years and gender for statistical normalization across cohorts.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "4. Durable Local-First Storage & Cloud Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Research sessions are written to durable, encrypted/isolated local CSV datasets on your device. Sessions are synchronized to the authorized research backend server only when submitted or scheduled via secure background sync. You maintain the right to view and export your session CSV files at any time.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "5. Voluntary Participation & Right to Withdraw",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Participation in this study is entirely voluntary. You may pause, resume, or exit the session at any point without penalty. Incomplete or interrupted sessions are preserved locally and accurately marked as incomplete.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back", fontWeight = FontWeight.Bold)
            }
        }
    }
}
