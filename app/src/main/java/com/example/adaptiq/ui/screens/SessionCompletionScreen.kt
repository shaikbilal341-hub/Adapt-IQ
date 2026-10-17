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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun SessionCompletionScreen(
    participantId: String,
    sessionId: String,
    ageYears: Int,
    gender: String,
    csvFile: File?,
    isSyncQueued: Boolean = true,
    onExportCsv: (File) -> Unit,
    onReturnToDashboard: () -> Unit
) {
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportSuccessFeedback by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Prominent Success Header
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "✓",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            Text(
                text = "Session Completed Successfully",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                textAlign = TextAlign.Center
            )

            Text(
                text = "All 13 research activities have been completed and verified. Data is durably recorded in local storage.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Session Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Session Verification Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Participant ID:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = participantId,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Session ID:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = sessionId,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Participant Age:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$ageYears Years", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gender:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = gender, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Activities Completed:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "13 of 13 (100%)",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Sync and Storage Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Storage & Synchronization Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (csvFile != null && csvFile.exists()) {
                        Text(
                            text = "• Local File: ${csvFile.name} (${csvFile.length() / 1024} KB)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Durable storage status: Saved to local isolated storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Text(
                            text = "• Local file status: Data recorded in local SQLite sync database",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Text(
                        text = if (isSyncQueued) {
                            "• Cloud sync: Queued in offline retry queue (WorkManager backoff policy active)"
                        } else {
                            "• Cloud sync: Synchronized with research backend"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSyncQueued) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }
            }

            // Error or success messages
            exportError?.let { err ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            exportSuccessFeedback?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Export / Download CSV Action
                Button(
                    onClick = {
                        exportError = null
                        exportSuccessFeedback = null
                        if (csvFile != null && csvFile.exists()) {
                            try {
                                onExportCsv(csvFile)
                                exportSuccessFeedback = "CSV export launched successfully for $participantId."
                            } catch (e: Exception) {
                                exportError = "Failed to export CSV: ${e.message}"
                            }
                        } else {
                            exportError = "Local CSV dataset file is not available on disk yet. Data is safely stored in local queue."
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Export / Download CSV",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Return to Main Dashboard
                OutlinedButton(
                    onClick = onReturnToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Return to Dashboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
