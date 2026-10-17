package com.example.adaptiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

/**
 * Redesigned Consent Screen complying with research ethics protocol:
 * - Clear readability, distinct information groupings
 * - Adult consent vs Minor assent and guardian permission
 * - Deep navy surface cards with high contrast text
 */
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

    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Research Consent & Protocol",
                subtitle = "Protocol Approved: Adapt-IQ Cognitive-Motor Adaptive UI Study",
                badgeText = "ETHICS VERIFIED"
            )

            // Protocol Summary Card
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "1. Purpose & Protocol Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Adapt-IQ is an academic research platform studying touchscreen interaction dynamics, precision motor coordination, latency reflexes, and steering stability across 13 standardized smartphone activities.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "2. Privacy, Anonymity & Zero PII",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All data is strictly pseudonymous. No personal identifiers (names, phone numbers, email addresses, hardware serials, or GPS) are collected. Typed text during typing evaluations is never stored or transmitted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "3. Voluntary Participation & Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Participation is entirely voluntary. You may pause, resume, or exit the session at any time. Evaluation data is recorded locally on device and synchronized securely.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            // Participant Category Card
            AdaptIQSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SurfaceElevatedVariant
            ) {
                Text(
                    text = "Participant Category & Verification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Adult option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isAdult = true }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = isAdult,
                        onClick = { isAdult = true },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AccentTeal,
                            unselectedColor = BorderLight
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Adult Participant (18 years or older)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isAdult) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary
                    )
                }

                // Minor option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isAdult = false }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = !isAdult,
                        onClick = { isAdult = false },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AccentTeal,
                            unselectedColor = BorderLight
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Minor Participant (Under 18 — Requires Guardian)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!isAdult) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Checkboxes
                if (isAdult) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { adultConsentGiven = !adultConsentGiven }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = adultConsentGiven,
                            onCheckedChange = { adultConsentGiven = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentTeal,
                                checkmarkColor = TextDark,
                                uncheckedColor = BorderLight
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I have read the research protocol and voluntarily consent to participate in this study.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { guardianPermissionGiven = !guardianPermissionGiven }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = guardianPermissionGiven,
                                onCheckedChange = { guardianPermissionGiven = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentTeal,
                                    checkmarkColor = TextDark,
                                    uncheckedColor = BorderLight
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Parent / Guardian: I have reviewed the research protocol and grant permission for my child/ward to participate.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { minorAssentGiven = !minorAssentGiven }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = minorAssentGiven,
                                onCheckedChange = { minorAssentGiven = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AccentTeal,
                                    checkmarkColor = TextDark,
                                    uncheckedColor = BorderLight
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Minor Assent: I agree to perform the guided smartphone activities.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdaptIQPrimaryButton(
                    text = if (isAdult) "Confirm Adult Consent & Continue" else "Confirm Assent & Guardian Permission",
                    onClick = onAgree,
                    enabled = canProceed,
                    modifier = Modifier.fillMaxWidth()
                )

                AdaptIQOutlinedButton(
                    text = "Back to Welcome",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderSubtle,
                    textColor = TextSecondary
                )
            }
        }
    }
}
