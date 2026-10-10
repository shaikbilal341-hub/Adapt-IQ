package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConsentScreen(
    onAgree: () -> Unit,
    onBack: () -> Unit
) {
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
                text = "Study Consent & Privacy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("1. Purpose of the Study", fontWeight = FontWeight.Bold)
                    Text("We are collecting anonymous interaction data to study touchscreen precision, reaction times, and gesture patterns for adaptive user interfaces.")

                    Text("2. Voluntary Participation", fontWeight = FontWeight.Bold)
                    Text("Participation is completely voluntary. You may pause, exit, or leave the study at any time without penalty.")

                    Text("3. Data Privacy & Confidentiality", fontWeight = FontWeight.Bold)
                    Text("No personal identifiers (such as names, phone numbers, emails, or device IDs) are collected. All data is associated with a random anonymous pseudonymous session identifier.")

                    Text("4. Local Storage & Sync", fontWeight = FontWeight.Bold)
                    Text("Research data is saved locally first and can be securely synchronized once completed.")
                }
            }

            Button(
                onClick = onAgree,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I Agree & Continue", fontWeight = FontWeight.Bold)
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
