package com.example.adaptiq.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.theme.*

/**
 * Header banner clearly distinguishing Practice Mode vs Official Data Collection
 * styled with Adapt-IQ design tokens.
 */
@Composable
fun PracticeHeader(
    isPractice: Boolean,
    onStartOfficialCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isPractice) SurfaceElevatedVariant else Color(0xFF0F261F)
    val borderColor = if (isPractice) StatusWarning.copy(alpha = 0.5f) else StatusSuccess.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .background(containerBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPractice) "⚡ PRACTICE / CALIBRATION PHASE" else "● OFFICIAL DATA COLLECTION ACTIVE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPractice) StatusWarning else StatusSuccess
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPractice) StatusWarningContainer else StatusSuccessContainer,
                    border = BorderStroke(1.dp, if (isPractice) StatusWarning else StatusSuccess)
                ) {
                    Text(
                        text = if (isPractice) "PRACTICE" else "RECORDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPractice) StatusWarning else StatusSuccess
                    )
                }
            }

            Text(
                text = if (isPractice) {
                    "Practice the required action below. Your practice actions are NOT saved to the dataset."
                } else {
                    "Your natural actions are being recorded to the research dataset with synchronized sensors."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (isPractice) {
                AdaptIQPrimaryButton(
                    text = "Start Data Collection",
                    onClick = onStartOfficialCollection,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp
                )
            }
        }
    }
}
