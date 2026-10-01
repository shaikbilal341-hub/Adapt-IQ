package com.example.adaptiq.sync

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File

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

    fun queueCompletedSession(sessionId: String, testId: String, csvFile: File, orientation: String, deviceModel: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("session_id", sessionId)
                put("test_id", testId)
                put("start_time", System.currentTimeMillis())
                put("orientation", orientation)
                put("device_model", deviceModel)
                put("status", "PENDING")
            }
            db.insertWithOnConflict(TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)

            // Parse CSV file lines and queue events / sensors
            if (csvFile.exists()) {
                val lines = csvFile.readLines()
                if (lines.size > 1) {
                    for (i in 1 until lines.size) {
                        val cols = lines[i].split(",")
                        if (cols.size >= 22) {
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
                                    put("session_id", sessionId)
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
                                put("session_id", sessionId)
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

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateSessionStatus(sessionId: String, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply { put("status", status) }
        db.update(TABLE_SESSIONS, values, "session_id = ?", arrayOf(sessionId))
    }
}
