package com.example.adaptiq

import android.R
import android.annotation.SuppressLint
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

    private var age = 0

    private var ageGroup = ""

    private var participantType = "UNKNOWN"

    private var condition = "SITTING"

    private var activity = "typing"

    @Volatile
    private var eventLabel = "SESSION_STARTED"

    private var deviceModel = ""

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

        const val EXTRA_AGE = "age"

        const val EXTRA_AGE_GROUP = "age_group"

        const val EXTRA_USER_TYPE = "user_type"

        const val EXTRA_CONDITION = "condition"

        const val EXTRA_ACTIVITY = "activity"

        const val EXTRA_EVENT_LABEL = "event_label"

        const val EXTRA_DEVICE_MODEL = "device_model"

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
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            ACTION_START -> {

                sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: "SESSION_0001"

                testId = intent.getStringExtra(EXTRA_TEST_ID) ?: "TEST_001"

                age = intent.getIntExtra(EXTRA_AGE, 0)

                ageGroup = intent.getStringExtra(EXTRA_AGE_GROUP) ?: ""

                participantType = intent.getStringExtra(EXTRA_USER_TYPE) ?: "UNKNOWN"

                condition = intent.getStringExtra(EXTRA_CONDITION) ?: "SITTING"

                activity = intent.getStringExtra(EXTRA_ACTIVITY) ?: "typing"

                eventLabel = intent.getStringExtra(EXTRA_EVENT_LABEL) ?: "SESSION_STARTED"

                deviceModel = intent.getStringExtra(EXTRA_DEVICE_MODEL) ?: ""

                orientation = intent.getStringExtra(EXTRA_ORIENTATION) ?: "portrait"

                eventSequence.set(0L)

                startRecording()
            }

            ACTION_UPDATE_EVENT -> {

                activity = intent.getStringExtra(EXTRA_ACTIVITY) ?: activity

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

                    val dur = intent.getLongExtra(EXTRA_DURATION, 0L)

                    val touch = intent.getStringExtra(EXTRA_TOUCH_INFO) ?: ""

                    val act = intent.getStringExtra(EXTRA_ACTIVITY) ?: activity

                    val orient = intent.getStringExtra(EXTRA_ORIENTATION) ?: orientation

                    val elapsed = SystemClock.elapsedRealtime() - recordingStartTime

                    writeActionEventRow(
                        eventId = eventId,
                        elapsedMs = elapsed,
                        activity = act,
                        orientation = orient,
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

        val directory = getExternalFilesDir("datasets")

        if (directory == null) {
            stopSelf()
            return
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val safeSession = sessionId.replace(Regex("[^A-Za-z0-9_-]"), "_")

        val safeActivity = activity.replace(Regex("[^A-Za-z0-9_-]"), "_")

        csvFile = File(directory, "${safeSession}_${safeActivity}_$time.csv")

        csvWriter = BufferedWriter(FileWriter(csvFile, false))

        // Updated CSV Header matching research specification
        csvWriter?.write(
            "timestamp," +
                    "elapsed_ms," +
                    "session_id," +
                    "test_id," +
                    "event_id," +
                    "activity," +
                    "orientation," +
                    "expected_action," +
                    "detected_action," +
                    "result," +
                    "attempt_id," +
                    "duration_ms," +
                    "touch_info," +
                    "acc_x," +
                    "acc_y," +
                    "acc_z," +
                    "gyro_x," +
                    "gyro_y," +
                    "gyro_z," +
                    "proximity," +
                    "acc_magnitude," +
                    "gyro_magnitude"
        )

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
            "%d,%d,%s,%s,%d,%s,%s,none,sensor_sample,none,0,0,\"\",%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f",
            timestamp,
            elapsedMs,
            csv(sessionId),
            csv(testId),
            eventId,
            csv(activity),
            csv(orientation),
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
        orientation: String,
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String
    ) {
        val writer = csvWriter ?: return

        val timestamp = System.currentTimeMillis()
        val accMag = sqrt((accX * accX) + (accY * accY) + (accZ * accZ))
        val gyroMag = sqrt((gyroX * gyroX) + (gyroY * gyroY) + (gyroZ * gyroZ))

        val row = String.format(
            Locale.US,
            "%d,%d,%s,%s,%d,%s,%s,%s,%s,%s,%d,%d,%s,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f",
            timestamp,
            elapsedMs,
            csv(sessionId),
            csv(testId),
            eventId,
            csv(activity),
            csv(orientation),
            csv(expectedAction),
            csv(detectedAction),
            csv(result),
            attemptId,
            durationMs,
            csv(touchInfo),
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

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AdaptIQ recording active")
            .setContentText("$testId • $activity • $orientation")
            .setSmallIcon(R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification() {
        if (!isRecording) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AdaptIQ Recording",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shows when AdaptIQ is recording sensor data"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
