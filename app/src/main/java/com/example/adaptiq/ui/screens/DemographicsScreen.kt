package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DemographicsScreen(
    onSubmitDemographics: (ageGroup: String, gender: String, participantType: String) -> Unit,
    onBack: () -> Unit
) {
    val ageGroups = listOf("Under 18", "18–25", "26–40", "41–60", "60+")
    val genders = listOf("Female", "Male", "Non-binary", "Prefer not to say")
    val participantTypes = listOf("ADULT", "CHILD", "UNKNOWN")

    var selectedAgeGroup by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var selectedParticipantType by remember { mutableStateOf("ADULT") }
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
            Text("Please provide the minimum required demographic information for this research study.")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Select Age Group:", fontWeight = FontWeight.Bold)
                    ageGroups.forEach { age ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedAgeGroup == age,
                                onClick = { selectedAgeGroup = age; errorMessage = null }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(age)
                        }
                    }

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
                    if (selectedAgeGroup.isBlank() || selectedGender.isBlank()) {
                        errorMessage = "Please select both your age group and gender to proceed."
                        return@Button
                    }
                    onSubmitDemographics(selectedAgeGroup, selectedGender, selectedParticipantType)
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
