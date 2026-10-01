package com.example.adaptiq.admin.ui.screens

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
fun AdminDashboardScreen(
    sessionCount: Int,
    onNavigateParticipants: () -> Unit,
    onNavigateSessions: () -> Unit,
    onNavigateExport: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Admin Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        // Stats Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RESEARCH PLATFORM STATUS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Total Uploaded Sessions: $sessionCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Backend Synchronization: REST API Connected", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = onNavigateSessions, modifier = Modifier.fillMaxWidth()) {
            Text("View Uploaded Sessions & Sensor Data", fontWeight = FontWeight.Bold)
        }

        Button(onClick = onNavigateParticipants, modifier = Modifier.fillMaxWidth()) {
            Text("View Participant Pseudonyms", fontWeight = FontWeight.Bold)
        }

        Button(onClick = onNavigateExport, modifier = Modifier.fillMaxWidth()) {
            Text("Authorized Dataset Export", fontWeight = FontWeight.Bold)
        }
    }
}
