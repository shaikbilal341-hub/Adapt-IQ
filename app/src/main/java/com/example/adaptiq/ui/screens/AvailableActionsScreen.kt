package com.example.adaptiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

/**
 * Available Actions Screen:
 * - Genuine supported participant utilities and system actions
 * - Deep navy surface cards with clear action affordances
 */
@Composable
fun AvailableActionsScreen(
    isSessionActive: Boolean,
    pendingSyncCount: Int,
    onViewPrivacy: () -> Unit,
    onTriggerSync: () -> Unit,
    onTestConnectivity: () -> Unit,
    onResetSession: () -> Unit,
    onBack: () -> Unit
) {
    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Available Actions",
                subtitle = "Supported research system actions and participant utilities",
                badgeText = "SYSTEM UTILITIES"
            )

            // 1. Review Study Protocol & Privacy Policy
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevatedVariant,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "📜", fontSize = 18.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Research Protocol & Privacy Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Study objectives, ethics disclosures, and zero PII safeguards",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AdaptIQSecondaryButton(
                    text = "View Privacy & Study Information",
                    onClick = onViewPrivacy,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
                )
            }

            // 2. Offline Synchronization
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceElevatedVariant,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "☁️", fontSize = 18.sp)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Background Cloud Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Upload queued local recordings via WorkManager",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    AdaptIQStatusBadge(
                        text = "$pendingSyncCount Pending",
                        status = if (pendingSyncCount > 0) "WARNING" else "SUCCESS"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AdaptIQPrimaryButton(
                    text = "Synchronize Queued Sessions Now",
                    onClick = onTriggerSync,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
                )
            }

            // 3. Test Backend Connectivity
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevatedVariant,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🌐", fontSize = 18.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Backend Connectivity Diagnostic",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Verify connection to the Adapt-IQ research server",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AdaptIQSecondaryButton(
                    text = "Check Server Connectivity",
                    onClick = onTestConnectivity,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp
                )
            }

            // 4. Reset Active Session
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceElevatedVariant,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "👤", fontSize = 18.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "New Participant Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isSessionActive) {
                                "Active research session running. Conclude to prepare next participant."
                            } else {
                                "No active session. Starting Conduct Session collects fresh demographics."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                AdaptIQOutlinedButton(
                    text = "Conclude Session for Next Participant",
                    onClick = onResetSession,
                    enabled = isSessionActive,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (isSessionActive) StatusWarning else BorderSubtle,
                    textColor = if (isSessionActive) StatusWarning else TextMuted,
                    height = 46.dp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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
