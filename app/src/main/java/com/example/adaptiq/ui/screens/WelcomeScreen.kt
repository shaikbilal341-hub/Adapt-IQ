package com.example.adaptiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

/**
 * Redesigned Welcome Screen complying with Phase 2 & 4 specifications:
 * - Independent Get Started and Privacy & Study Information navigation actions
 * - Premium Adapt-IQ research branding with deep navy background and teal accents
 * - Clear zero-PII and voluntary participation highlights
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onViewPrivacy: () -> Unit
) {
    AdaptIQScreenContainer {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
                // Adapt-IQ Brand Header
                Surface(
                    shape = BadgeShape,
                    color = SurfaceElevatedVariant,
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "RESEARCH PLATFORM",
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
                    text = "AI Adaptive User Intelligence Research",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentTeal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Introduction & Trust Card
                AdaptIQSurfaceCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Welcome to the Study",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This study evaluates natural touchscreen motor dynamics, precision coordination, reaction latency, and steering stability across 13 guided smartphone activities to help model next-generation adaptive user interfaces.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = BadgeShape,
                            color = StatusSuccessContainer,
                            border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "🔒 ZERO PII",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }

                        Text(
                            text = "Pseudonymous by design. No names, phone numbers, or hardware IDs are collected. Keystroke activities record only touch timing, never typed characters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Primary & Secondary Navigation Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdaptIQPrimaryButton(
                        text = "Get Started",
                        onClick = onGetStarted,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdaptIQOutlinedButton(
                        text = "Privacy & Study Information",
                        onClick = onViewPrivacy,
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderSubtle,
                        textColor = TextSecondary
                    )
                }
            }
        }
    }
}
