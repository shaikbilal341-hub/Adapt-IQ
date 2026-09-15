package com.example.adaptiq

import kotlinx.coroutines.delay
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.adaptiq.ui.theme.AdaptIQTheme
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.max

private enum class Screen {
    HOME,
    NEW_TEST,
    SENSOR_CHECK,
    ACTION,
    LIVE,
    SUMMARY,
    PREVIOUS,
    EXPORT,
    HANDOVER
}

private data class SavedSession(
    val file: File,
    val sessionId: String,
    val testId: String,
    val age: String,
    val ageGroup: String,
    val participantType: String,
    val action: String,
    val records: Int
)

class MainActivity : ComponentActivity() {

    // =========================================================
    // SCREEN
    // =========================================================

    private var screen by mutableStateOf(Screen.HOME)

    // =========================================================
    // SESSION DATA
    // =========================================================

    private var testId by mutableStateOf("Test01")

    private var age by mutableStateOf("")

    private var ageGroup by mutableStateOf("")

    private var participantType by mutableStateOf("UNKNOWN")

    private var condition by mutableStateOf("SITTING")

    private var actionType by mutableStateOf("one_hand_holding")

    private var sessionId by mutableStateOf("")

    // =========================================================
    // RECORDING STATE
    // =========================================================

    private var recording by mutableStateOf(false)

    private var recordingStartTime by mutableLongStateOf(0L)

    private var elapsedMs by mutableLongStateOf(0L)

    private var recordCount by mutableIntStateOf(0)

    private var lastCsvPath by mutableStateOf<String?>(null)

    private var currentEventLabel by mutableStateOf("SESSION_STARTED")

    // =========================================================
    // HANDOVER
    // =========================================================

    private var handoverBefore by mutableStateOf("ADULT")

    private var handoverAfter by mutableStateOf("CHILD")

    private var handoverEvent by mutableStateOf("BEFORE_HANDOVER")

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        loadLastCsv()

        setContent {

            AdaptIQTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    AdaptIQApp()
                }
            }
        }
    }

    // =========================================================
    // MAIN APP
    // =========================================================

    @Composable
    private fun AdaptIQApp() {

        LaunchedEffect(recording) {

            if (recording) {

                while (recording) {

                    elapsedMs =
                        System.currentTimeMillis() -
                                recordingStartTime

                    recordCount =
                        (elapsedMs / 100L).toInt()

                    delay(250)
                }
            }
        }

        when (screen) {

            Screen.HOME ->
                HomeScreen()

            Screen.NEW_TEST ->
                NewTestScreen()

            Screen.SENSOR_CHECK ->
                SensorCheckScreen()

            Screen.ACTION ->
                ActionScreen()

            Screen.LIVE ->
                LiveScreen()

            Screen.SUMMARY ->
                SummaryScreen()

            Screen.PREVIOUS ->
                PreviousSessionsScreen()

            Screen.EXPORT ->
                ExportScreen()

            Screen.HANDOVER ->
                HandoverScreen()
        }
    }

    // =========================================================
    // AGE GROUP
    // =========================================================

    private fun calculateAgeGroup(
        value: String
    ): String {

        val enteredAge =
            value.toIntOrNull()
                ?: return ""

        return when {

            enteredAge in 0..2 ->
                "Under 3"

            enteredAge in 3..9 ->
                "3–9"

            enteredAge in 10..15 ->
                "10–15"

            enteredAge in 16..17 ->
                "16–17"

            enteredAge in 18..25 ->
                "18–25"

            enteredAge in 26..40 ->
                "26–40"

            enteredAge in 41..60 ->
                "41–60"

            enteredAge >= 61 ->
                "60+"

            else ->
                ""
        }
    }

    // =========================================================
    // HOME
    // =========================================================

    @Composable
    private fun HomeScreen() {

        val sessions =
            getSavedSessions()

        val totalRecords =
            sessions.sumOf {
                it.records
            }

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Adapt-IQ",
                style =
                    MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Data Collection App",
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text =
                    "Collect sensor data for user-adaptation research"
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                DashboardCard(
                    title = "Total Sessions",
                    value = sessions.size.toString(),
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    title = "Total Records",
                    value = totalRecords.toString(),
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    title = "Sensors",
                    value =
                        "${availableSensorCount()}/3",
                    modifier = Modifier.weight(1f)
                )
            }

            Button(

                onClick = {

                    testId =
                        generateNextTestId()

                    age = ""

                    ageGroup = ""

                    participantType =
                        "UNKNOWN"

                    condition =
                        "SITTING"

                    actionType =
                        "one_hand_holding"

                    screen =
                        Screen.NEW_TEST
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Start New Test")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.PREVIOUS
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Previous Sessions")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.EXPORT
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Export Dataset")
            }

            OutlinedButton(

                onClick = {

                    handoverBefore =
                        "ADULT"

                    handoverAfter =
                        "CHILD"

                    handoverEvent =
                        "BEFORE_HANDOVER"

                    screen =
                        Screen.HANDOVER
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Record Handover")
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "Version 1.0"
            )

            Text(
                text =
                    "APP 1 • Sensor Data Collection Platform"
            )
        }
    }

    // =========================================================
    // DASHBOARD CARD
    // =========================================================

    @Composable
    private fun DashboardCard(

        title: String,

        value: String,

        modifier: Modifier

    ) {

        Card(
            modifier = modifier
        ) {

            Column(
                modifier =
                    Modifier.padding(12.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = value,
                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    // =========================================================
    // NEW TEST
    // =========================================================

    @Composable
    private fun NewTestScreen() {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "New Test Session",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text =
                    "Enter anonymous research information"
            )

            // -------------------------------------------------
            // TEST ID
            // -------------------------------------------------

            OutlinedTextField(

                value = testId,

                onValueChange = {

                    if (!recording) {
                        testId = it
                    }
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Test ID")
                },

                singleLine = true
            )

            // -------------------------------------------------
            // AGE
            // -------------------------------------------------

            OutlinedTextField(

                value = age,

                onValueChange = {

                    age =
                        it
                            .filter {
                                    char ->
                                char.isDigit()
                            }
                            .take(3)

                    ageGroup =
                        calculateAgeGroup(age)
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Age")
                },

                placeholder = {
                    Text("Enter exact age")
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                singleLine = true
            )

            // -------------------------------------------------
            // AUTOMATIC AGE GROUP
            // -------------------------------------------------

            OutlinedTextField(

                value =
                    ageGroup,

                onValueChange = {},

                enabled = false,

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Age Group — Automatically Calculated")
                }
            )

            Text(
                text =
                    "Age group cannot be manually changed."
            )

            // -------------------------------------------------
            // PARTICIPANT TYPE
            // -------------------------------------------------

            Text(
                text = "Participant Type",
                style =
                    MaterialTheme.typography.titleMedium
            )

            SelectionRow(
                options =
                    listOf(
                        "ADULT",
                        "CHILD",
                        "UNKNOWN"
                    ),

                selected =
                    participantType,

                onSelected = {
                    participantType = it
                }
            )

            // -------------------------------------------------
            // PHONE CONDITION
            // -------------------------------------------------

            Text(
                text =
                    "Phone Usage Condition",
                style =
                    MaterialTheme.typography.titleMedium
            )

            SelectionRow(
                options =
                    listOf(
                        "SITTING",
                        "WALKING",
                        "STANDING",
                        "OTHER"
                    ),

                selected =
                    condition,

                onSelected = {
                    condition = it
                }
            )

            // -------------------------------------------------
            // DEVICE
            // -------------------------------------------------

            Text(
                text = "Device Model",
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text =
                    "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            // -------------------------------------------------
            // CONTINUE
            // -------------------------------------------------

            Button(

                onClick = {

                    if (ageGroup.isBlank()) {

                        showToast(
                            "Enter a valid age first"
                        )

                    } else {

                        createSession()

                        screen =
                            Screen.SENSOR_CHECK
                    }
                },

                enabled =
                    testId.isNotBlank() &&
                            ageGroup.isNotBlank(),

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Continue")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.HOME
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Back")
            }
        }
    }

    // =========================================================
    // CREATE SESSION
    // =========================================================

    private fun createSession() {

        sessionId =
            "SESSION_" +
                    UUID
                        .randomUUID()
                        .toString()
                        .replace("-", "")
                        .take(8)
                        .uppercase(Locale.US)

        currentEventLabel =
            "SESSION_STARTED"
    }

    // =========================================================
    // SENSOR CHECK
    // =========================================================

    @Composable
    private fun SensorCheckScreen() {

        val sensors =
            sensorAvailability()

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Sensor Check",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text =
                    "Verify sensors before recording"
            )

            SensorStatus(
                "Accelerometer",
                sensors[0]
            )

            SensorStatus(
                "Gyroscope",
                sensors[1]
            )

            SensorStatus(
                "Proximity",
                sensors[2]
            )

            InfoRow(
                "Sampling Rate",
                "Approximately 10 Hz"
            )

            InfoRow(
                "Storage",
                "App external datasets directory"
            )

            InfoRow(
                "Device",
                "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Button(

                onClick = {
                    screen = Screen.ACTION
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Continue")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.NEW_TEST
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Back")
            }
        }
    }

    // =========================================================
    // SENSOR STATUS
    // =========================================================

    @Composable
    private fun SensorStatus(

        name: String,

        ready: Boolean

    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(name)

            Text(
                if (ready)
                    "READY"
                else
                    "NOT AVAILABLE"
            )
        }
    }

    // =========================================================
    // ACTION SELECTION
    // =========================================================

    @Composable
    private fun ActionScreen() {

        val actions =
            listOf(

                "one_hand_holding" to
                        "One-hand holding",

                "two_hand_holding" to
                        "Two-hand holding",

                "typing" to
                        "Typing",

                "scrolling" to
                        "Scrolling",

                "watching_video" to
                        "Watching video",

                "gaming" to
                        "GAMING",

                "phone_call" to
                        "Phone call",

                "walking_while_holding" to
                        "Walking while holding",

                "phone_on_table" to
                        "Phone on table",

                "picking_up_phone" to
                        "Picking up phone",

                "rotating_phone" to
                        "Rotating phone",

                "changing_grip" to
                        "Changing grip",

                "other" to
                        "Other"
            )

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "Action Selection",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text =
                    "Select the activity being recorded"
            )

            actions.forEach {

                    (id, label) ->

                FilterChip(

                    selected =
                        actionType == id,

                    onClick = {
                        actionType = id
                    },

                    label = {
                        Text(label)
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Button(

                onClick = {

                    currentEventLabel =
                        "SESSION_STARTED"

                    startRecording()
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Start Recording")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.SENSOR_CHECK
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Back")
            }
        }
    }

    // =========================================================
    // START RECORDING
    // =========================================================

    private fun startRecording() {

        if (sessionId.isBlank()) {

            createSession()
        }

        val intent =
            Intent(
                this,
                RecordingService::class.java
            ).apply {

                action =
                    RecordingService.ACTION_START

                putExtra(
                    RecordingService.EXTRA_SESSION_ID,
                    sessionId
                )

                putExtra(
                    RecordingService.EXTRA_TEST_ID,
                    testId.trim()
                )

                putExtra(
                    RecordingService.EXTRA_AGE,
                    age.toIntOrNull() ?: 0
                )

                putExtra(
                    RecordingService.EXTRA_AGE_GROUP,
                    ageGroup
                )

                putExtra(
                    RecordingService.EXTRA_USER_TYPE,
                    participantType
                )

                putExtra(
                    RecordingService.EXTRA_CONDITION,
                    condition
                )

                putExtra(
                    RecordingService.EXTRA_ACTIVITY,
                    actionType
                )

                putExtra(
                    RecordingService.EXTRA_EVENT_LABEL,
                    currentEventLabel
                )

                putExtra(
                    RecordingService.EXTRA_DEVICE_MODEL,
                    "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            }

        ContextCompat.startForegroundService(
            this,
            intent
        )

        recordingStartTime =
            System.currentTimeMillis()

        elapsedMs = 0L

        recordCount = 0

        recording = true

        screen = Screen.LIVE

        showToast(
            "Background recording started"
        )
    }

    // =========================================================
    // LIVE SCREEN
    // =========================================================

    @Composable
    private fun LiveScreen() {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Live Data Collection",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            InfoRow(
                "Test ID",
                testId
            )

            InfoRow(
                "Session ID",
                sessionId
            )

            InfoRow(
                "Recording Time",
                formatDuration(elapsedMs)
            )

            InfoRow(
                "Records Collected",
                recordCount.toString()
            )

            InfoRow(
                "Current Action",
                actionType
            )

            InfoRow(
                "Event",
                currentEventLabel
            )

            InfoRow(
                "Sensors",
                "${availableSensorCount()}/3 Ready"
            )

            InfoRow(
                "Sampling",
                "Approximately 10 Hz"
            )

            Text(
                text =
                    "Sensor values are being collected automatically by the foreground service."
            )

            Text(
                text =
                    "Accelerometer: X/Y/Z"
            )

            Text(
                text =
                    "Gyroscope: X/Y/Z"
            )

            Text(
                text =
                    "Proximity sensor active"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            OutlinedButton(

                onClick = {

                    showToast(
                        "Pause will be implemented without changing the recorder architecture."
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Pause")
            }

            Button(

                onClick = {
                    stopRecording()
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("STOP RECORDING")
            }
        }
    }

    // =========================================================
    // STOP RECORDING
    // =========================================================

    private fun stopRecording() {

        val intent =
            Intent(
                this,
                RecordingService::class.java
            ).apply {

                action =
                    RecordingService.ACTION_STOP
            }

        startService(intent)

        recording = false

        loadLastCsv()

        screen =
            Screen.SUMMARY

        showToast(
            "Recording stopped and CSV saved"
        )
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    @Composable
    private fun SummaryScreen() {

        val file =
            lastCsvPath?.let {
                File(it)
            }

        val records =
            file?.let {
                countRecords(it)
            } ?: recordCount

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "Session Summary",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            InfoRow(
                "Test ID",
                testId
            )

            InfoRow(
                "Session ID",
                sessionId
            )

            InfoRow(
                "Age",
                age
            )

            InfoRow(
                "Age Group",
                ageGroup
            )

            InfoRow(
                "Participant Type",
                participantType
            )

            InfoRow(
                "Action",
                actionType
            )

            InfoRow(
                "Duration",
                formatDuration(elapsedMs)
            )

            InfoRow(
                "Records",
                records.toString()
            )

            InfoRow(
                "Sensors",
                "${availableSensorCount()}/3 available"
            )

            InfoRow(
                "Data Quality",
                if (records > 0)
                    "Valid sensor records found"
                else
                    "No records found"
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Button(

                onClick = {
                    shareLastCsv()
                },

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    file?.exists() == true
            ) {

                Text("Save Session / Share CSV")
            }

            OutlinedButton(

                onClick = {

                    screen =
                        Screen.ACTION
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Record Again")
            }

            OutlinedButton(

                onClick = {

                    screen =
                        Screen.HOME
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Return Home")
            }
        }
    }

    // =========================================================
    // PREVIOUS SESSIONS
    // =========================================================

    @Composable
    private fun PreviousSessionsScreen() {

        val sessions =
            getSavedSessions()

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
        ) {

            Text(
                text = "Previous Sessions",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (sessions.isEmpty()) {

                Text(
                    text =
                        "No saved sessions found."
                )

            } else {

                LazyColumn(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(sessions) {

                            session ->

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(12.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(5.dp)
                            ) {

                                Text(
                                    session.sessionId,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    "${session.testId} • ${session.ageGroup}"
                                )

                                Text(
                                    "Action: ${session.action}"
                                )

                                Text(
                                    "Records: ${session.records}"
                                )

                                Row(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(8.dp)
                                ) {

                                    OutlinedButton(
                                        onClick = {
                                            shareFile(
                                                session.file
                                            )
                                        }
                                    ) {

                                        Text("Export")
                                    }

                                    OutlinedButton(
                                        onClick = {

                                            session.file.delete()

                                            showToast(
                                                "Session deleted"
                                            )
                                        }
                                    ) {

                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            OutlinedButton(

                onClick = {
                    screen = Screen.HOME
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Back")
            }
        }
    }

    // =========================================================
    // EXPORT
    // =========================================================

    @Composable
    private fun ExportScreen() {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "Export Dataset",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text =
                    "CSV export options"
            )

            Button(

                onClick = {
                    shareLastCsv()
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Selected / Last Session")
            }

            OutlinedButton(

                onClick = {
                    shareFilteredCsv("ADULT")
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Adult Data")
            }

            OutlinedButton(

                onClick = {
                    shareFilteredCsv("CHILD")
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Child Data")
            }

            OutlinedButton(

                onClick = {
                    shareFilteredCsv("gaming")
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Gaming Data")
            }

            OutlinedButton(

                onClick = {
                    showToast(
                        "All-session combined export will be added after the MVP recording workflow is verified."
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("All Sessions")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.HOME
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Back")
            }
        }
    }

    // =========================================================
    // HANDOVER
    // =========================================================

    @Composable
    private fun HandoverScreen() {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Handover Recording",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text =
                    "Record a transition between smartphone users."
            )

            Text(
                text = "Before Handover",
                style =
                    MaterialTheme.typography.titleMedium
            )

            SelectionRow(

                options =
                    listOf(
                        "ADULT",
                        "CHILD",
                        "UNKNOWN"
                    ),

                selected =
                    handoverBefore,

                onSelected = {
                    handoverBefore = it
                }
            )

            Text(
                text = "New User",
                style =
                    MaterialTheme.typography.titleMedium
            )

            SelectionRow(

                options =
                    listOf(
                        "ADULT",
                        "CHILD",
                        "UNKNOWN"
                    ),

                selected =
                    handoverAfter,

                onSelected = {
                    handoverAfter = it
                }
            )

            InfoRow(
                "Current Event",
                handoverEvent
            )

            Button(

                onClick = {

                    createSession()

                    actionType =
                        "handover"

                    participantType =
                        handoverBefore

                    currentEventLabel =
                        "BEFORE_HANDOVER"

                    screen =
                        Screen.SENSOR_CHECK
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Start Handover Recording")
            }

            Text(
                text =
                    "After starting recording, use the event controls below."
            )

            OutlinedButton(

                onClick = {

                    handoverEvent =
                        "BEFORE_HANDOVER"

                    currentEventLabel =
                        "BEFORE_HANDOVER"

                    sendEventToService(
                        "BEFORE_HANDOVER"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("BEFORE_HANDOVER")
            }

            OutlinedButton(

                onClick = {

                    handoverEvent =
                        "HANDOVER_STARTED"

                    currentEventLabel =
                        "HANDOVER_STARTED"

                    sendEventToService(
                        "HANDOVER_STARTED"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("HANDOVER_STARTED")
            }

            OutlinedButton(

                onClick = {

                    handoverEvent =
                        "HANDOVER_COMPLETED"

                    currentEventLabel =
                        "HANDOVER_COMPLETED"

                    sendEventToService(
                        "HANDOVER_COMPLETED"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("HANDOVER_COMPLETED")
            }

            OutlinedButton(

                onClick = {

                    participantType =
                        handoverAfter

                    handoverEvent =
                        "NEW_USER_STARTED"

                    currentEventLabel =
                        "NEW_USER_STARTED"

                    sendEventToService(
                        "NEW_USER_STARTED"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("NEW_USER_STARTED")
            }

            Button(

                onClick = {

                    handoverEvent =
                        "SESSION_COMPLETED"

                    currentEventLabel =
                        "SESSION_COMPLETED"

                    sendEventToService(
                        "SESSION_COMPLETED"
                    )

                    if (recording) {
                        stopRecording()
                    } else {
                        screen = Screen.HOME
                    }
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("SESSION_COMPLETED")
            }

            OutlinedButton(

                onClick = {
                    screen = Screen.HOME
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("Back")
            }
        }
    }

    // =========================================================
    // SEND EVENT TO SERVICE
    // =========================================================

    private fun sendEventToService(
        event: String
    ) {

        val intent =
            Intent(
                this,
                RecordingService::class.java
            ).apply {

                action =
                    RecordingService.ACTION_UPDATE_EVENT

                putExtra(
                    RecordingService.EXTRA_EVENT_LABEL,
                    event
                )
            }

        startService(intent)
    }

    // =========================================================
    // SELECTION ROW
    // =========================================================

    @Composable
    private fun SelectionRow(

        options: List<String>,

        selected: String,

        onSelected: (String) -> Unit

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),

            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            options.forEach {

                    option ->

                FilterChip(

                    selected =
                        selected == option,

                    onClick = {
                        onSelected(option)
                    },

                    label = {
                        Text(option)
                    }
                )
            }
        }
    }

    // =========================================================
    // INFO ROW
    // =========================================================

    @Composable
    private fun InfoRow(

        label: String,

        value: String

    ) {

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelLarge
            )

            Text(
                text = value
            )
        }
    }

    // =========================================================
    // SENSOR AVAILABILITY
    // =========================================================

    private fun sensorAvailability(): List<Boolean> {

        val manager =
            getSystemService(
                SENSOR_SERVICE
            ) as SensorManager

        return listOf(

            manager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            ) != null,

            manager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            ) != null,

            manager.getDefaultSensor(
                Sensor.TYPE_PROXIMITY
            ) != null
        )
    }

    private fun availableSensorCount(): Int {

        return sensorAvailability()
            .count {
                it
            }
    }

    // =========================================================
    // DATASET DIRECTORY
    // =========================================================

    private fun datasetsDirectory(): File {

        return getExternalFilesDir(
            "datasets"
        ) ?: File(
            filesDir,
            "datasets"
        )
    }

    // =========================================================
    // SAVED SESSIONS
    // =========================================================

    private fun getSavedSessions(): List<SavedSession> {

        val directory =
            datasetsDirectory()

        if (!directory.exists()) {
            return emptyList()
        }

        val files =
            directory.listFiles {
                    file ->

                file.isFile &&
                        file.extension
                            .equals(
                                "csv",
                                true
                            )
            } ?: return emptyList()

        return files.map {

                file ->

            val lines =
                runCatching {
                    file.readLines()
                }.getOrDefault(
                    emptyList()
                )

            val header =
                lines.firstOrNull()
                    ?: ""

            val columns =
                header.split(",")

            val firstRow =
                lines.getOrNull(1)
                    ?.split(",")
                    ?: emptyList()

            fun value(
                name: String
            ): String {

                val index =
                    columns.indexOf(name)

                return if (
                    index >= 0 &&
                    index < firstRow.size
                ) {
                    firstRow[index]
                } else {
                    ""
                }
            }

            SavedSession(

                file =
                    file,

                sessionId =
                    value("session_id")
                        .ifBlank {
                            file.nameWithoutExtension
                        },

                testId =
                    value("test_id"),

                age =
                    value("age"),

                ageGroup =
                    value("age_group"),

                participantType =
                    value("participant_type"),

                action =
                    value("action_label"),

                records =
                    max(
                        0,
                        lines.size - 1
                    )
            )

        }.sortedByDescending {
            it.file.lastModified()
        }
    }

    // =========================================================
    // NEXT TEST ID
    // =========================================================

    private fun generateNextTestId(): String {

        val sessions =
            getSavedSessions()

        return "Test%02d".format(
            Locale.US,
            sessions.size + 1
        )
    }

    // =========================================================
    // COUNT CSV RECORDS
    // =========================================================

    private fun countRecords(
        file: File
    ): Int {

        return runCatching {

            max(
                0,
                file.readLines().size - 1
            )

        }.getOrDefault(0)
    }

    // =========================================================
    // LOAD LAST CSV
    // =========================================================

    private fun loadLastCsv() {

        lastCsvPath =
            getSharedPreferences(
                RecordingService.PREFS_NAME,
                MODE_PRIVATE
            ).getString(
                RecordingService.KEY_LAST_CSV,
                null
            )
    }

    // =========================================================
    // SHARE LAST CSV
    // =========================================================

    private fun shareLastCsv() {

        loadLastCsv()

        val path =
            lastCsvPath

        if (path.isNullOrBlank()) {

            showToast(
                "No recorded CSV found"
            )

            return
        }

        val file =
            File(path)

        if (!file.exists()) {

            showToast(
                "CSV file no longer exists"
            )

            return
        }

        shareFile(file)
    }

    // =========================================================
    // SHARE FILE
    // =========================================================

    private fun shareFile(
        file: File
    ) {

        if (!file.exists()) {

            showToast(
                "File not found"
            )

            return
        }

        val uri =
            FileProvider.getUriForFile(

                this,

                "${applicationContext.packageName}.fileprovider",

                file
            )

        val shareIntent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "text/csv"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                shareIntent,
                "Share AdaptIQ Dataset"
            )
        )
    }

    // =========================================================
    // FILTER CSV
    // =========================================================

    private fun shareFilteredCsv(
        filter: String
    ) {

        val sessions =
            getSavedSessions()

        val matching =
            sessions.firstOrNull {

                    session ->

                session.participantType
                    .equals(
                        filter,
                        true
                    ) ||

                        session.action
                            .equals(
                                filter,
                                true
                            )
            }

        if (matching == null) {

            showToast(
                "No $filter data found"
            )

            return
        }

        shareFile(
            matching.file
        )
    }

    // =========================================================
    // FORMAT TIME
    // =========================================================

    private fun formatDuration(
        milliseconds: Long
    ): String {

        val seconds =
            milliseconds / 1000

        val minutes =
            seconds / 60

        val remaining =
            seconds % 60

        return "%02d:%02d".format(
            Locale.US,
            minutes,
            remaining
        )
    }

    // =========================================================
    // TOAST
    // =========================================================

    private fun showToast(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}

