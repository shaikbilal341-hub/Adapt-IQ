package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
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
    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Previous Session History",
                subtitle = "Historical research records saved in local durable storage and synchronization status",
                badgeText = "${sessions.size} RECORDED"
            )

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AdaptIQEmptyState(
                        title = "No Previous Sessions Recorded",
                        description = "Completed and in-progress sessions will appear here with full verification details and export capabilities.",
                        iconText = "📁"
                    )
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

            AdaptIQOutlinedButton(
                text = "Back to Dashboard",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                borderColor = BorderSubtle,
                textColor = TextSecondary
            )
        }
    }
}

@Composable
private fun SessionHistoryCard(
    record: SessionHistoryRecord,
    onExport: () -> Unit,
    onUpload: () -> Unit
) {
    AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.participantId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentTeal
            )

            AdaptIQStatusBadge(
                text = record.status.uppercase(),
                status = record.status
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Session: ${record.sessionId}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Text(
            text = "Recorded: ${record.formattedDateTime}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Text(
            text = "Activities: ${record.activityCompletionSummary} • ${record.recordCount} CSV records",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

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
                    "SYNCED" -> StatusSuccess
                    "PENDING_UPLOAD" -> StatusWarning
                    else -> StatusInfo
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (record.csvFile != null && record.csvFile.exists()) {
                AdaptIQPrimaryButton(
                    text = "Export CSV",
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
            }

            if (record.syncStatus.uppercase() != "SYNCED" && record.csvFile != null) {
                AdaptIQSecondaryButton(
                    text = "Sync to Cloud",
                    onClick = onUpload,
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
            }
        }
    }
}
