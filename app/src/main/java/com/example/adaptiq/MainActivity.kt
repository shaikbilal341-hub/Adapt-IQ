package com.example.adaptiq

import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.adaptiq.data.ActivityStatus
import com.example.adaptiq.ui.screens.*
import com.example.adaptiq.ui.theme.AdaptIQTheme
import java.io.File
import java.util.Locale
import java.util.UUID

private enum class Screen {
    HOME,
    TEST_SETUP,
    TYPING,
    GESTURES,
    SCROLLING,
    GAMING,
    MIXED_INTERACTION,
    PREVIOUS_SESSIONS,
    EXPORT_DATASET
}

private data class SavedSession(
    val file: File,
    val sessionId: String,
    val testId: String,
    val records: Int
)

class MainActivity : ComponentActivity() {

    private var currentScreen by mutableStateOf(Screen.HOME)

    // Session State
    private var testId by mutableStateOf("TEST_001")
    private var sessionId by mutableStateOf("")
    private var isSessionActive by mutableStateOf(false)

    // Activity Status tracking
    private var typingStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var gestureStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var scrollStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var gamingStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var mixedStatus by mutableStateOf(ActivityStatus.NOT_STARTED)

    private var lastCsvPath by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadLastCsv()

        setContent {
            AdaptIQTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdaptIQAppRouter()
                }
            }
        }
    }

    @Composable
    private fun AdaptIQAppRouter() {
        when (currentScreen) {
            Screen.HOME -> HomeScreen()
            Screen.TEST_SETUP -> TestSetupScreen()
            Screen.TYPING -> TypingActivityScreen(
                testId = testId,
                onLogEvent = { exp, det, res, att, dur, touch ->
                    logActionEvent("typing", exp, det, res, att, dur, touch)
                },
                onActivityCompleted = {
                    typingStatus = ActivityStatus.COMPLETED
                    currentScreen = Screen.HOME
                    showToast("Typing Activity Completed")
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.GESTURES -> GestureActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch ->
                    logActionEvent("gestures", exp, det, res, att, dur, touch)
                },
                onActivityCompleted = {
                    gestureStatus = ActivityStatus.COMPLETED
                    currentScreen = Screen.HOME
                    showToast("Gesture Activity Completed")
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.SCROLLING -> ScrollingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch ->
                    logActionEvent("scrolling", exp, det, res, att, dur, touch)
                },
                onActivityCompleted = {
                    scrollStatus = ActivityStatus.COMPLETED
                    currentScreen = Screen.HOME
                    showToast("Scrolling Activity Completed")
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.GAMING -> GamingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch ->
                    logActionEvent("gaming", exp, det, res, att, dur, touch)
                },
                onActivityCompleted = {
                    gamingStatus = ActivityStatus.COMPLETED
                    currentScreen = Screen.HOME
                    showToast("Gaming Activity Completed")
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.MIXED_INTERACTION -> MixedInteractionScreen(
                onLogEvent = { exp, det, res, att, dur, touch ->
                    logActionEvent("mixed_interaction", exp, det, res, att, dur, touch)
                },
                onActivityCompleted = {
                    mixedStatus = ActivityStatus.COMPLETED
                    currentScreen = Screen.HOME
                    showToast("Mixed Interaction Completed")
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.PREVIOUS_SESSIONS -> PreviousSessionsScreen()
            Screen.EXPORT_DATASET -> ExportDatasetScreen()
        }
    }

    @Composable
    private fun HomeScreen() {
        val completedCount = listOf(typingStatus, gestureStatus, scrollStatus, gamingStatus, mixedStatus)
            .count { it == ActivityStatus.COMPLETED }
        val totalActivities = 5
        val sessionProgress = completedCount.toFloat() / totalActivities.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Adapt-IQ",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "User Interaction Data Collection",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Test Session Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TEST ID: $testId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sensors: ${availableSensorCount()}/3 Ready • Storage: Local CSV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            // Overall Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Session Progress", style = MaterialTheme.typography.labelLarge)
                    Text("$completedCount / $totalActivities Completed", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { sessionProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (!isSessionActive) {
                Button(
                    onClick = {
                        startNewSession()
                        currentScreen = Screen.TEST_SETUP
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("START SESSION", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Research Activities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Activity Cards
            ActivityCard("Typing", typingStatus) {
                ensureSessionStarted()
                typingStatus = ActivityStatus.IN_PROGRESS
                currentScreen = Screen.TYPING
            }

            ActivityCard("Gestures", gestureStatus) {
                ensureSessionStarted()
                gestureStatus = ActivityStatus.IN_PROGRESS
                currentScreen = Screen.GESTURES
            }

            ActivityCard("Scrolling", scrollStatus) {
                ensureSessionStarted()
                scrollStatus = ActivityStatus.IN_PROGRESS
                currentScreen = Screen.SCROLLING
            }

            ActivityCard("Gaming", gamingStatus) {
                ensureSessionStarted()
                gamingStatus = ActivityStatus.IN_PROGRESS
                currentScreen = Screen.GAMING
            }

            ActivityCard("Mixed Interaction", mixedStatus) {
                ensureSessionStarted()
                mixedStatus = ActivityStatus.IN_PROGRESS
                currentScreen = Screen.MIXED_INTERACTION
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { currentScreen = Screen.PREVIOUS_SESSIONS },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Previous Sessions")
            }

            OutlinedButton(
                onClick = { currentScreen = Screen.EXPORT_DATASET },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Dataset")
            }
        }
    }

    @Composable
    private fun ActivityCard(
        title: String,
        status: ActivityStatus,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (status) {
                    ActivityStatus.COMPLETED -> Color(0xFFE8F5E9)
                    ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                    ActivityStatus.NOT_STARTED -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = when (status) {
                            ActivityStatus.COMPLETED -> "Completed"
                            ActivityStatus.IN_PROGRESS -> "In Progress"
                            ActivityStatus.NOT_STARTED -> "Not Started"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (status) {
                            ActivityStatus.COMPLETED -> Color(0xFF2E7D32)
                            ActivityStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onTertiaryContainer
                            ActivityStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (status) {
                        ActivityStatus.COMPLETED -> Color(0xFF81C784)
                        ActivityStatus.IN_PROGRESS -> Color(0xFFFFB74D)
                        ActivityStatus.NOT_STARTED -> Color(0xFFE0E0E0)
                    }
                ) {
                    Text(
                        text = when (status) {
                            ActivityStatus.COMPLETED -> "✓ DONE"
                            ActivityStatus.IN_PROGRESS -> "● ACTIVE"
                            ActivityStatus.NOT_STARTED -> "START"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }

    @Composable
    private fun TestSetupScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Test Session Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Enter anonymous research test identifier.")

            OutlinedTextField(
                value = testId,
                onValueChange = { testId = it.trim() },
                label = { Text("Test ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { currentScreen = Screen.HOME },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm & Continue")
            }
        }
    }

    @Composable
    private fun PreviousSessionsScreen() {
        val sessions = getSavedSessions()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("Previous Sessions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Text("No saved CSV dataset sessions found.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { session ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(session.sessionId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${session.testId} • ${session.records} Records")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { shareFile(session.file) }) { Text("Export") }
                                    OutlinedButton(onClick = { session.file.delete(); showToast("Deleted") }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { currentScreen = Screen.HOME }, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }

    @Composable
    private fun ExportDatasetScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Export Dataset", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("CSV dataset file export options")

            Button(onClick = { shareLastCsv() }, modifier = Modifier.fillMaxWidth()) {
                Text("Share Latest CSV Session")
            }

            OutlinedButton(onClick = { currentScreen = Screen.HOME }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }

    private fun startNewSession() {
        sessionId = "SESSION_" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        isSessionActive = true

        typingStatus = ActivityStatus.NOT_STARTED
        gestureStatus = ActivityStatus.NOT_STARTED
        scrollStatus = ActivityStatus.NOT_STARTED
        gamingStatus = ActivityStatus.NOT_STARTED
        mixedStatus = ActivityStatus.NOT_STARTED

        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
            putExtra(RecordingService.EXTRA_TEST_ID, testId)
            putExtra(RecordingService.EXTRA_DEVICE_MODEL, "${Build.MANUFACTURER} ${Build.MODEL}")
        }

        ContextCompat.startForegroundService(this, intent)
    }

    private fun ensureSessionStarted() {
        if (!isSessionActive) {
            startNewSession()
        }
    }

    private fun logActionEvent(
        activity: String,
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String
    ) {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_LOG_EVENT
            putExtra(RecordingService.EXTRA_ACTIVITY, activity)
            putExtra(RecordingService.EXTRA_EXPECTED_ACTION, expectedAction)
            putExtra(RecordingService.EXTRA_DETECTED_ACTION, detectedAction)
            putExtra(RecordingService.EXTRA_RESULT, result)
            putExtra(RecordingService.EXTRA_ATTEMPT_ID, attemptId)
            putExtra(RecordingService.EXTRA_DURATION, durationMs)
            putExtra(RecordingService.EXTRA_TOUCH_INFO, touchInfo)
            putExtra(RecordingService.EXTRA_ORIENTATION, if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait")
        }
        startService(intent)
    }

    private fun sensorAvailability(): List<Boolean> {
        val manager = getSystemService(SENSOR_SERVICE) as SensorManager
        return listOf(
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
            manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null,
            manager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
        )
    }

    private fun availableSensorCount(): Int {
        return sensorAvailability().count { it }
    }

    private fun datasetsDirectory(): File {
        return getExternalFilesDir("datasets") ?: File(filesDir, "datasets")
    }

    private fun getSavedSessions(): List<SavedSession> {
        val directory = datasetsDirectory()
        if (!directory.exists()) return emptyList()

        val files = directory.listFiles { file ->
            file.isFile && file.extension.equals("csv", true)
        } ?: return emptyList()

        return files.map { file ->
            val lines = runCatching { file.readLines() }.getOrDefault(emptyList())
            val recordCount = (lines.size - 1).coerceAtLeast(0)
            SavedSession(
                file = file,
                sessionId = file.nameWithoutExtension,
                testId = testId,
                records = recordCount
            )
        }.sortedByDescending { it.file.lastModified() }
    }

    private fun loadLastCsv() {
        lastCsvPath = getSharedPreferences(RecordingService.PREFS_NAME, MODE_PRIVATE)
            .getString(RecordingService.KEY_LAST_CSV, null)
    }

    private fun shareLastCsv() {
        loadLastCsv()
        val path = lastCsvPath
        if (path.isNullOrBlank()) {
            showToast("No recorded CSV found")
            return
        }
        val file = File(path)
        if (!file.exists()) {
            showToast("CSV file no longer exists")
            return
        }
        shareFile(file)
    }

    private fun shareFile(file: File) {
        if (!file.exists()) {
            showToast("File not found")
            return
        }
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share AdaptIQ Dataset"))
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
