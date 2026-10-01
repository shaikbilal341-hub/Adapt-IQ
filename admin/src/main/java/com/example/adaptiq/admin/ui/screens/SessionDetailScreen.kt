package com.example.adaptiq.admin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.adaptiq.admin.sync.SessionDetailDto

@Composable
fun SessionDetailScreen(
    sessionDetail: SessionDetailDto?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Session Detail Inspector",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        } else if (sessionDetail != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Session ID: ${sessionDetail.sessionId}", fontWeight = FontWeight.Bold)
                    Text("Test ID: ${sessionDetail.testId}")
                    Text("Orientation: ${sessionDetail.orientation}")
                    Text("Status: ${sessionDetail.status}")
                    Text("Action Events Recorded: ${sessionDetail.events.size}")
                    Text("Sensor Samples Recorded: ${sessionDetail.sensorSamples.size}")
                }
            }

            Text("Action Events Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessionDetail.events) { event ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Activity: ${event.activity} • Action: ${event.detectedAction}", fontWeight = FontWeight.Bold)
                            Text("Result: ${event.result} • Attempt: ${event.attemptId}")
                            Text("Timestamp: ${event.timestamp}")
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sessions")
        }
    }
}
