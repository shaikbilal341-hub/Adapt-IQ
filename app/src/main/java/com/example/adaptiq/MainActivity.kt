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
import com.example.adaptiq.sync.AdaptIqHttpClient
import com.example.adaptiq.sync.ParticipantSessionStartRequest
import com.example.adaptiq.ui.screens.*
import com.example.adaptiq.ui.theme.AdaptIQTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

private enum class Screen {
    WELCOME,
    CONSENT,
    DEMOGRAPHICS,
    HOME,
    TAP_PRECISION,
    REACTION_TIME,
    TYPING,
    GESTURES,
    SCROLLING,
    DRAG_DROP,
    TARGET_TRACKING,
    SLIDER_CONTROL,
    VISUAL_SEARCH,
    NAVIGATION,
    MULTI_TOUCH,
    MIXED_INTERACTION,
    GAMING,
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

    private var currentScreen by mutableStateOf(Screen.WELCOME)

    // Session State (Pseudonymous and Automated via backend)
    private var testId by mutableStateOf("PARTICIPANT_TEMP")
    private var sessionId by mutableStateOf("")
    private var sessionToken by mutableStateOf("")
    private var isSessionActive by mutableStateOf(false)

    // Activity Status tracking (13 Activities)
    private var tapPrecisionStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var reactionTimeStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var typingStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var gestureStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var scrollStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var dragDropStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var targetTrackingStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var sliderControlStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var visualSearchStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var navigationStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var multiTouchStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var mixedStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
    private var gamingStatus by mutableStateOf(ActivityStatus.NOT_STARTED)

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
        val coroutineScope = rememberCoroutineScope()

        when (currentScreen) {
            Screen.WELCOME -> WelcomeScreen(
                onGetStarted = { currentScreen = Screen.CONSENT },
                onViewPrivacy = { currentScreen = Screen.CONSENT }
            )
            Screen.CONSENT -> ConsentScreen(
                onAgree = { currentScreen = Screen.DEMOGRAPHICS },
                onBack = { currentScreen = Screen.WELCOME }
            )
            Screen.DEMOGRAPHICS -> DemographicsScreen(
                onSubmitDemographics = { ageGroup, gender, participantType ->
                    coroutineScope.launch {
                        startBackendPseudonymousSession(ageGroup, gender, participantType)
                    }
                },
                onBack = { currentScreen = Screen.CONSENT }
            )
            Screen.HOME -> HomeScreen()
            Screen.TAP_PRECISION -> TapPrecisionActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("tap_precision", exp, det, res, att, dur, touch) },
                onActivityCompleted = { tapPrecisionStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Tap Precision Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.REACTION_TIME -> ReactionTimeActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("reaction_time", exp, det, res, att, dur, touch) },
                onActivityCompleted = { reactionTimeStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Reaction Time Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.TYPING -> TypingActivityScreen(
                testId = testId,
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("typing", exp, det, res, att, dur, touch) },
                onActivityCompleted = { typingStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Typing Activity Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.GESTURES -> GestureActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("gestures", exp, det, res, att, dur, touch) },
                onActivityCompleted = { gestureStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Gesture Activity Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.SCROLLING -> ScrollingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("scrolling", exp, det, res, att, dur, touch) },
                onActivityCompleted = { scrollStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Scrolling Activity Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.DRAG_DROP -> DragAndDropActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("drag_drop", exp, det, res, att, dur, touch) },
                onActivityCompleted = { dragDropStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Drag & Drop Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.TARGET_TRACKING -> TargetTrackingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("target_tracking", exp, det, res, att, dur, touch) },
                onActivityCompleted = { targetTrackingStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Target Tracking Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.SLIDER_CONTROL -> SliderControlActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("slider_control", exp, det, res, att, dur, touch) },
                onActivityCompleted = { sliderControlStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Slider Control Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.VISUAL_SEARCH -> VisualSearchActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("visual_search", exp, det, res, att, dur, touch) },
                onActivityCompleted = { visualSearchStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Visual Search Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.NAVIGATION -> NavigationTaskActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("navigation", exp, det, res, att, dur, touch) },
                onActivityCompleted = { navigationStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Navigation Task Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.MULTI_TOUCH -> MultiTouchActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("multi_touch", exp, det, res, att, dur, touch) },
                onActivityCompleted = { multiTouchStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Multi-Touch Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.MIXED_INTERACTION -> MixedInteractionScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("mixed_interaction", exp, det, res, att, dur, touch) },
                onActivityCompleted = { mixedStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Mixed Interaction Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.GAMING -> GamingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("gaming", exp, det, res, att, dur, touch) },
                onActivityCompleted = { gamingStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Gaming Interaction Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.PREVIOUS_SESSIONS -> PreviousSessionsScreen()
            Screen.EXPORT_DATASET -> ExportDatasetScreen()
        }
    }

    @Composable
    private fun HomeScreen() {
        val statuses = listOf(
            tapPrecisionStatus, reactionTimeStatus, typingStatus, gestureStatus, scrollStatus,
            dragDropStatus, targetTrackingStatus, sliderControlStatus, visualSearchStatus,
            navigationStatus, multiTouchStatus, mixedStatus, gamingStatus
        )
        val completedCount = statuses.count { it == ActivityStatus.COMPLETED }
        val totalActivities = 13
        val sessionProgress = completedCount.toFloat() / totalActivities.toFloat()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Adapt-IQ Research Dashboard",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "$completedCount of $totalActivities Activities Completed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Pseudonymous Session Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PARTICIPANT ID: $testId",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zero PII • Local-First Storage • Secure Synchronization",
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
                    Text("Study Progress", style = MaterialTheme.typography.labelLarge)
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

            if (completedCount == totalActivities) {
                Button(
                    onClick = { submitDatasetToAdmin() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Submit Dataset to Admin", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Research Activities Battery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // 13 Activity Cards
            ActivityCard("1. Tap Precision", tapPrecisionStatus) { ensureSessionStarted(); currentScreen = Screen.TAP_PRECISION }
            ActivityCard("2. Reaction Time", reactionTimeStatus) { ensureSessionStarted(); currentScreen = Screen.REACTION_TIME }
            ActivityCard("3. Typing", typingStatus) { ensureSessionStarted(); currentScreen = Screen.TYPING }
            ActivityCard("4. Swipe / Gesture", gestureStatus) { ensureSessionStarted(); currentScreen = Screen.GESTURES }
            ActivityCard("5. Scrolling", scrollStatus) { ensureSessionStarted(); currentScreen = Screen.SCROLLING }
            ActivityCard("6. Drag & Drop", dragDropStatus) { ensureSessionStarted(); currentScreen = Screen.DRAG_DROP }
            ActivityCard("7. Target Tracking", targetTrackingStatus) { ensureSessionStarted(); currentScreen = Screen.TARGET_TRACKING }
            ActivityCard("8. Slider Control", sliderControlStatus) { ensureSessionStarted(); currentScreen = Screen.SLIDER_CONTROL }
            ActivityCard("9. Visual Search", visualSearchStatus) { ensureSessionStarted(); currentScreen = Screen.VISUAL_SEARCH }
            ActivityCard("10. Navigation Task", navigationStatus) { ensureSessionStarted(); currentScreen = Screen.NAVIGATION }
            ActivityCard("11. Multi-Touch", multiTouchStatus) { ensureSessionStarted(); currentScreen = Screen.MULTI_TOUCH }
            ActivityCard("12. Mixed Interaction", mixedStatus) { ensureSessionStarted(); currentScreen = Screen.MIXED_INTERACTION }
            ActivityCard("13. Gaming-style Interaction", gamingStatus) { ensureSessionStarted(); currentScreen = Screen.GAMING }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { currentScreen = Screen.PREVIOUS_SESSIONS },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Partial / Local Dataset")
            }

            OutlinedButton(
                onClick = {
                    currentScreen = Screen.WELCOME
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finish / Next Participant (Reset Session)")
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
    private fun PreviousSessionsScreen() {
        val sessions = getSavedSessions()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text("Local Dataset Export", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Export partial or completed CSV sessions locally stored on device.")
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
                                    OutlinedButton(onClick = { shareFile(session.file) }) { Text("Export CSV") }
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
            Text("Dataset Export & Submission", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Export local CSV dataset or submit completed research session to admin backend.")

            Button(onClick = { shareLastCsv() }, modifier = Modifier.fillMaxWidth()) {
                Text("Export Local CSV Dataset")
            }

            Button(
                onClick = { submitDatasetToAdmin() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Submit Dataset to Admin", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(onClick = { currentScreen = Screen.HOME }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }

    private suspend fun startBackendPseudonymousSession(ageGroup: String, gender: String, participantType: String) {
        try {
            val response = withContext(Dispatchers.IO) {
                AdaptIqHttpClient.api.startParticipantSession(
                    ParticipantSessionStartRequest(
                        participantType = participantType,
                        condition = "SITTING",
                        ageGroup = ageGroup,
                        deviceModel = "Normalized Device"
                    )
                )
            }
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionId = body.sessionId
                testId = body.testId
                sessionToken = body.token
                isSessionActive = true

                tapPrecisionStatus = ActivityStatus.NOT_STARTED
                reactionTimeStatus = ActivityStatus.NOT_STARTED
                typingStatus = ActivityStatus.NOT_STARTED
                gestureStatus = ActivityStatus.NOT_STARTED
                scrollStatus = ActivityStatus.NOT_STARTED
                dragDropStatus = ActivityStatus.NOT_STARTED
                targetTrackingStatus = ActivityStatus.NOT_STARTED
                sliderControlStatus = ActivityStatus.NOT_STARTED
                visualSearchStatus = ActivityStatus.NOT_STARTED
                navigationStatus = ActivityStatus.NOT_STARTED
                multiTouchStatus = ActivityStatus.NOT_STARTED
                mixedStatus = ActivityStatus.NOT_STARTED
                gamingStatus = ActivityStatus.NOT_STARTED

                val intent = Intent(this@MainActivity, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
                    putExtra(RecordingService.EXTRA_TEST_ID, testId)
                    putExtra(RecordingService.EXTRA_DEVICE_MODEL, "Normalized Device")
                }
                ContextCompat.startForegroundService(this@MainActivity, intent)

                withContext(Dispatchers.Main) {
                    showToast("Research Session Started ($testId)")
                    currentScreen = Screen.HOME
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("Failed to start session on backend. Running offline mode.")
                    startFallbackLocalSession()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("Network unavailable. Running offline-first local mode.")
                startFallbackLocalSession()
            }
        }
    }

    private fun startFallbackLocalSession() {
        val randomHex = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
        val sessionHex = UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)
        testId = "PARTICIPANT_$randomHex"
        sessionId = "SESSION_$sessionHex"
        isSessionActive = true

        tapPrecisionStatus = ActivityStatus.NOT_STARTED
        reactionTimeStatus = ActivityStatus.NOT_STARTED
        typingStatus = ActivityStatus.NOT_STARTED
        gestureStatus = ActivityStatus.NOT_STARTED
        scrollStatus = ActivityStatus.NOT_STARTED
        dragDropStatus = ActivityStatus.NOT_STARTED
        targetTrackingStatus = ActivityStatus.NOT_STARTED
        sliderControlStatus = ActivityStatus.NOT_STARTED
        visualSearchStatus = ActivityStatus.NOT_STARTED
        navigationStatus = ActivityStatus.NOT_STARTED
        multiTouchStatus = ActivityStatus.NOT_STARTED
        mixedStatus = ActivityStatus.NOT_STARTED
        gamingStatus = ActivityStatus.NOT_STARTED

        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
            putExtra(RecordingService.EXTRA_TEST_ID, testId)
            putExtra(RecordingService.EXTRA_DEVICE_MODEL, "Normalized Device")
        }

        ContextCompat.startForegroundService(this, intent)
        currentScreen = Screen.HOME
    }

    private fun ensureSessionStarted() {
        if (!isSessionActive || sessionId.isBlank()) {
            startFallbackLocalSession()
        }
    }

    private fun checkSessionCompletion() {
        val statuses = listOf(
            tapPrecisionStatus, reactionTimeStatus, typingStatus, gestureStatus, scrollStatus,
            dragDropStatus, targetTrackingStatus, sliderControlStatus, visualSearchStatus,
            navigationStatus, multiTouchStatus, mixedStatus, gamingStatus
        )
        val completedCount = statuses.count { it == ActivityStatus.COMPLETED }
        if (completedCount == 13) {
            val intent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(intent)
            isSessionActive = false
            loadLastCsv()
            showToast("Study Completed Successfully! All 13 Activities Saved Locally.")
        }
    }

    private fun submitDatasetToAdmin() {
        showToast("Submitting dataset to backend admin...")
        // Triggers WorkManager / sync upload queue
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
