package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.data.ActivityStatus

data class ActivityItem(
    val key: String,
    val title: String,
    val status: ActivityStatus
)

@Composable
fun ConductSessionScreen(
    testId: String,
    sessionId: String,
    ageYears: Int,
    gender: String,
    activities: List<ActivityItem>,
    onSelectActivity: (activityKey: String) -> Unit,
    onViewCompletion: () -> Unit,
    onBack: () -> Unit
) {
    val completedCount = activities.count { it.status == ActivityStatus.COMPLETED }
    val totalCount = activities.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val allCompleted = completedCount == totalCount

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Conduct Research Session",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Session Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Participant ID: $testId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Session: $sessionId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Cohort: $ageYears Years Old • $gender",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            // Overall Progress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Study Battery Progress", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "$completedCount of $totalCount Completed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (allCompleted) {
                Button(
                    onClick = onViewCompletion,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text(
                        "✓ All 13 Activities Completed — View Session Summary & Export",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Activities Battery (13 Activities)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Activity Cards
            activities.forEach { item ->
                ActivityCard(
                    title = item.title,
                    status = item.status,
                    onClick = { onSelectActivity(item.key) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Dashboard (Preserve Session)")
            }
        }
    }
}

@Composable
private fun ActivityCard(
    title: String,
    status: ActivityStatus,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                ActivityStatus.COMPLETED -> Color(0xFFE8F5E9)
                ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                ActivityStatus.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (status) {
                        ActivityStatus.COMPLETED -> "Completed"
                        ActivityStatus.IN_PROGRESS -> "In Progress"
                        ActivityStatus.NOT_STARTED -> "Ready to Begin"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (status) {
                        ActivityStatus.COMPLETED -> Color(0xFF2E7D32)
                        ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                        ActivityStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (status) {
                    ActivityStatus.COMPLETED -> Color(0xFF81C784)
                    ActivityStatus.IN_PROGRESS -> Color(0xFFFFB74D)
                    ActivityStatus.NOT_STARTED -> MaterialTheme.colorScheme.primary
                }
            ) {
                Text(
                    text = when (status) {
                        ActivityStatus.COMPLETED -> "✓ DONE"
                        ActivityStatus.IN_PROGRESS -> "● RESUME"
                        ActivityStatus.NOT_STARTED -> "START"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (status == ActivityStatus.NOT_STARTED) Color.White else Color.Black
                )
            }
        }
    }
}
