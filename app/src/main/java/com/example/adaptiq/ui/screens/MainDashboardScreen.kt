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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

/**
 * Main Participant Dashboard complying with Phase 3 & 6 specifications:
 * - Prominent "Adapt-IQ" title centered in the layout
 * - Exactly 3 primary action cards: Conduct Session, Previous Session History, Available Actions
 * - Large, accessible action cards with icons, titles, and supporting text
 * - Clean, spacious layout without activity catalogue, demographics, or export controls
 */
@Composable
fun MainDashboardScreen(
    onConductSession: () -> Unit,
    onPreviousSessions: () -> Unit,
    onAvailableActions: () -> Unit
) {
    AdaptIQScreenContainer {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Centered Prominent Branding
                Surface(
                    shape = BadgeShape,
                    color = SurfaceElevatedVariant,
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "RESEARCH DASHBOARD",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Adapt-IQ",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "AI Adaptive User Intelligence Research Platform",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Exactly 3 Primary Action Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Conduct Session (Primary teal emphasized action)
                    DashboardActionCard(
                        icon = "▶️",
                        title = "Conduct Session",
                        subtitle = "Start a new standardized research battery or resume an active session",
                        isPrimary = true,
                        onClick = onConductSession
                    )

                    // 2. Previous Session History (Secondary action)
                    DashboardActionCard(
                        icon = "📁",
                        title = "Previous Session History",
                        subtitle = "View recorded datasets, verify integrity, and export research CSVs",
                        isPrimary = false,
                        onClick = onPreviousSessions
                    )

                    // 3. Available Actions (System utilities)
                    DashboardActionCard(
                        icon = "⚙️",
                        title = "Available Actions",
                        subtitle = "Review research protocol, monitor cloud sync, and participant utilities",
                        isPrimary = false,
                        onClick = onAvailableActions
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardActionCard(
    icon: String,
    title: String,
    subtitle: String,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isPrimary) AccentTeal else BorderSubtle
    val containerBg = if (isPrimary) SurfaceElevatedVariant else SurfaceElevated
    val iconBg = if (isPrimary) AccentTeal.copy(alpha = 0.15f) else BackgroundSecondary

    AdaptIQSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        containerColor = containerBg,
        borderColor = borderColor,
        borderWidth = if (isPrimary) 1.5.dp else 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconBg,
                border = BorderStroke(1.dp, if (isPrimary) AccentTeal.copy(alpha = 0.5f) else BorderSubtle),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 22.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPrimary) AccentTeal else TextPrimary
                    )
                    if (isPrimary) {
                        Surface(
                            shape = BadgeShape,
                            color = AccentTeal.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        }
                    }
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Text(
                text = "➔",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) AccentTeal else TextMuted
            )
        }
    }
}
