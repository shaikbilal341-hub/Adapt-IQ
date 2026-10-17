package com.example.adaptiq.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

/**
 * Step 1 Demographics Screen complying with Phase 4:
 * - Collects Age in Years (1-120) and Gender
 * - Age Group completely removed from active workflow
 * - Clear inputs, validation feedback, and Adapt-IQ visual styling
 */
@Composable
fun DemographicsScreen(
    onSubmitDemographics: (ageYears: Int, gender: String, participantType: String) -> Unit,
    onBack: () -> Unit
) {
    val genders = listOf("Female", "Male", "Non-binary", "Prefer not to say")

    var ageInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Participant Demographics",
                subtitle = "Enter completed age and select gender for study cohort normalization",
                badgeText = "STEP 1 OF 2"
            )

            // Demographics Input Card
            AdaptIQSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Demographic Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Age Field
                AdaptIQTextField(
                    value = ageInput,
                    onValueChange = {
                        ageInput = it
                        errorMessage = null
                    },
                    label = "Age in Completed Years (e.g. 25)",
                    placeholder = "Enter age between 1 and 120",
                    isError = errorMessage != null && ageInput.isBlank(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Gender Identification",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gender Selection Cards/Radios
                genders.forEach { gender ->
                    val isSelected = selectedGender == gender
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedGender = gender
                                errorMessage = null
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedGender = gender
                                errorMessage = null
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentTeal,
                                unselectedColor = BorderLight
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = gender,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }

            // Error Message
            errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StatusErrorContainer,
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $err",
                        color = StatusError,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdaptIQPrimaryButton(
                    text = "Start Research Session",
                    onClick = {
                        val ageInt = ageInput.toIntOrNull()
                        if (ageInt == null || ageInt < 1 || ageInt > 120) {
                            errorMessage = "Please enter a valid age in completed years (between 1 and 120)."
                            return@AdaptIQPrimaryButton
                        }
                        if (selectedGender.isBlank()) {
                            errorMessage = "Please select your gender identification to proceed."
                            return@AdaptIQPrimaryButton
                        }
                        val participantType = if (ageInt < 18) "CHILD" else "ADULT"
                        onSubmitDemographics(ageInt, selectedGender, participantType)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                AdaptIQOutlinedButton(
                    text = "Back",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderSubtle,
                    textColor = TextSecondary
                )
            }
        }
    }
}
