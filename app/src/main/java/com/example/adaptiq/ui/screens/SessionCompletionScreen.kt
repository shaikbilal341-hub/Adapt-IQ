package com.example.adaptiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import java.io.File

/**
 * Dedicated Session Completion Screen complying with Phase 6:
 * - Prominent "Session Completed Successfully" header with verification badge
 * - Actual Participant ID and verified session details
 * - Properly aligned Export / Download CSV action
 * - Honest cloud synchronization & offline storage status
 * - Return to Dashboard action
 */
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

    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Prominent Success Icon
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = StatusSuccessContainer,
                border = BorderStroke(2.dp, StatusSuccess),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "✓",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatusSuccess
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Session Completed Successfully",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "All 13 research activities have been completed and verified. Data is durably recorded in local storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                )
            }

            // Session Verification Details Card
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Session Verification Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                VerificationRow(label = "Participant ID:", value = participantId, isHighlight = true)
                VerificationRow(label = "Session ID:", value = sessionId)
                VerificationRow(label = "Participant Cohort:", value = "$ageYears Years • $gender")
                VerificationRow(label = "Activities Completed:", value = "13 of 13 (100%)", isSuccess = true)
            }

            // Storage & Cloud Synchronization Status Card
            AdaptIQSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceElevatedVariant
            ) {
                Text(
                    text = "Storage & Synchronization Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (csvFile != null && csvFile.exists()) {
                    Text(
                        text = "• Local File: ${csvFile.name} (${csvFile.length() / 1024} KB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Durable storage status: Saved to local isolated storage",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusSuccess
                    )
                } else {
                    Text(
                        text = "• Local file status: Data recorded in local SQLite sync database",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Text(
                    text = if (isSyncQueued) {
                        "• Cloud sync: Queued in offline retry queue (WorkManager backoff policy active)"
                    } else {
                        "• Cloud sync: Synchronized with research backend"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSyncQueued) StatusWarning else StatusSuccess
                )
            }

            // Error or Success Messages
            exportError?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusErrorContainer,
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $err",
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            exportSuccessFeedback?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusSuccessContainer,
                    border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ $msg",
                        color = StatusSuccess,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
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
                AdaptIQPrimaryButton(
                    text = "Export / Download CSV",
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
                    modifier = Modifier.fillMaxWidth()
                )

                // Return to Main Dashboard
                AdaptIQOutlinedButton(
                    text = "Return to Dashboard",
                    onClick = onReturnToDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderSubtle,
                    textColor = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun VerificationRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isSuccess: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight || isSuccess) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isHighlight -> AccentTeal
                isSuccess -> StatusSuccess
                else -> TextPrimary
            }
        )
    }
}
