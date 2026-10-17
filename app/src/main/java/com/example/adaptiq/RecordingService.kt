package com.example.adaptiq

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class RecordingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    // =========================================================
    // SENSOR VALUES
    // =========================================================

    private var accX = 0f
    private var accY = 0f
    private var accZ = 0f

    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f

    private var proximity = 0f

    // =========================================================
    // RECORDING
    // =========================================================

    private var recordingThread: Thread? = null

    @Volatile
    private var isRecording = false

    private var csvFile: File? = null

    private var csvWriter: BufferedWriter? = null

    private var recordingStartTime = 0L

    private val eventSequence = AtomicLong(0L)

    // =========================================================
    // SESSION METADATA
    // =========================================================

    private var sessionId = "SESSION_0001"

    private var testId = "TEST_001"

    private var ageYears = 18

    private var gender = "Prefer not to say"

    private var sessionName = "Adapt-IQ Research Session"

    private var activity = "typing"

    private var gameMode = "standard"

    @Volatile
    private var eventLabel = "SESSION_STARTED"

    private var orientation = "portrait"

    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {

        const val ACTION_START = "com.example.adaptiq.START_RECORDING"

        const val ACTION_STOP = "com.example.adaptiq.STOP_RECORDING"

        const val ACTION_UPDATE_EVENT = "com.example.adaptiq.UPDATE_EVENT"

        const val ACTION_LOG_EVENT = "com.example.adaptiq.LOG_EVENT"

        // -----------------------------------------------------
        // EXTRAS
        // -----------------------------------------------------

        const val EXTRA_SESSION_ID = "session_id"

        const val EXTRA_TEST_ID = "test_id"

        const val EXTRA_AGE_YEARS = "age_years"

        const val EXTRA_GENDER = "gender"

        const val EXTRA_SESSION_NAME = "session_name"

        const val EXTRA_ACTIVITY = "activity"

        const val EXTRA_GAME_MODE = "game_mode"

        const val EXTRA_EVENT_LABEL = "event_label"

        const val EXTRA_ORIENTATION = "orientation"

        // Event specific extras
        const val EXTRA_EVENT_ID = "event_id"

        const val EXTRA_EXPECTED_ACTION = "expected_action"

        const val EXTRA_DETECTED_ACTION = "detected_action"

        const val EXTRA_RESULT = "result"

        const val EXTRA_ATTEMPT_ID = "attempt_id"

        const val EXTRA_DURATION = "duration"

        const val EXTRA_TOUCH_INFO = "touch_info"

        // -----------------------------------------------------
        // PREFERENCES
        // -----------------------------------------------------

        const val PREFS_NAME = "adaptiq_recording"

        const val KEY_LAST_CSV = "last_csv"

        // -----------------------------------------------------
        // NOTIFICATION
        // -----------------------------------------------------

        private const val CHANNEL_ID = "AdaptIQRecording"

        private const val NOTIFICATION_ID = 1001

        /**
         * Easy Analysis CSV Header matching Phase 8.4 Specification (Using Age_Years)
         */
        const val EASY_ANALYSIS_HEADER = "Age_Years,Gender,Session_Name,Participant_ID,Session_ID,Activity_Name,Game_Mode,Trial_ID,Event_ID,Timestamp_UTC,Elapsed_Time_ms,Expected_Action,Detected_Action,Outcome,Response_Latency_ms,Touch_Info,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,proximity,acc_magnitude,gyro_magnitude"

        /**
         * Legacy 22-column CSV Header for backward compatibility
         */
        const val LEGACY_CSV_HEADER = "timestamp,elapsed_ms,session_id,test_id,event_id,activity,orientation,expected_action,detected_action,result,attempt_id,duration_ms,touch_info,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,proximity,acc_magnitude,gyro_magnitude"
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channelName = "Adapt-IQ Research Recording"
        val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_START -> {

                sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "SESSION_0001"

                testId = intent.getStringExtra(EXTRA_TEST_ID) ?: "TEST_001"

                ageYears = intent.getIntExtra(EXTRA_AGE_YEARS, 18)

                gender = intent.getStringExtra(EXTRA_GENDER) ?: "Prefer not to say"

                sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "Adapt-IQ Research Session"

                activity = intent.getStringExtra(EXTRA_ACTIVITY) ?: "typing"

                gameMode = intent.getStringExtra(EXTRA_GAME_MODE) ?: "standard"

                eventLabel = intent.getStringExtra(EXTRA_EVENT_LABEL) ?: "SESSION_STARTED"

                orientation = intent.getStringExtra(EXTRA_ORIENTATION) ?: "portrait"

                eventSequence.set(0L)

                startRecording()
            }

            ACTION_UPDATE_EVENT -> {

                activity = intent.getStringExtra(EXTRA_ACTIVITY) ?: activity

                gameMode = intent.getStringExtra(EXTRA_GAME_MODE) ?: gameMode

                eventLabel = intent.getStringExtra(EXTRA_EVENT_LABEL) ?: eventLabel

                orientation = intent.getStringExtra(EXTRA_ORIENTATION) ?: orientation

                updateNotification()
            }

            ACTION_LOG_EVENT -> {

                if (isRecording) {

                    val eventId = intent.getLongExtra(EXTRA_EVENT_ID, eventSequence.incrementAndGet())

                    val expAction = intent.getStringExtra(EXTRA_EXPECTED_ACTION) ?: "none"

                    val detAction = intent.getStringExtra(EXTRA_DETECTED_ACTION) ?: "none"

                    val res = intent.getStringExtra(EXTRA_RESULT) ?: "successful"

                    val attempt = intent.getIntExtra(EXTRA_ATTEMPT_ID, 1)

                    val dur = intent.getLongExtra(EXTRA_DURATION, -1L)

                    val touch = intent.getStringExtra(EXTRA_TOUCH_INFO) ?: ""

                    val act = intent.getStringExtra(EXTRA_ACTIVITY) ?: activity
                    activity = act

                    val gMode = intent.getStringExtra(EXTRA_GAME_MODE) ?: gameMode
                    gameMode = gMode

                    val elapsed = SystemClock.elapsedRealtime() - recordingStartTime

                    writeActionEventRow(
                        eventId = eventId,
                        elapsedMs = elapsed,
                        activity = act,
                        expectedAction = expAction,
                        detectedAction = detAction,
                        result = res,
                        attemptId = attempt,
                        durationMs = dur,
                        touchInfo = touch
                    )
                }
            }

            ACTION_STOP -> {

                stopRecording()

                stopForeground(STOP_FOREGROUND_REMOVE)

                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun startRecording() {

        if (isRecording) {
            return
        }

        val directory = getExternalFilesDir("datasets") ?: File(filesDir, "datasets")

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val safeSession = sessionId.replace(Regex("[^A-Za-z0-9_-]"), "_")

        csvFile = File(directory, "${safeSession}_AdaptIQ_$time.csv")

        csvWriter = BufferedWriter(FileWriter(csvFile, false))

        // Write Easy Analysis Header (25 columns)
        csvWriter?.write(EASY_ANALYSIS_HEADER)

        csvWriter?.newLine()

        csvWriter?.flush()

        // Sensor registration
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        isRecording = true

        recordingStartTime = SystemClock.elapsedRealtime()

        startForeground(NOTIFICATION_ID, createNotification())

        // Background continuous sensor recording thread (~10 Hz)
        recordingThread = Thread {
            while (isRecording) {
                val elapsed = SystemClock.elapsedRealtime() - recordingStartTime
                writePeriodicSensorRow(elapsed)
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }

        recordingThread?.start()
    }

    private fun stopRecording() {

        if (!isRecording) {
            return
        }

        isRecording = false

        recordingThread?.interrupt()

        try {
            recordingThread?.join(1000)
        } catch (_: InterruptedException) {
        }

        recordingThread = null

        sensorManager.unregisterListener(this)

        try {
            csvWriter?.flush()
            csvWriter?.close()
        } catch (_: Exception) {
        }

        csvWriter = null

        csvFile?.let { file ->
            if (file.exists()) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CSV, file.absolutePath)
                    .apply()
            }
        }

        csvFile = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accX = event.values[0]
                accY = event.values[1]
                accZ = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
            }
            Sensor.TYPE_PROXIMITY -> {
                proximity = event.values[0]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun writePeriodicSensorRow(elapsedMs: Long) {
        val writer = csvWriter ?: return

        val timestamp = System.currentTimeMillis()
        val accMag = sqrt((accX * accX) + (accY * accY) + (accZ * accZ))
        val gyroMag = sqrt((gyroX * gyroX) + (gyroY * gyroY) + (gyroZ * gyroZ))
        val eventId = eventSequence.incrementAndGet()

        val row = String.format(
            Locale.US,
            "%d,%s,%s,%s,%s,%s,%s,0,%d,%d,%d,none,sensor_sample,none,NA,\"\",%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f",
            ageYears,
            csv(gender),
            csv(sessionName),
            csv(testId),
            csv(sessionId),
            csv(activity),
            csv(gameMode),
            eventId,
            timestamp,
            elapsedMs,
            accX, accY, accZ,
            gyroX, gyroY, gyroZ,
            proximity,
            accMag, gyroMag
        )

        synchronized(writer) {
            try {
                writer.write(row)
                writer.newLine()
                writer.flush()
            } catch (_: Exception) {
            }
        }
    }

    private fun writeActionEventRow(
        eventId: Long,
        elapsedMs: Long,
        activity: String,
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String
    ) {
        val writer = csvWriter ?: return

        val timestamp = System.currentTimeMillis()
        val latencyStr = if (durationMs < 0L) "NA" else durationMs.toString()

        val row = String.format(
            Locale.US,
            "%d,%s,%s,%s,%s,%s,%s,%d,%d,%d,%d,%s,%s,%s,%s,%s,NA,NA,NA,NA,NA,NA,NA,NA,NA",
            ageYears,
            csv(gender),
            csv(sessionName),
            csv(testId),
            csv(sessionId),
            csv(activity),
            csv(gameMode),
            attemptId,
            eventId,
            timestamp,
            elapsedMs,
            csv(expectedAction),
            csv(detectedAction),
            csv(result),
            latencyStr,
            csv(touchInfo)
        )

        synchronized(writer) {
            try {
                writer.write(row)
                writer.newLine()
                writer.flush()
            } catch (_: Exception) {
            }
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Adapt-IQ Research Session Active")
            .setContentText("Collecting anonymous interaction data for $activity")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val notification = createNotification()
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
