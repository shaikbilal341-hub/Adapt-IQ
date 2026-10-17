package com.example.adaptiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.data.ActivityStatus
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

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

    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Conduct Research Session",
                subtitle = "Complete the 13 standardized cognitive-motor activities",
                badgeText = "BATTERY ACTIVE"
            )

            // Session Metadata Card
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Participant: $testId",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                        Text(
                            text = "Session: $sessionId",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = BadgeShape,
                        color = SurfaceElevatedVariant,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Text(
                            text = "$ageYears Y • $gender",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Overall Battery Progress Card
            AdaptIQSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceElevatedVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Study Battery Progress",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "$completedCount of $totalCount Completed",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (allCompleted) StatusSuccess else AccentTeal
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                AdaptIQProgressIndicator(progress = progress, height = 10.dp)
            }

            // Completion Banner (Shown when all 13 are finished)
            if (allCompleted) {
                AdaptIQSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = StatusSuccessContainer,
                    borderColor = StatusSuccess,
                    onClick = onViewCompletion
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🎉", fontSize = 24.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All 13 Activities Completed!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                            Text(
                                text = "Tap to view session summary and export research CSV",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "➔",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                    }
                }
            }

            Text(
                text = "Activities Battery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Activity Cards List
            activities.forEachIndexed { index, item ->
                ActivityCardItem(
                    index = index + 1,
                    title = item.title,
                    status = item.status,
                    onClick = { onSelectActivity(item.key) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AdaptIQOutlinedButton(
                text = "Back to Dashboard (Preserve Session)",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                borderColor = BorderSubtle,
                textColor = TextSecondary
            )
        }
    }
}

@Composable
private fun ActivityCardItem(
    index: Int,
    title: String,
    status: ActivityStatus,
    onClick: () -> Unit
) {
    val borderColor = when (status) {
        ActivityStatus.COMPLETED -> StatusSuccess.copy(alpha = 0.5f)
        ActivityStatus.IN_PROGRESS -> StatusWarning.copy(alpha = 0.5f)
        ActivityStatus.NOT_STARTED -> BorderSubtle
    }

    val (badgeText, badgeStatus) = when (status) {
        ActivityStatus.COMPLETED -> Pair("✓ DONE", "SUCCESS")
        ActivityStatus.IN_PROGRESS -> Pair("● RESUME", "WARNING")
        ActivityStatus.NOT_STARTED -> Pair("START", "NOT_STARTED")
    }

    AdaptIQSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        containerColor = SurfaceElevated,
        borderColor = borderColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevatedVariant,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$index",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = when (status) {
                            ActivityStatus.COMPLETED -> "Data recorded & verified"
                            ActivityStatus.IN_PROGRESS -> "Session in progress"
                            ActivityStatus.NOT_STARTED -> "Ready to begin evaluation"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            ActivityStatus.COMPLETED -> StatusSuccess
                            ActivityStatus.IN_PROGRESS -> StatusWarning
                            ActivityStatus.NOT_STARTED -> TextMuted
                        }
                    )
                }
            }

            AdaptIQStatusBadge(
                text = badgeText,
                status = badgeStatus
            )
        }
    }
}
