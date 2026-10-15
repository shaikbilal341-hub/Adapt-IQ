package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConsentScreen(
    onAgree: () -> Unit,
    onBack: () -> Unit
) {
    var isAdult by remember { mutableStateOf(true) }
    var adultConsentGiven by remember { mutableStateOf(false) }
    var guardianPermissionGiven by remember { mutableStateOf(false) }
    var minorAssentGiven by remember { mutableStateOf(false) }

    val canProceed = if (isAdult) adultConsentGiven else (guardianPermissionGiven && minorAssentGiven)

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
                text = "Research Consent & Protocol",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Protocol Approved: Adapt-IQ Cognitive-Motor Adaptive UI Intelligence Study",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Purpose & Protocol Summary", fontWeight = FontWeight.Bold)
                    Text(
                        "Adapt-IQ is an academic research platform studying touchscreen interaction dynamics, precision motor coordination, latency reflexes, and steering stability across 13 standardized smartphone activities."
                    )

                    Text("2. Privacy, Anonymity & Zero PII", fontWeight = FontWeight.Bold)
                    Text(
                        "All data is strictly pseudonymous. No personal identifiers (names, phone numbers, email addresses, hardware serials, or GPS) are collected. Typed text during typing evaluations is never stored or transmitted."
                    )

                    Text("3. Voluntary Participation", fontWeight = FontWeight.Bold)
                    Text(
                        "Participation is entirely voluntary. You may pause, resume, or exit the session at any time. Incomplete sessions are stored locally without penalty."
                    )

                    Text("4. Durable Local Storage & Sync", fontWeight = FontWeight.Bold)
                    Text(
                        "Evaluation data is recorded to an encrypted/isolated local CSV file on device. Sessions are synchronized to the research backend server only when submitted or scheduled via background sync."
                    )
                }
            }

            // Participant Age Group & Permission
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Participant Category & Consent Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isAdult,
                            onClick = { isAdult = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adult Participant (18 years or older)", fontWeight = FontWeight.Medium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !isAdult,
                            onClick = { isAdult = false }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Minor Participant (Under 18 — Requires Guardian Permission)", fontWeight = FontWeight.Medium)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (isAdult) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = adultConsentGiven,
                                onCheckedChange = { adultConsentGiven = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "I have read the research protocol and voluntarily consent to participate.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = guardianPermissionGiven,
                                    onCheckedChange = { guardianPermissionGiven = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Parent/Guardian: I have reviewed the research information and grant permission for my child/ward to participate.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = minorAssentGiven,
                                    onCheckedChange = { minorAssentGiven = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Minor Assent: I agree to do the phone activities.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onAgree,
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isAdult) "Confirm Adult Consent & Continue" else "Confirm Assent & Guardian Permission",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Welcome")
            }
        }
    }
}

