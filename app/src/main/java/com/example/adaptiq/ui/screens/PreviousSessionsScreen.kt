package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class SessionHistoryRecord(
    val sessionId: String,
    val participantId: String,
    val formattedDateTime: String,
    val status: String, // COMPLETED, IN_PROGRESS, INTERRUPTED, PENDING, SYNCED
    val activityCompletionSummary: String,
    val syncStatus: String, // SYNCED, PENDING_UPLOAD, LOCAL_ONLY
    val csvFile: File?,
    val recordCount: Int
)

@Composable
fun PreviousSessionsScreen(
    sessions: List<SessionHistoryRecord>,
    onExportCsv: (File) -> Unit,
    onUploadSession: (SessionHistoryRecord) -> Unit,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Previous Session History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Historical research records saved in local durable storage and synchronization status.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No Previous Sessions Recorded",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed and in-progress sessions will appear here with full verification details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions, key = { it.sessionId }) { record ->
                        SessionHistoryCard(
                            record = record,
                            onExport = { record.csvFile?.let { onExportCsv(it) } },
                            onUpload = { onUploadSession(record) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    record: SessionHistoryRecord,
    onExport: () -> Unit,
    onUpload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.participantId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (record.status.uppercase()) {
                        "COMPLETED" -> Color(0xFFE8F5E9)
                        "SYNCED" -> Color(0xFFE3F2FD)
                        "IN_PROGRESS" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFEEEEEE)
                    }
                ) {
                    Text(
                        text = record.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status.uppercase()) {
                            "COMPLETED" -> Color(0xFF2E7D32)
                            "SYNCED" -> Color(0xFF1565C0)
                            "IN_PROGRESS" -> Color(0xFFE65100)
                            else -> Color(0xFF616161)
                        }
                    )
                }
            }

            Text(
                text = "Session: ${record.sessionId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Recorded: ${record.formattedDateTime}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Activities: ${record.activityCompletionSummary} • ${record.recordCount} CSV records",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            // Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (record.syncStatus.uppercase()) {
                        "SYNCED" -> "☁️ Cloud Synced"
                        "PENDING_UPLOAD" -> "⏳ Offline Queued (Sync Pending)"
                        else -> "💾 Local Storage Only"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (record.syncStatus.uppercase()) {
                        "SYNCED" -> Color(0xFF1565C0)
                        "PENDING_UPLOAD" -> Color(0xFFE65100)
                        else -> Color(0xFF2E7D32)
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (record.csvFile != null && record.csvFile.exists()) {
                    Button(
                        onClick = onExport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (record.syncStatus.uppercase() != "SYNCED" && record.csvFile != null) {
                    OutlinedButton(
                        onClick = onUpload,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sync to Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
