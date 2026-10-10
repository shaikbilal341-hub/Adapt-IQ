package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun DemographicsScreen(
    onSubmitDemographics: (ageYears: Int, gender: String, participantType: String) -> Unit,
    onBack: () -> Unit
) {
    val genders = listOf("Female", "Male", "Non-binary", "Prefer not to say")

    var ageInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Participant Information",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Please enter your age in completed years and select your gender.")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it; errorMessage = null },
                        label = { Text("Age in Years (e.g. 25)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Gender:", fontWeight = FontWeight.Bold)
                    genders.forEach { gender ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender; errorMessage = null }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(gender)
                        }
                    }
                }
            }

            errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val ageInt = ageInput.toIntOrNull()
                    if (ageInt == null || ageInt < 1 || ageInt > 120) {
                        errorMessage = "Please enter a valid age in years (between 1 and 120)."
                        return@Button
                    }
                    if (selectedGender.isBlank()) {
                        errorMessage = "Please select your gender to proceed."
                        return@Button
                    }
                    val participantType = if (ageInt < 18) "CHILD" else "ADULT"
                    onSubmitDemographics(ageInt, selectedGender, participantType)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Research Session", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
