package com.example.adaptiq

import android.R



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
import kotlin.math.sqrt

class RecordingService :
    Service(),
    SensorEventListener {

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

    // =========================================================
    // SESSION METADATA
    // =========================================================

    private var sessionId = "SESSION_0001"

    private var testId = "Test01"

    private var age = 0

    private var ageGroup = ""

    private var participantType = "UNKNOWN"

    private var condition = "SITTING"

    private var activity = "one_hand_holding"

    @Volatile
    private var eventLabel = "SESSION_STARTED"

    private var deviceModel = ""

    // =========================================================
    // CONSTANTS
    // =========================================================

    companion object {

        const val ACTION_START =
            "com.example.adaptiq.START_RECORDING"

        const val ACTION_STOP =
            "com.example.adaptiq.STOP_RECORDING"

        const val ACTION_UPDATE_EVENT =
            "com.example.adaptiq.UPDATE_EVENT"

        // -----------------------------------------------------
        // SESSION
        // -----------------------------------------------------

        const val EXTRA_SESSION_ID =
            "session_id"

        const val EXTRA_TEST_ID =
            "test_id"

        const val EXTRA_AGE =
            "age"

        const val EXTRA_AGE_GROUP =
            "age_group"

        // -----------------------------------------------------
        // OLD COMPATIBILITY FIELD
        // -----------------------------------------------------

        const val EXTRA_PARTICIPANT_ID =
            "participant_id"

        // -----------------------------------------------------
        // USER
        // -----------------------------------------------------

        const val EXTRA_USER_TYPE =
            "user_type"

        const val EXTRA_CONDITION =
            "condition"

        // -----------------------------------------------------
        // ACTION
        // -----------------------------------------------------

        const val EXTRA_ACTIVITY =
            "activity"

        // -----------------------------------------------------
        // EVENT
        // -----------------------------------------------------

        const val EXTRA_EVENT_LABEL =
            "event_label"

        // -----------------------------------------------------
        // DEVICE
        // -----------------------------------------------------

        const val EXTRA_DEVICE_MODEL =
            "device_model"

        // -----------------------------------------------------
        // PREFERENCES
        // -----------------------------------------------------

        const val PREFS_NAME =
            "adaptiq_recording"

        const val KEY_LAST_CSV =
            "last_csv"

        // -----------------------------------------------------
        // NOTIFICATION
        // -----------------------------------------------------

        private const val CHANNEL_ID =
            "AdaptIQRecording"

        private const val NOTIFICATION_ID =
            1001
    }

    // =========================================================
    // SERVICE CREATED
    // =========================================================

    override fun onCreate() {

        super.onCreate()

        sensorManager =
            getSystemService(
                SENSOR_SERVICE
            ) as SensorManager

        createNotificationChannel()
    }

    // =========================================================
    // SERVICE COMMAND
    // =========================================================

    override fun onStartCommand(

        intent: Intent?,

        flags: Int,

        startId: Int

    ): Int {

        when (intent?.action) {

            // -------------------------------------------------
            // START
            // -------------------------------------------------

            ACTION_START -> {

                sessionId =
                    intent.getStringExtra(
                        EXTRA_SESSION_ID
                    )
                        ?: "SESSION_0001"

                testId =
                    intent.getStringExtra(
                        EXTRA_TEST_ID
                    )
                        ?: "Test01"

                age =
                    intent.getIntExtra(
                        EXTRA_AGE,
                        0
                    )

                ageGroup =
                    intent.getStringExtra(
                        EXTRA_AGE_GROUP
                    )
                        ?: ""

                participantType =
                    intent.getStringExtra(
                        EXTRA_USER_TYPE
                    )
                        ?: "UNKNOWN"

                condition =
                    intent.getStringExtra(
                        EXTRA_CONDITION
                    )
                        ?: "SITTING"

                activity =
                    intent.getStringExtra(
                        EXTRA_ACTIVITY
                    )
                        ?: "one_hand_holding"

                eventLabel =
                    intent.getStringExtra(
                        EXTRA_EVENT_LABEL
                    )
                        ?: "SESSION_STARTED"

                deviceModel =
                    intent.getStringExtra(
                        EXTRA_DEVICE_MODEL
                    )
                        ?: ""

                startRecording()
            }

            // -------------------------------------------------
            // UPDATE EVENT
            // -------------------------------------------------

            ACTION_UPDATE_EVENT -> {

                eventLabel =
                    intent.getStringExtra(
                        EXTRA_EVENT_LABEL
                    )
                        ?: eventLabel

                updateNotification()
            }

            // -------------------------------------------------
            // STOP
            // -------------------------------------------------

            ACTION_STOP -> {

                stopRecording()

                stopForeground(
                    STOP_FOREGROUND_REMOVE
                )

                stopSelf()
            }
        }

        return START_STICKY
    }

    // =========================================================
    // START RECORDING
    // =========================================================

    private fun startRecording() {

        if (isRecording) {
            return
        }

        val directory =
            getExternalFilesDir(
                "datasets"
            )

        if (directory == null) {

            stopSelf()

            return
        }

        if (!directory.exists()) {

            directory.mkdirs()
        }

        val time =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(
                Date()
            )

        val safeSession =
            sessionId.replace(
                Regex(
                    "[^A-Za-z0-9_-]"
                ),
                "_"
            )

        val safeActivity =
            activity.replace(
                Regex(
                    "[^A-Za-z0-9_-]"
                ),
                "_"
            )

        csvFile =
            File(

                directory,

                "${safeSession}_${safeActivity}_$time.csv"
            )

        // -----------------------------------------------------
        // OPEN CSV
        // -----------------------------------------------------

        csvWriter =
            BufferedWriter(
                FileWriter(
                    csvFile,
                    false
                )
            )

        // -----------------------------------------------------
        // NEW CSV HEADER
        // -----------------------------------------------------

        csvWriter?.write(

            "timestamp," +
                    "elapsed_ms," +
                    "session_id," +
                    "test_id," +
                    "age," +
                    "age_group," +
                    "participant_type," +
                    "condition," +
                    "device_model," +
                    "sampling_rate," +
                    "action_label," +
                    "event_label," +
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

        // -----------------------------------------------------
        // ACCELEROMETER
        // -----------------------------------------------------

        sensorManager
            .getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )
            ?.let {

                sensorManager.registerListener(

                    this,

                    it,

                    SensorManager.SENSOR_DELAY_GAME
                )
            }

        // -----------------------------------------------------
        // GYROSCOPE
        // -----------------------------------------------------

        sensorManager
            .getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            )
            ?.let {

                sensorManager.registerListener(

                    this,

                    it,

                    SensorManager.SENSOR_DELAY_GAME
                )
            }

        // -----------------------------------------------------
        // PROXIMITY
        // -----------------------------------------------------

        sensorManager
            .getDefaultSensor(
                Sensor.TYPE_PROXIMITY
            )
            ?.let {

                sensorManager.registerListener(

                    this,

                    it,

                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }

        // -----------------------------------------------------
        // RECORDING START
        // -----------------------------------------------------

        isRecording = true

        recordingStartTime =
            SystemClock.elapsedRealtime()

        // -----------------------------------------------------
        // FOREGROUND SERVICE
        // -----------------------------------------------------

        startForeground(

            NOTIFICATION_ID,

            createNotification()
        )

        // -----------------------------------------------------
        // RECORDING LOOP
        //
        // Existing architecture preserved:
        // approximately one row every 100 ms = ~10 Hz
        // -----------------------------------------------------

        recordingThread =
            Thread {

                while (isRecording) {

                    val elapsed =
                        SystemClock
                            .elapsedRealtime() -
                                recordingStartTime

                    writeDataRow(
                        elapsed
                    )

                    try {

                        Thread.sleep(
                            100
                        )

                    } catch (
                        _: InterruptedException
                    ) {

                        break
                    }
                }
            }

        recordingThread?.start()
    }

    // =========================================================
    // STOP RECORDING
    // =========================================================

    private fun stopRecording() {

        if (!isRecording) {
            return
        }

        // -----------------------------------------------------
        // STOP NEW ROWS
        // -----------------------------------------------------

        isRecording = false

        // -----------------------------------------------------
        // STOP THREAD
        // -----------------------------------------------------

        recordingThread?.interrupt()

        try {

            recordingThread?.join(
                1000
            )

        } catch (
            _: InterruptedException
        ) {
        }

        recordingThread = null

        // -----------------------------------------------------
        // STOP SENSORS
        // -----------------------------------------------------

        sensorManager.unregisterListener(
            this
        )

        // -----------------------------------------------------
        // CLOSE CSV
        // -----------------------------------------------------

        try {

            csvWriter?.flush()

            csvWriter?.close()

        } catch (
            _: Exception
        ) {
        }

        csvWriter = null

        // -----------------------------------------------------
        // SAVE LAST CSV PATH
        // -----------------------------------------------------

        csvFile?.let {

                file ->

            if (file.exists()) {

                getSharedPreferences(

                    PREFS_NAME,

                    MODE_PRIVATE

                )
                    .edit()
                    .putString(
                        KEY_LAST_CSV,
                        file.absolutePath
                    )
                    .apply()
            }
        }

        csvFile = null
    }

    // =========================================================
    // SENSOR DATA
    // =========================================================

    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        if (event == null) {
            return
        }

        when (
            event.sensor.type
        ) {

            // -------------------------------------------------
            // ACCELEROMETER
            // -------------------------------------------------

            Sensor.TYPE_ACCELEROMETER -> {

                accX =
                    event.values[0]

                accY =
                    event.values[1]

                accZ =
                    event.values[2]
            }

            // -------------------------------------------------
            // GYROSCOPE
            // -------------------------------------------------

            Sensor.TYPE_GYROSCOPE -> {

                gyroX =
                    event.values[0]

                gyroY =
                    event.values[1]

                gyroZ =
                    event.values[2]
            }

            // -------------------------------------------------
            // PROXIMITY
            // -------------------------------------------------

            Sensor.TYPE_PROXIMITY -> {

                proximity =
                    event.values[0]
            }
        }
    }

    // =========================================================
    // SENSOR ACCURACY
    // =========================================================

    override fun onAccuracyChanged(

        sensor: Sensor?,

        accuracy: Int

    ) {
    }

    // =========================================================
    // WRITE DATA ROW
    // =========================================================

    private fun writeDataRow(

        elapsedMs: Long

    ) {

        val writer =
            csvWriter ?: return

        val timestamp =
            System.currentTimeMillis()

        // -----------------------------------------------------
        // ACCELEROMETER MAGNITUDE
        // -----------------------------------------------------

        val accMagnitude =
            sqrt(

                (accX * accX) +

                        (accY * accY) +

                        (accZ * accZ)
            )

        // -----------------------------------------------------
        // GYROSCOPE MAGNITUDE
        // -----------------------------------------------------

        val gyroMagnitude =
            sqrt(

                (gyroX * gyroX) +

                        (gyroY * gyroY) +

                        (gyroZ * gyroZ)
            )

        // -----------------------------------------------------
        // CSV ROW
        // -----------------------------------------------------

        val row =
            String.format(

                Locale.US,

                "%d,%d,%s,%s,%d,%s,%s,%s,%s,10,%s,%s," +
                        "%.5f,%.5f,%.5f," +
                        "%.5f,%.5f,%.5f," +
                        "%.5f,%.5f,%.5f",

                timestamp,

                elapsedMs,

                csv(sessionId),

                csv(testId),

                age,

                csv(ageGroup),

                csv(participantType),

                csv(condition),

                csv(deviceModel),

                csv(activity),

                csv(eventLabel),

                accX,

                accY,

                accZ,

                gyroX,

                gyroY,

                gyroZ,

                proximity,

                accMagnitude,

                gyroMagnitude
            )

        // -----------------------------------------------------
        // WRITE
        // -----------------------------------------------------

        synchronized(writer) {

            try {

                writer.write(
                    row
                )

                writer.newLine()

                writer.flush()

            } catch (
                _: Exception
            ) {
            }
        }
    }

    // =========================================================
    // CSV ESCAPING
    // =========================================================

    private fun csv(
        value: String
    ): String {

        val escaped =
            value.replace(
                "\"",
                "\"\""
            )

        return if (

            escaped.contains(",") ||

            escaped.contains("\"")

        ) {

            "\"$escaped\""

        } else {

            escaped
        }
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private fun createNotification(): Notification {

        return NotificationCompat.Builder(

            this,

            CHANNEL_ID

        )
            .setContentTitle(
                "AdaptIQ recording active"
            )
            .setContentText(

                "$testId • " +
                        "$participantType • " +
                        "$activity • " +
                        eventLabel
            )
            .setSmallIcon(
                R.drawable
                    .ic_menu_info_details
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat
                    .PRIORITY_LOW
            )
            .build()
    }

    // =========================================================
    // UPDATE NOTIFICATION
    // =========================================================

    private fun updateNotification() {

        if (!isRecording) {
            return
        }

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.notify(

            NOTIFICATION_ID,

            createNotification()
        )
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(

                CHANNEL_ID,

                "AdaptIQ Recording",

                NotificationManager
                    .IMPORTANCE_LOW
            )

        channel.description =
            "Shows when AdaptIQ is recording sensor data"

        val manager =
            getSystemService(
                NotificationManager::class.java
            )

        manager.createNotificationChannel(
            channel
        )
    }

    // =========================================================
    // BINDER
    // =========================================================

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroy() {

        stopRecording()

        super.onDestroy()
    }
}
