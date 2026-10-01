package com.example.adaptiq.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Header banner clearly distinguishing Practice Mode vs Official Data Collection
 */
@Composable
fun PracticeHeader(
    isPractice: Boolean,
    onStartOfficialCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isPractice) MaterialTheme.colorScheme.tertiaryContainer else Color(0xFFE8F5E9)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isPractice) "⚡ PRACTICE / CALIBRATION PHASE" else "● OFFICIAL DATA COLLECTION ACTIVE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPractice) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF2E7D32)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPractice) Color(0xFFFFB74D) else Color(0xFF81C784)
                ) {
                    Text(
                        text = if (isPractice) "PRACTICE" else "RECORDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
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
                color = if (isPractice) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF1B5E20)
            )

            if (isPractice) {
                Button(
                    onClick = onStartOfficialCollection,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Start Data Collection", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
