package com.example.adaptiq

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.adaptiq.data.ActivityStatus
import com.example.adaptiq.data.ConsentManager
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class Screen {
    WELCOME,
    PRIVACY_INFO,
    CONSENT,
    HOME,
    DEMOGRAPHICS,
    CONDUCT_SESSION,
    PREVIOUS_SESSIONS,
    AVAILABLE_ACTIONS,
    SESSION_COMPLETION,
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
    GAMING
}

data class SavedSession(
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
    private var targetGameMode by mutableStateOf(GamingMode.SELECTION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadLastCsv()

        // Durable Consent check: consent persists across recreations and restarts
        val consentAccepted = ConsentManager.isConsentAccepted(this)
        val targetScreen = intent?.getStringExtra("screen")
        val modeExtra = intent?.getStringExtra("mode")
        targetGameMode = if (modeExtra == "runner") GamingMode.RUNNER else GamingMode.SELECTION
        if (targetScreen == "gaming") {
            if (!isSessionActive) {
                startFallbackLocalSession()
            }
            currentScreen = Screen.GAMING
        } else {
            currentScreen = if (consentAccepted) Screen.HOME else Screen.WELCOME
        }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetScreen = intent.getStringExtra("screen")
        val modeExtra = intent.getStringExtra("mode")
        targetGameMode = if (modeExtra == "runner") GamingMode.RUNNER else GamingMode.SELECTION
        if (targetScreen == "gaming") {
            if (!isSessionActive) {
                startFallbackLocalSession()
            }
            currentScreen = Screen.GAMING
        }
    }

    @Composable
    private fun AdaptIQAppRouter() {
        val coroutineScope = rememberCoroutineScope()

        // Comprehensive Back Navigation Handler
        BackHandler(enabled = currentScreen != Screen.HOME && currentScreen != Screen.WELCOME) {
            when (currentScreen) {
                Screen.HOLDING_PHONE, Screen.TAP_PRECISION, Screen.REACTION_TIME,
                Screen.TYPING, Screen.GESTURES, Screen.SCROLLING, Screen.DRAG_DROP,
                Screen.TARGET_TRACKING, Screen.SLIDER_CONTROL, Screen.VISUAL_SEARCH,
                Screen.NAVIGATION, Screen.MULTI_TOUCH, Screen.GAMING -> {
                    currentScreen = Screen.CONDUCT_SESSION
                }
                Screen.CONDUCT_SESSION, Screen.DEMOGRAPHICS, Screen.PREVIOUS_SESSIONS,
                Screen.AVAILABLE_ACTIONS, Screen.SESSION_COMPLETION -> {
                    currentScreen = Screen.HOME
                }
                Screen.PRIVACY_INFO -> {
                    currentScreen = if (ConsentManager.isConsentAccepted(this@MainActivity)) Screen.AVAILABLE_ACTIONS else Screen.WELCOME
                }
                Screen.CONSENT -> {
                    currentScreen = Screen.WELCOME
                }
                else -> {
                    currentScreen = Screen.HOME
                }
            }
        }

        when (currentScreen) {
            // 1. Welcome Screen
            Screen.WELCOME -> WelcomeScreen(
                onGetStarted = {
                    if (ConsentManager.isConsentAccepted(this)) {
                        currentScreen = Screen.HOME
                    } else {
                        currentScreen = Screen.CONSENT
                    }
                },
                onViewPrivacy = {
                    currentScreen = Screen.PRIVACY_INFO
                }
            )

            // 2. Dedicated Privacy & Study Information
            Screen.PRIVACY_INFO -> PrivacyStudyInfoScreen(
                onBack = {
                    currentScreen = if (ConsentManager.isConsentAccepted(this)) Screen.AVAILABLE_ACTIONS else Screen.WELCOME
                }
            )

            // 3. Consent Screen
            Screen.CONSENT -> ConsentScreen(
                onAgree = {
                    ConsentManager.setConsentAccepted(this, true)
                    currentScreen = Screen.HOME
                },
                onBack = {
                    currentScreen = Screen.WELCOME
                }
            )

            // 4. Clean Main Dashboard (Exactly 3 Primary Action Buttons)
            Screen.HOME -> MainDashboardScreen(
                onConductSession = {
                    if (isSessionActive && sessionId.isNotBlank()) {
                        currentScreen = Screen.CONDUCT_SESSION
                    } else {
                        currentScreen = Screen.DEMOGRAPHICS
                    }
                },
                onPreviousSessions = {
                    currentScreen = Screen.PREVIOUS_SESSIONS
                },
                onAvailableActions = {
                    currentScreen = Screen.AVAILABLE_ACTIONS
                }
            )

            // 5. Demographics Screen (Age in Years, Gender, Zero Age Group)
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

            // 6. Dedicated Conduct Session Screen (Activities Battery & Progress)
            Screen.CONDUCT_SESSION -> ConductSessionScreen(
                testId = testId,
                sessionId = sessionId,
                ageYears = currentAgeYears,
                gender = currentGender,
                activities = listOf(
                    ActivityItem("holding_phone", "1. Holding the Phone", holdingPhoneStatus),
                    ActivityItem("tap_precision", "2. Tap Precision", tapPrecisionStatus),
                    ActivityItem("reaction_time", "3. Reaction Time", reactionTimeStatus),
                    ActivityItem("typing", "4. Typing", typingStatus),
                    ActivityItem("gestures", "5. Swipe / Gesture", gestureStatus),
                    ActivityItem("scrolling", "6. Scrolling", scrollStatus),
                    ActivityItem("drag_drop", "7. Drag & Drop", dragDropStatus),
                    ActivityItem("target_tracking", "8. Target Tracking", targetTrackingStatus),
                    ActivityItem("slider_control", "9. Slider Control", sliderControlStatus),
                    ActivityItem("visual_search", "10. Visual Search", visualSearchStatus),
                    ActivityItem("navigation", "11. Navigation Task", navigationStatus),
                    ActivityItem("multi_touch", "12. Multi-Touch", multiTouchStatus),
                    ActivityItem("gaming", "13. Gaming-style Interaction", gamingStatus)
                ),
                onSelectActivity = { key ->
                    when (key) {
                        "holding_phone" -> navigateToActivity("holding_phone", Screen.HOLDING_PHONE)
                        "tap_precision" -> navigateToActivity("tap_precision", Screen.TAP_PRECISION)
                        "reaction_time" -> navigateToActivity("reaction_time", Screen.REACTION_TIME)
                        "typing" -> navigateToActivity("typing", Screen.TYPING)
                        "gestures" -> navigateToActivity("gestures", Screen.GESTURES)
                        "scrolling" -> navigateToActivity("scrolling", Screen.SCROLLING)
                        "drag_drop" -> navigateToActivity("drag_drop", Screen.DRAG_DROP)
                        "target_tracking" -> navigateToActivity("target_tracking", Screen.TARGET_TRACKING)
                        "slider_control" -> navigateToActivity("slider_control", Screen.SLIDER_CONTROL)
                        "visual_search" -> navigateToActivity("visual_search", Screen.VISUAL_SEARCH)
                        "navigation" -> navigateToActivity("navigation", Screen.NAVIGATION)
                        "multi_touch" -> navigateToActivity("multi_touch", Screen.MULTI_TOUCH)
                        "gaming" -> navigateToActivity("gaming", Screen.GAMING)
                    }
                },
                onViewCompletion = {
                    currentScreen = Screen.SESSION_COMPLETION
                },
                onBack = { currentScreen = Screen.HOME }
            )

            // 7. Dedicated Session Completion Screen (CSV Export & Verification)
            Screen.SESSION_COMPLETION -> SessionCompletionScreen(
                participantId = testId,
                sessionId = sessionId,
                ageYears = currentAgeYears,
                gender = currentGender,
                csvFile = lastCsvPath?.let { File(it) } ?: getSavedSessions().firstOrNull()?.file,
                isSyncQueued = true,
                onExportCsv = { file -> shareFile(file) },
                onReturnToDashboard = {
                    // Conclude active session so dashboard can start a fresh session later
                    isSessionActive = false
                    currentScreen = Screen.HOME
                }
            )

            // 8. Dedicated Previous Session History Screen
            Screen.PREVIOUS_SESSIONS -> {
                val history = loadSessionHistoryRecords()
                PreviousSessionsScreen(
                    sessions = history,
                    onExportCsv = { file -> shareFile(file) },
                    onUploadSession = { record ->
                        record.csvFile?.let { queueAndUploadFile(it) }
                    },
                    onBack = { currentScreen = Screen.HOME }
                )
            }

            // 9. Dedicated Available Actions Screen
            Screen.AVAILABLE_ACTIONS -> {
                val pendingCount = remember {
                    SyncQueueManager.getInstance(this@MainActivity).getPendingSessions().size
                }
                AvailableActionsScreen(
                    isSessionActive = isSessionActive,
                    pendingSyncCount = pendingCount,
                    onViewPrivacy = { currentScreen = Screen.PRIVACY_INFO },
                    onTriggerSync = { submitDatasetToAdmin() },
                    onTestConnectivity = { testBackendConnectivity() },
                    onResetSession = {
                        resetSessionForNextParticipant()
                        currentScreen = Screen.HOME
                    },
                    onBack = { currentScreen = Screen.HOME }
                )
            }

            // --- 13 Research Battery Activity Screens ---
            Screen.HOLDING_PHONE -> HoldingThePhoneActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("holding_phone", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("holding_phone", ActivityStatus.COMPLETED, "Holding the Phone Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.TAP_PRECISION -> TapPrecisionActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("tap_precision", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("tap_precision", ActivityStatus.COMPLETED, "Tap Precision Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.REACTION_TIME -> ReactionTimeActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("reaction_time", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("reaction_time", ActivityStatus.COMPLETED, "Reaction Time Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.TYPING -> TypingActivityScreen(
                testId = testId,
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("typing", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("typing", ActivityStatus.COMPLETED, "Typing Activity Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.GESTURES -> GestureActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("gestures", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("gestures", ActivityStatus.COMPLETED, "Gesture Activity Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.SCROLLING -> ScrollingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("scrolling", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("scrolling", ActivityStatus.COMPLETED, "Scrolling Activity Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.DRAG_DROP -> DragAndDropActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("drag_drop", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("drag_drop", ActivityStatus.COMPLETED, "Drag & Drop Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.TARGET_TRACKING -> TargetTrackingActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("target_tracking", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("target_tracking", ActivityStatus.COMPLETED, "Target Tracking Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.SLIDER_CONTROL -> SliderControlActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("slider_control", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("slider_control", ActivityStatus.COMPLETED, "Slider Control Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.VISUAL_SEARCH -> VisualSearchActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("visual_search", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("visual_search", ActivityStatus.COMPLETED, "Visual Search Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.NAVIGATION -> NavigationTaskActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("navigation", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("navigation", ActivityStatus.COMPLETED, "Navigation Task Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.MULTI_TOUCH -> MultiTouchActivityScreen(
                onLogEvent = { exp, det, res, att, dur, touch -> logActionEvent("multi_touch", exp, det, res, att, dur, touch) },
                onActivityCompleted = { onCompleteActivity("multi_touch", ActivityStatus.COMPLETED, "Multi-Touch Completed") },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
            Screen.GAMING -> GamingActivityScreen(
                initialGameMode = targetGameMode,
                onLogEvent = { exp, det, res, att, dur, touch, gMode ->
                    logActionEvent("gaming", exp, det, res, att, dur, touch, gMode)
                },
                onGameModeSelected = { selectedMode ->
                    updateCurrentActivity("gaming", selectedMode)
                },
                onActivityCompleted = {
                    onCompleteActivity("gaming", ActivityStatus.COMPLETED, "Gaming Interaction Completed")
                },
                onBack = { currentScreen = Screen.CONDUCT_SESSION }
            )
        }
    }

    private fun onCompleteActivity(activityKey: String, status: ActivityStatus, toastMsg: String) {
        when (activityKey) {
            "holding_phone" -> holdingPhoneStatus = status
            "tap_precision" -> tapPrecisionStatus = status
            "reaction_time" -> reactionTimeStatus = status
            "typing" -> typingStatus = status
            "gestures" -> gestureStatus = status
            "scrolling" -> scrollStatus = status
            "drag_drop" -> dragDropStatus = status
            "target_tracking" -> targetTrackingStatus = status
            "slider_control" -> sliderControlStatus = status
            "visual_search" -> visualSearchStatus = status
            "navigation" -> navigationStatus = status
            "multi_touch" -> multiTouchStatus = status
            "gaming" -> gamingStatus = status
        }

        showToast(toastMsg)
        checkSessionCompletion()
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
            SyncQueueManager.getInstance(this).markSessionCompleted(sessionId)
            loadLastCsv()
            lastCsvPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    queueAndUploadFile(file)
                }
            }
            showToast("Study Completed Successfully! All 13 Activities Saved Locally.")
            currentScreen = Screen.SESSION_COMPLETION
        } else {
            currentScreen = Screen.CONDUCT_SESSION
        }
    }

    private suspend fun startBackendPseudonymousSession(ageYears: Int, gender: String, participantType: String) {
        try {
            val response = withContext(Dispatchers.IO) {
                AdaptIqHttpClient.api.startParticipantSession(
                    ParticipantSessionStartRequest(
                        participantType = participantType,
                        condition = "SITTING",
                        ageGroup = "",
                        deviceModel = "Normalized Android Device"
                    )
                )
            }
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                sessionId = body.sessionId
                testId = body.testId
                sessionToken = body.token
                isSessionActive = true

                resetActivityStatuses()

                SyncQueueManager.getInstance(this).registerSessionStart(
                    sessionId = sessionId,
                    testId = testId,
                    orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
                )

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
                    currentScreen = Screen.CONDUCT_SESSION
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

        resetActivityStatuses()

        SyncQueueManager.getInstance(this).registerSessionStart(
            sessionId = sessionId,
            testId = testId,
            orientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) "landscape" else "portrait"
        )

        val intent = Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_SESSION_ID, sessionId)
            putExtra(RecordingService.EXTRA_TEST_ID, testId)
            putExtra(RecordingService.EXTRA_AGE_YEARS, currentAgeYears)
            putExtra(RecordingService.EXTRA_GENDER, currentGender)
            putExtra(RecordingService.EXTRA_SESSION_NAME, "Adapt-IQ Research Session")
        }

        ContextCompat.startForegroundService(this, intent)
        currentScreen = Screen.CONDUCT_SESSION
    }

    private fun resetActivityStatuses() {
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

        resetActivityStatuses()
        loadLastCsv()
        showToast("Session reset for next participant.")
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

    private fun testBackendConnectivity() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    AdaptIqHttpClient.api.startParticipantSession(
                        ParticipantSessionStartRequest(
                            participantType = "PING",
                            condition = "SITTING",
                            ageGroup = "",
                            deviceModel = "Connectivity Test"
                        )
                    )
                }
                if (response.isSuccessful) {
                    showToast("✓ Backend Online: Connected to Adapt-IQ Research Server")
                } else {
                    showToast("⚠️ Backend reachable with response code: ${response.code()}")
                }
            } catch (e: Exception) {
                showToast("❌ Offline: Cannot reach backend server. Local-first storage active.")
            }
        }
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

    private fun loadSessionHistoryRecords(): List<SessionHistoryRecord> {
        val savedSessions = getSavedSessions()
        val queuedSessions = SyncQueueManager.getInstance(this).getAllSessions()

        val recordsMap = mutableMapOf<String, SessionHistoryRecord>()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)

        // SQLite session records
        for (qs in queuedSessions) {
            val dateStr = dateFormat.format(Date(qs.startTime))
            recordsMap[qs.sessionId] = SessionHistoryRecord(
                sessionId = qs.sessionId,
                participantId = qs.testId,
                formattedDateTime = dateStr,
                status = qs.status,
                activityCompletionSummary = if (qs.status == "COMPLETED") "13 of 13 Completed" else "In Progress / Interrupted",
                syncStatus = if (qs.status == "SYNCED") "SYNCED" else "PENDING_UPLOAD",
                csvFile = null,
                recordCount = 0
            )
        }

        // Merge with actual CSV files on disk
        for (ss in savedSessions) {
            val existing = recordsMap[ss.sessionId]
            val dateStr = dateFormat.format(Date(ss.file.lastModified()))
            val status = if (ss.records >= 13) "COMPLETED" else (existing?.status ?: "COMPLETED")
            val syncStatus = existing?.syncStatus ?: "LOCAL_ONLY"

            recordsMap[ss.sessionId] = SessionHistoryRecord(
                sessionId = ss.sessionId,
                participantId = ss.testId,
                formattedDateTime = dateStr,
                status = status,
                activityCompletionSummary = "${ss.records} recorded events",
                syncStatus = syncStatus,
                csvFile = ss.file,
                recordCount = ss.records
            )
        }

        return recordsMap.values.sortedByDescending { it.formattedDateTime }
    }

    private fun loadLastCsv() {
        lastCsvPath = getSharedPreferences(RecordingService.PREFS_NAME, MODE_PRIVATE)
            .getString(RecordingService.KEY_LAST_CSV, null)
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
