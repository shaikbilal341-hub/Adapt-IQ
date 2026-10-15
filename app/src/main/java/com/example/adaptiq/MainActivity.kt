package com.example.adaptiq

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.adaptiq.data.ActivityStatus
import com.example.adaptiq.sync.AdaptIqHttpClient
import com.example.adaptiq.sync.ParticipantSessionStartRequest
import com.example.adaptiq.sync.SyncQueueManager
import com.example.adaptiq.sync.SyncWorker
import com.example.adaptiq.ui.screens.*
import com.example.adaptiq.ui.theme.AdaptIQTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

private enum class Screen {
    WELCOME,
    CONSENT,
    DEMOGRAPHICS,
    HOME,
    HOLDING_PHONE,
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
    private var currentAgeYears by mutableIntStateOf(25)
    private var currentGender by mutableStateOf("Prefer not to say")

    // Activity Status tracking (13 Activities)
    private var holdingPhoneStatus by mutableStateOf(ActivityStatus.NOT_STARTED)
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
    private var gamingStatus by mutableStateOf(ActivityStatus.NOT_STARTED)

    private var lastCsvPath by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadLastCsv()

        val prefs = getSharedPreferences(RecordingService.PREFS_NAME, MODE_PRIVATE)
        val consentAccepted = prefs.getBoolean("consent_accepted", false)
        currentScreen = if (consentAccepted) Screen.HOME else Screen.WELCOME

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

        if (currentScreen != Screen.HOME && currentScreen != Screen.WELCOME) {
            BackHandler {
                currentScreen = Screen.HOME
            }
        }

        when (currentScreen) {
            Screen.WELCOME -> WelcomeScreen(
                onGetStarted = { currentScreen = Screen.CONSENT },
                onViewPrivacy = { currentScreen = Screen.CONSENT }
            )
            Screen.CONSENT -> ConsentScreen(
                onAgree = {
                    getSharedPreferences(RecordingService.PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean("consent_accepted", true)
                        .apply()
                    currentScreen = Screen.HOME
                },
                onBack = { currentScreen = Screen.WELCOME }
            )
            Screen.DEMOGRAPHICS -> DemographicsScreen(
                onSubmitDemographics = { ageYears, gender, participantType ->
                    currentAgeYears = ageYears
                    currentGender = gender
                    coroutineScope.launch {
                        startBackendPseudonymousSession(ageYears, gender, participantType)
                    }
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.HOME -> HomeScreen()
            Screen.HOLDING_PHONE -> HoldingThePhoneActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("holding_phone", exp, det, res, att, dur, touch) },
                onActivityCompleted = { holdingPhoneStatus = ActivityStatus.COMPLETED; currentScreen = Screen.HOME; checkSessionCompletion(); showToast("Holding the Phone Completed") },
                onBack = { currentScreen = Screen.HOME }
            )
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
            Screen.GAMING -> GamingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch, gMode ->
                    logActionEvent("gaming", exp, det, res, att, dur, touch, gMode)
                },
                onGameModeSelected = { selectedMode ->
                    updateCurrentActivity("gaming", selectedMode)
                },
                onActivityCompleted = {
                    gamingStatus = ActivityStatus.COMPLETED
                    currentScreen = Screen.HOME
                    checkSessionCompletion()
                    showToast("Gaming Interaction Completed")
                },
                onBack = { currentScreen = Screen.HOME }
            )
            Screen.PREVIOUS_SESSIONS -> PreviousSessionsScreen()
            Screen.EXPORT_DATASET -> ExportDatasetScreen()
        }
    }

    @Composable
    private fun HomeScreen() {
        val statuses = listOf(
            holdingPhoneStatus, tapPrecisionStatus, reactionTimeStatus, typingStatus, gestureStatus, scrollStatus,
            dragDropStatus, targetTrackingStatus, sliderControlStatus, visualSearchStatus,
            navigationStatus, multiTouchStatus, gamingStatus
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

            // 3 Primary Flow Actions prominently at the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { currentScreen = Screen.DEMOGRAPHICS },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (isSessionActive) "PARTICIPANT" else "CONDUCT TEST",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                OutlinedButton(
                    onClick = { currentScreen = Screen.PREVIOUS_SESSIONS },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PREVIOUS DATA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { currentScreen = Screen.EXPORT_DATASET },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("EXPORT DATA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // Pseudonymous Session Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isSessionActive) "PARTICIPANT ID: $testId (Age $currentAgeYears)" else "PARTICIPANT ID: $testId (Not Started)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSessionActive) "Gender: $currentGender • Active Research Session" else "Zero PII • Local-First Storage • Tap 'Conduct Test' to Begin",
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

            // Sync and Storage Status Card
            val savedCount = remember { datasetsDirectory().listFiles { file -> file.isFile && file.extension.equals("csv", true) }?.size ?: 0 }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local & Sync Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$savedCount Saved Local Sessions • Offline-First Queue Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { submitDatasetToAdmin() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("Sync All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (completedCount == totalActivities) {
                Button(
                    onClick = { submitDatasetToAdmin() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Submit Completed Dataset to Admin", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Research Activities Battery (13 Activities)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // 13 Activity Cards
            ActivityCard("1. Holding the Phone", holdingPhoneStatus) {
                navigateToActivity("holding_phone", Screen.HOLDING_PHONE)
            }
            ActivityCard("2. Tap Precision", tapPrecisionStatus) {
                navigateToActivity("tap_precision", Screen.TAP_PRECISION)
            }
            ActivityCard("3. Reaction Time", reactionTimeStatus) {
                navigateToActivity("reaction_time", Screen.REACTION_TIME)
            }
            ActivityCard("4. Typing", typingStatus) {
                navigateToActivity("typing", Screen.TYPING)
            }
            ActivityCard("5. Swipe / Gesture", gestureStatus) {
                navigateToActivity("gestures", Screen.GESTURES)
            }
            ActivityCard("6. Scrolling", scrollStatus) {
                navigateToActivity("scrolling", Screen.SCROLLING)
            }
            ActivityCard("7. Drag & Drop", dragDropStatus) {
                navigateToActivity("drag_drop", Screen.DRAG_DROP)
            }
            ActivityCard("8. Target Tracking", targetTrackingStatus) {
                navigateToActivity("target_tracking", Screen.TARGET_TRACKING)
            }
            ActivityCard("9. Slider Control", sliderControlStatus) {
                navigateToActivity("slider_control", Screen.SLIDER_CONTROL)
            }
            ActivityCard("10. Visual Search", visualSearchStatus) {
                navigateToActivity("visual_search", Screen.VISUAL_SEARCH)
            }
            ActivityCard("11. Navigation Task", navigationStatus) {
                navigateToActivity("navigation", Screen.NAVIGATION)
            }
            ActivityCard("12. Multi-Touch", multiTouchStatus) {
                navigateToActivity("multi_touch", Screen.MULTI_TOUCH)
            }
            ActivityCard("13. Gaming-style Interaction", gamingStatus) {
                navigateToActivity("gaming", Screen.GAMING)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    resetSessionForNextParticipant()
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
            Text("Previous Test Data", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("View and export previous research sessions and local CSV records.")
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
                                    OutlinedButton(onClick = { queueAndUploadFile(session.file) }) { Text("Upload") }
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
        loadLastCsv()
        val lastFile = lastCsvPath?.let { File(it) }
        val sessions = getSavedSessions()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Dataset Export & Submission", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Export local CSV dataset or submit completed research session to admin backend.")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Latest Recorded CSV Dataset", fontWeight = FontWeight.Bold)
                    if (lastFile != null && lastFile.exists()) {
                        val recordCount = runCatching {
                            var count = 0
                            lastFile.bufferedReader().use { reader ->
                                if (reader.readLine() != null) {
                                    while (reader.readLine() != null) {
                                        count++
                                    }
                                }
                            }
                            count
                        }.getOrDefault(0)
                        Text("File: ${lastFile.name}", style = MaterialTheme.typography.bodySmall)
                        Text("Size: ${lastFile.length() / 1024} KB • Records: $recordCount rows", style = MaterialTheme.typography.bodySmall)
                        Text("Storage: Local durable isolated storage", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    } else {
                        Text("No active session recording file found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Button(
                onClick = { shareLastCsv() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export Local CSV Dataset (Share Sheet)")
            }

            Button(
                onClick = { submitDatasetToAdmin() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Submit Dataset to Admin Server", fontWeight = FontWeight.Bold)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sync Queue Status", fontWeight = FontWeight.Bold)
                    Text("Total saved local session files: ${sessions.size}")
                    Text("Background retry policy: Exponential backoff with WorkManager")
                }
            }

            OutlinedButton(onClick = { currentScreen = Screen.HOME }, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Dashboard")
            }
        }
    }

    private suspend fun startBackendPseudonymousSession(ageYears: Int, gender: String, participantType: String) {
        val ageGroupStr = if (ageYears < 18) "Under 18" else if (ageYears <= 25) "18–25" else if (ageYears <= 40) "26–40" else if (ageYears <= 60) "41–60" else "60+"
        try {
            val response = withContext(Dispatchers.IO) {
                AdaptIqHttpClient.api.startParticipantSession(
                    ParticipantSessionStartRequest(
                        participantType = participantType,
                        condition = "SITTING",
                        ageGroup = ageGroupStr,
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

                holdingPhoneStatus = ActivityStatus.NOT_STARTED
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
                gamingStatus = ActivityStatus.NOT_STARTED

                val intent = Intent(this@MainActivity, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
                    putExtra(RecordingService.EXTRA_TEST_ID, testId)
                    putExtra(RecordingService.EXTRA_AGE_YEARS, currentAgeYears)
                    putExtra(RecordingService.EXTRA_GENDER, currentGender)
                    putExtra(RecordingService.EXTRA_SESSION_NAME, "Adapt-IQ Research Session")
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

        holdingPhoneStatus = ActivityStatus.NOT_STARTED
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
        gamingStatus = ActivityStatus.NOT_STARTED

        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
            putExtra(RecordingService.EXTRA_TEST_ID, testId)
            putExtra(RecordingService.EXTRA_AGE_YEARS, currentAgeYears)
            putExtra(RecordingService.EXTRA_GENDER, currentGender)
            putExtra(RecordingService.EXTRA_SESSION_NAME, "Adapt-IQ Research Session")
        }

        ContextCompat.startForegroundService(this, intent)
        currentScreen = Screen.HOME
    }

    private fun markActivityInProgress(activityName: String) {
        when (activityName) {
            "holding_phone" -> if (holdingPhoneStatus == ActivityStatus.NOT_STARTED) holdingPhoneStatus = ActivityStatus.IN_PROGRESS
            "tap_precision" -> if (tapPrecisionStatus == ActivityStatus.NOT_STARTED) tapPrecisionStatus = ActivityStatus.IN_PROGRESS
            "reaction_time" -> if (reactionTimeStatus == ActivityStatus.NOT_STARTED) reactionTimeStatus = ActivityStatus.IN_PROGRESS
            "typing" -> if (typingStatus == ActivityStatus.NOT_STARTED) typingStatus = ActivityStatus.IN_PROGRESS
            "gestures" -> if (gestureStatus == ActivityStatus.NOT_STARTED) gestureStatus = ActivityStatus.IN_PROGRESS
            "scrolling" -> if (scrollStatus == ActivityStatus.NOT_STARTED) scrollStatus = ActivityStatus.IN_PROGRESS
            "drag_drop" -> if (dragDropStatus == ActivityStatus.NOT_STARTED) dragDropStatus = ActivityStatus.IN_PROGRESS
            "target_tracking" -> if (targetTrackingStatus == ActivityStatus.NOT_STARTED) targetTrackingStatus = ActivityStatus.IN_PROGRESS
            "slider_control" -> if (sliderControlStatus == ActivityStatus.NOT_STARTED) sliderControlStatus = ActivityStatus.IN_PROGRESS
            "visual_search" -> if (visualSearchStatus == ActivityStatus.NOT_STARTED) visualSearchStatus = ActivityStatus.IN_PROGRESS
            "navigation" -> if (navigationStatus == ActivityStatus.NOT_STARTED) navigationStatus = ActivityStatus.IN_PROGRESS
            "multi_touch" -> if (multiTouchStatus == ActivityStatus.NOT_STARTED) multiTouchStatus = ActivityStatus.IN_PROGRESS
            "gaming" -> if (gamingStatus == ActivityStatus.NOT_STARTED) gamingStatus = ActivityStatus.IN_PROGRESS
        }
    }

    private fun navigateToActivity(activityName: String, screen: Screen) {
        if (!isSessionActive || sessionId.isBlank()) {
            showToast("Please complete participant details first to start research session")
            currentScreen = Screen.DEMOGRAPHICS
        } else {
            markActivityInProgress(activityName)
            updateCurrentActivity(activityName)
            currentScreen = screen
        }
    }

    private fun updateCurrentActivity(activityName: String, gameMode: String = "standard") {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_UPDATE_EVENT
            putExtra(RecordingService.EXTRA_ACTIVITY, activityName)
            putExtra(RecordingService.EXTRA_GAME_MODE, gameMode)
        }
        startService(intent)
    }

    private fun resetSessionForNextParticipant() {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        startService(intent)

        isSessionActive = false
        testId = "PARTICIPANT_TEMP"
        sessionId = ""
        sessionToken = ""
        currentAgeYears = 25
        currentGender = "Prefer not to say"

        holdingPhoneStatus = ActivityStatus.NOT_STARTED
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
        gamingStatus = ActivityStatus.NOT_STARTED

        getSharedPreferences(RecordingService.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean("consent_accepted", false)
            .apply()

        loadLastCsv()
        showToast("Session reset for next participant.")
        currentScreen = Screen.WELCOME
    }

    private fun ensureSessionStarted() {
        if (!isSessionActive || sessionId.isBlank()) {
            startFallbackLocalSession()
        }
    }

    private fun checkSessionCompletion() {
        val statuses = listOf(
            holdingPhoneStatus, tapPrecisionStatus, reactionTimeStatus, typingStatus, gestureStatus, scrollStatus,
            dragDropStatus, targetTrackingStatus, sliderControlStatus, visualSearchStatus,
            navigationStatus, multiTouchStatus, gamingStatus
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

    private fun queueAndUploadFile(file: File) {
        val queueManager = SyncQueueManager.getInstance(this)
        queueManager.queueCompletedSession(
            sessionId = file.nameWithoutExtension,
            testId = testId,
            csvFile = file,
            orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        )
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(this).enqueue(syncRequest)
        showToast("Session queued for upload. SyncWorker scheduled.")
    }

    private fun submitDatasetToAdmin() {
        val lastFile = lastCsvPath?.let { File(it) }
        val directory = datasetsDirectory()
        val filesToQueue = mutableListOf<File>()

        if (lastFile != null && lastFile.exists()) {
            filesToQueue.add(lastFile)
        } else if (directory.exists()) {
            directory.listFiles { file -> file.isFile && file.extension.equals("csv", true) }?.let {
                filesToQueue.addAll(it)
            }
        }

        if (filesToQueue.isEmpty()) {
            showToast("No CSV research recordings found to upload.")
            return
        }

        val queueManager = SyncQueueManager.getInstance(this)
        for (file in filesToQueue) {
            queueManager.queueCompletedSession(
                sessionId = file.nameWithoutExtension,
                testId = testId,
                csvFile = file,
                orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
            )
        }

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(this).enqueue(syncRequest)

        showToast("Queued ${filesToQueue.size} sessions for upload. Background sync initiated.")
    }

    private fun logActionEvent(
        activity: String,
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String,
        gameMode: String = "standard"
    ) {
        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_LOG_EVENT
            putExtra(RecordingService.EXTRA_ACTIVITY, activity)
            putExtra(RecordingService.EXTRA_GAME_MODE, gameMode)
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
            var recordCount = 0
            var recordedParticipantId = testId
            var recordedSessionId = file.nameWithoutExtension

            runCatching {
                file.bufferedReader().use { reader ->
                    val header = reader.readLine()
                    if (header != null) {
                        val firstData = reader.readLine()
                        if (firstData != null) {
                            recordCount = 1
                            val cols = firstData.split(",")
                            if (cols.size >= 5) {
                                val p = cols[3].replace("\"", "").trim()
                                val s = cols[4].replace("\"", "").trim()
                                if (p.isNotBlank()) recordedParticipantId = p
                                if (s.isNotBlank()) recordedSessionId = s
                            }
                            while (reader.readLine() != null) {
                                recordCount++
                            }
                        }
                    }
                }
            }

            SavedSession(
                file = file,
                sessionId = recordedSessionId,
                testId = recordedParticipantId,
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
