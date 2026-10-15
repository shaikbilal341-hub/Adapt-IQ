package com.example.adaptiq.sync

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

data class QueuedSession(
    val sessionId: String,
    val testId: String,
    val startTime: Long,
    val endTime: Long?,
    val orientation: String,
    val deviceModel: String?,
    val status: String
)

class SyncQueueManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "adaptiq_sync_queue.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_SESSIONS = "sync_sessions"
        private const val TABLE_EVENTS = "sync_events"
        private const val TABLE_SENSORS = "sync_sensors"

        @Volatile
        private var INSTANCE: SyncQueueManager? = null

        fun getInstance(context: Context): SyncQueueManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncQueueManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun parseCsvLine(line: String): List<String> {
            val tokens = mutableListOf<String>()
            var inQuotes = false
            val sb = StringBuilder()
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == '\"') {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == ',' && !inQuotes) {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                } else {
                    sb.append(c)
                }
                i++
            }
            tokens.add(sb.toString().trim())
            return tokens
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SESSIONS (
                session_id TEXT PRIMARY KEY,
                test_id TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                orientation TEXT,
                device_model TEXT,
                status TEXT NOT NULL
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                event_id INTEGER PRIMARY KEY,
                session_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                elapsed_ms INTEGER NOT NULL,
                activity TEXT NOT NULL,
                orientation TEXT NOT NULL,
                expected_action TEXT NOT NULL,
                detected_action TEXT NOT NULL,
                result TEXT NOT NULL,
                attempt_id INTEGER NOT NULL,
                duration_ms INTEGER NOT NULL,
                touch_info TEXT
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SENSORS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                elapsed_ms INTEGER NOT NULL,
                activity TEXT NOT NULL,
                acc_x REAL NOT NULL,
                acc_y REAL NOT NULL,
                acc_z REAL NOT NULL,
                gyro_x REAL NOT NULL,
                gyro_y REAL NOT NULL,
                gyro_z REAL NOT NULL,
                proximity REAL NOT NULL,
                acc_magnitude REAL NOT NULL,
                gyro_magnitude REAL NOT NULL
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SENSORS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        onCreate(db)
    }

    fun queueCompletedSession(
        sessionId: String,
        testId: String,
        csvFile: File,
        orientation: String = "portrait",
        deviceModel: String = "Normalized Android Device"
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            var effectiveSessionId = sessionId
            var effectiveTestId = testId

            if (csvFile.exists()) {
                val lines = csvFile.readLines()
                if (lines.isNotEmpty()) {
                    val headerLine = lines[0]
                    val isEasyAnalysis = headerLine.contains("Age_Years") || headerLine.contains("Participant_ID")

                    for (i in 1 until lines.size) {
                        val line = lines[i].trim()
                        if (line.isBlank()) continue
                        val cols = parseCsvLine(line)

                        if (isEasyAnalysis && cols.size >= 25) {
                            // Easy Analysis format (25 cols)
                            // 0: Age_Years, 1: Gender, 2: Session_Name, 3: Participant_ID, 4: Session_ID,
                            // 5: Activity_Name, 6: Game_Mode, 7: Attempt_ID, 8: Event_ID, 9: Timestamp_UTC,
                            // 10: Elapsed_Time_ms, 11: Expected_Action, 12: Detected_Action, 13: Outcome,
                            // 14: Response_Latency_ms, 15: Touch_Info, 16..24: Sensors
                            val pId = cols[3].replace("\"", "").trim()
                            val sId = cols[4].replace("\"", "").trim()
                            if (pId.isNotBlank() && effectiveTestId.startsWith("PARTICIPANT_TEMP")) effectiveTestId = pId
                            if (sId.isNotBlank() && effectiveSessionId.isBlank()) effectiveSessionId = sId

                            val activity = cols[5].replace("\"", "").trim()
                            val gameMode = cols[6].replace("\"", "").trim()
                            val fullActivity = if (gameMode.isNotBlank() && gameMode != "standard") "$activity:$gameMode" else activity
                            val attemptId = cols[7].toIntOrNull() ?: 1
                            val eventId = cols[8].toLongOrNull() ?: 0L
                            val timestamp = cols[9].toLongOrNull() ?: System.currentTimeMillis()
                            val elapsedMs = cols[10].toLongOrNull() ?: 0L
                            val expectedAction = cols[11].replace("\"", "").trim()
                            val detectedAction = cols[12].replace("\"", "").trim()
                            val outcome = cols[13].replace("\"", "").trim()
                            val latency = cols[14].toLongOrNull() ?: -1L
                            val touchInfo = cols[15].replace("\"", "").trim()

                            if (detectedAction == "sensor_sample") {
                                val accX = cols[16].toFloatOrNull() ?: 0f
                                val accY = cols[17].toFloatOrNull() ?: 0f
                                val accZ = cols[18].toFloatOrNull() ?: 0f
                                val gyroX = cols[19].toFloatOrNull() ?: 0f
                                val gyroY = cols[20].toFloatOrNull() ?: 0f
                                val gyroZ = cols[21].toFloatOrNull() ?: 0f
                                val proximity = cols[22].toFloatOrNull() ?: 0f
                                val accMag = cols[23].toFloatOrNull() ?: 0f
                                val gyroMag = cols[24].toFloatOrNull() ?: 0f

                                val sensValues = ContentValues().apply {
                                    put("session_id", effectiveSessionId)
                                    put("timestamp", timestamp)
                                    put("elapsed_ms", elapsedMs)
                                    put("activity", fullActivity)
                                    put("acc_x", accX)
                                    put("acc_y", accY)
                                    put("acc_z", accZ)
                                    put("gyro_x", gyroX)
                                    put("gyro_y", gyroY)
                                    put("gyro_z", gyroZ)
                                    put("proximity", proximity)
                                    put("acc_magnitude", accMag)
                                    put("gyro_magnitude", gyroMag)
                                }
                                db.insert(TABLE_SENSORS, null, sensValues)
                            } else if (eventId > 0L) {
                                val evValues = ContentValues().apply {
                                    put("event_id", eventId)
                                    put("session_id", effectiveSessionId)
                                    put("timestamp", timestamp)
                                    put("elapsed_ms", elapsedMs)
                                    put("activity", fullActivity)
                                    put("orientation", orientation)
                                    put("expected_action", expectedAction)
                                    put("detected_action", detectedAction)
                                    put("result", outcome)
                                    put("attempt_id", attemptId)
                                    put("duration_ms", latency)
                                    put("touch_info", touchInfo)
                                }
                                db.insertWithOnConflict(TABLE_EVENTS, null, evValues, SQLiteDatabase.CONFLICT_REPLACE)
                            }
                        } else if (cols.size >= 22) {
                            // Legacy format (22 cols)
                            val timestamp = cols[0].toLongOrNull() ?: 0L
                            val elapsedMs = cols[1].toLongOrNull() ?: 0L
                            val eventId = cols[4].toLongOrNull() ?: 0L
                            val activity = cols[5]
                            val orient = cols[6]
                            val expectedAction = cols[7]
                            val detectedAction = cols[8]
                            val result = cols[9]
                            val attemptId = cols[10].toIntOrNull() ?: 1
                            val durationMs = cols[11].toLongOrNull() ?: 0L
                            val touchInfo = cols[12]
                            val accX = cols[13].toFloatOrNull() ?: 0f
                            val accY = cols[14].toFloatOrNull() ?: 0f
                            val accZ = cols[15].toFloatOrNull() ?: 0f
                            val gyroX = cols[16].toFloatOrNull() ?: 0f
                            val gyroY = cols[17].toFloatOrNull() ?: 0f
                            val gyroZ = cols[18].toFloatOrNull() ?: 0f
                            val proximity = cols[19].toFloatOrNull() ?: 0f
                            val accMag = cols[20].toFloatOrNull() ?: 0f
                            val gyroMag = cols[21].toFloatOrNull() ?: 0f

                            if (eventId > 0 && detectedAction != "sensor_sample") {
                                val evValues = ContentValues().apply {
                                    put("event_id", eventId)
                                    put("session_id", effectiveSessionId)
                                    put("timestamp", timestamp)
                                    put("elapsed_ms", elapsedMs)
                                    put("activity", activity)
                                    put("orientation", orient)
                                    put("expected_action", expectedAction)
                                    put("detected_action", detectedAction)
                                    put("result", result)
                                    put("attempt_id", attemptId)
                                    put("duration_ms", durationMs)
                                    put("touch_info", touchInfo)
                                }
                                db.insertWithOnConflict(TABLE_EVENTS, null, evValues, SQLiteDatabase.CONFLICT_REPLACE)
                            }

                            val sensValues = ContentValues().apply {
                                put("session_id", effectiveSessionId)
                                put("timestamp", timestamp)
                                put("elapsed_ms", elapsedMs)
                                put("activity", activity)
                                put("acc_x", accX)
                                put("acc_y", accY)
                                put("acc_z", accZ)
                                put("gyro_x", gyroX)
                                put("gyro_y", gyroY)
                                put("gyro_z", gyroZ)
                                put("proximity", proximity)
                                put("acc_magnitude", accMag)
                                put("gyro_magnitude", gyroMag)
                            }
                            db.insert(TABLE_SENSORS, null, sensValues)
                        }
                    }
                }
            }

            val sessionValues = ContentValues().apply {
                put("session_id", effectiveSessionId)
                put("test_id", effectiveTestId)
                put("start_time", System.currentTimeMillis())
                put("orientation", orientation)
                put("device_model", deviceModel)
                put("status", "PENDING")
            }
            db.insertWithOnConflict(TABLE_SESSIONS, null, sessionValues, SQLiteDatabase.CONFLICT_REPLACE)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getPendingSessions(): List<QueuedSession> {
        val list = mutableListOf<QueuedSession>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SESSIONS,
            null,
            "status = ?",
            arrayOf("PENDING"),
            null,
            null,
            "start_time ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    QueuedSession(
                        sessionId = it.getString(it.getColumnIndexOrThrow("session_id")),
                        testId = it.getString(it.getColumnIndexOrThrow("test_id")),
                        startTime = it.getLong(it.getColumnIndexOrThrow("start_time")),
                        endTime = if (it.isNull(it.getColumnIndexOrThrow("end_time"))) null else it.getLong(it.getColumnIndexOrThrow("end_time")),
                        orientation = it.getString(it.getColumnIndexOrThrow("orientation")),
                        deviceModel = it.getString(it.getColumnIndexOrThrow("device_model")),
                        status = it.getString(it.getColumnIndexOrThrow("status"))
                    )
                )
            }
        }
        return list
    }

    fun getEventsForSession(sessionId: String): List<ActionEventDto> {
        val list = mutableListOf<ActionEventDto>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EVENTS,
            null,
            "session_id = ?",
            arrayOf(sessionId),
            null,
            null,
            "timestamp ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ActionEventDto(
                        eventId = it.getLong(it.getColumnIndexOrThrow("event_id")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        elapsedMs = it.getLong(it.getColumnIndexOrThrow("elapsed_ms")),
                        activity = it.getString(it.getColumnIndexOrThrow("activity")),
                        orientation = it.getString(it.getColumnIndexOrThrow("orientation")),
                        expectedAction = it.getString(it.getColumnIndexOrThrow("expected_action")),
                        detectedAction = it.getString(it.getColumnIndexOrThrow("detected_action")),
                        result = it.getString(it.getColumnIndexOrThrow("result")),
                        attemptId = it.getInt(it.getColumnIndexOrThrow("attempt_id")),
                        durationMs = it.getLong(it.getColumnIndexOrThrow("duration_ms")),
                        touchInfo = it.getString(it.getColumnIndexOrThrow("touch_info"))
                    )
                )
            }
        }
        return list
    }

    fun getSensorsForSession(sessionId: String): List<SensorSampleDto> {
        val list = mutableListOf<SensorSampleDto>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SENSORS,
            null,
            "session_id = ?",
            arrayOf(sessionId),
            null,
            null,
            "timestamp ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SensorSampleDto(
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp")),
                        elapsedMs = it.getLong(it.getColumnIndexOrThrow("elapsed_ms")),
                        activity = it.getString(it.getColumnIndexOrThrow("activity")),
                        accX = it.getFloat(it.getColumnIndexOrThrow("acc_x")),
                        accY = it.getFloat(it.getColumnIndexOrThrow("acc_y")),
                        accZ = it.getFloat(it.getColumnIndexOrThrow("acc_z")),
                        gyroX = it.getFloat(it.getColumnIndexOrThrow("gyro_x")),
                        gyroY = it.getFloat(it.getColumnIndexOrThrow("gyro_y")),
                        gyroZ = it.getFloat(it.getColumnIndexOrThrow("gyro_z")),
                        proximity = it.getFloat(it.getColumnIndexOrThrow("proximity")),
                        accMagnitude = it.getFloat(it.getColumnIndexOrThrow("acc_magnitude")),
                        gyroMagnitude = it.getFloat(it.getColumnIndexOrThrow("gyro_magnitude"))
                    )
                )
            }
        }
        return list
    }

    fun markSessionSynced(sessionId: String) {
        updateSessionStatus(sessionId, "SYNCED")
    }

    fun updateSessionStatus(sessionId: String, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply { put("status", status) }
        db.update(TABLE_SESSIONS, values, "session_id = ?", arrayOf(sessionId))
    }
}
