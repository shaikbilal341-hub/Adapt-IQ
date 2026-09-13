package com.example.adaptiq

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.adaptiq.ui.theme.AdaptIQTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    // Accelerometer
    private var accX = 0f
    private var accY = 0f
    private var accZ = 0f

    // Gyroscope
    private var gyroX = 0f
    private var gyroY = 0f
    private var gyroZ = 0f

    // Proximity
    private var proximity = 0f

    // Recording state
    private var recording by mutableStateOf(false)
    private var selectedLabel by mutableStateOf("ADULT")
    private var touchCount by mutableStateOf(0)

    // CSV file
    private var csvFile: File? = null

    // Recording timer
    private val handler = Handler(Looper.getMainLooper())

    private val recorder = object : Runnable {
        override fun run() {

            if (recording) {

                writeDataRow()

                // Record every 100 ms = 10 Hz
                handler.postDelayed(this, 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get sensors
        val accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        val gyroscope =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            )

        val proximitySensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_PROXIMITY
            )

        // Start accelerometer
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        // Start gyroscope
        gyroscope?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        // Start proximity
        proximitySensor?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // UI
        setContent {

            AdaptIQTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    RecorderScreen(
                        recording = recording,
                        selectedLabel = selectedLabel,
                        touchCount = touchCount,

                        onLabelChange = { label ->
                            if (!recording) {
                                selectedLabel = label
                            }
                        },

                        onStartRecording = {
                            startRecording()
                        },

                        onStopRecording = {
                            stopRecording()
                        },

                        onTouchDetected = {
                            if (recording) {
                                touchCount++
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event == null) {
            return
        }

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

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // Not required for this experiment
    }

    // ---------------------------------------------------------
    // START RECORDING
    // ---------------------------------------------------------

    private fun startRecording() {

        val directory =
            getExternalFilesDir("datasets")

        if (directory == null) {

            Toast.makeText(
                this,
                "Unable to create dataset folder",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val time =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(Date())

        csvFile = File(
            directory,
            "adaptiq_${selectedLabel}_$time.csv"
        )

        // CSV header
        csvFile?.writeText(
            "timestamp," +
                    "acc_x," +
                    "acc_y," +
                    "acc_z," +
                    "gyro_x," +
                    "gyro_y," +
                    "gyro_z," +
                    "proximity," +
                    "touch_count," +
                    "acc_magnitude," +
                    "gyro_magnitude," +
                    "label\n"
        )

        touchCount = 0

        recording = true

        handler.post(recorder)

        Toast.makeText(
            this,
            "Recording started",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------------------------------------------------
    // STOP RECORDING
    // ---------------------------------------------------------

    private fun stopRecording() {

        recording = false

        handler.removeCallbacks(recorder)

        Toast.makeText(
            this,
            "Recording saved",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ---------------------------------------------------------
    // WRITE CSV DATA
    // ---------------------------------------------------------

    private fun writeDataRow() {

        val file = csvFile ?: return

        val timestamp =
            System.currentTimeMillis()

        // Accelerometer magnitude
        val accMagnitude =
            sqrt(
                (accX * accX) +
                        (accY * accY) +
                        (accZ * accZ)
            )

        // Gyroscope magnitude
        val gyroMagnitude =
            sqrt(
                (gyroX * gyroX) +
                        (gyroY * gyroY) +
                        (gyroZ * gyroZ)
            )

        val row = String.format(
            Locale.US,

            "%d," +
                    "%.5f," +
                    "%.5f," +
                    "%.5f," +
                    "%.5f," +
                    "%.5f," +
                    "%.5f," +
                    "%.5f," +
                    "%d," +
                    "%.5f," +
                    "%.5f," +
                    "%s\n",

            timestamp,

            accX,
            accY,
            accZ,

            gyroX,
            gyroY,
            gyroZ,

            proximity,

            touchCount,

            accMagnitude,

            gyroMagnitude,

            selectedLabel
        )

        file.appendText(row)
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    override fun onDestroy() {

        recording = false

        handler.removeCallbacks(recorder)

        sensorManager.unregisterListener(this)

        super.onDestroy()
    }
}


// =============================================================
// RECORDER SCREEN
// =============================================================

@Composable
fun RecorderScreen(

    recording: Boolean,

    selectedLabel: String,

    touchCount: Int,

    onLabelChange: (String) -> Unit,

    onStartRecording: () -> Unit,

    onStopRecording: () -> Unit,

    onTouchDetected: () -> Unit

) {

    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)

            // CORRECT pointerInput usage
            .pointerInput(recording) {

                detectTapGestures {

                    onTouchDetected()
                }
            },

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        // Title
        Text(
            text = "AdaptIQ",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Sensor Data Recorder",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Label
        Text(
            text = "Session Label",
            style = MaterialTheme.typography.titleMedium
        )

        // Adult / Child
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            LabelButton(
                text = "ADULT",

                selected =
                    selectedLabel == "ADULT",

                enabled =
                    !recording,

                onClick = {
                    onLabelChange("ADULT")
                }
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            LabelButton(
                text = "CHILD",

                selected =
                    selectedLabel == "CHILD",

                enabled =
                    !recording,

                onClick = {
                    onLabelChange("CHILD")
                }
            )
        }

        // Handover / Other
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            LabelButton(
                text = "HANDOVER",

                selected =
                    selectedLabel == "HANDOVER",

                enabled =
                    !recording,

                onClick = {
                    onLabelChange("HANDOVER")
                }
            )

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            LabelButton(
                text = "OTHER",

                selected =
                    selectedLabel == "OTHER",

                enabled =
                    !recording,

                onClick = {
                    onLabelChange("OTHER")
                }
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Current label
        Text(
            text =
                "Current label: $selectedLabel"
        )

        // Touch counter
        Text(
            text =
                "Touch events: $touchCount"
        )

        // Recording status
        Text(
            text =
                if (recording) {
                    "● RECORDING"
                } else {
                    "● READY"
                }
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // Start / Stop
        if (!recording) {

            Button(

                onClick =
                    onStartRecording,

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    text = "START RECORDING"
                )
            }

        } else {

            OutlinedButton(

                onClick =
                    onStopRecording,

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    text = "STOP RECORDING"
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Sampling rate: 10 Hz"
        )

        Text(
            text =
                "Accelerometer + Gyroscope + Proximity"
        )

        Text(
            text =
                "Data is stored locally on the phone."
        )
    }
}


// =============================================================
// LABEL BUTTON
// =============================================================

@Composable
fun LabelButton(

    text: String,

    selected: Boolean,

    enabled: Boolean,

    onClick: () -> Unit

) {

    if (selected) {

        Button(

            onClick = onClick,

            enabled = enabled

        ) {

            Text(
                text = text
            )
        }

    } else {

        OutlinedButton(

            onClick = onClick,

            enabled = enabled

        ) {

            Text(
                text = text
            )
        }
    }
}