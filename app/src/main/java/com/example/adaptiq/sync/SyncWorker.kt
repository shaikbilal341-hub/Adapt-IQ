package com.example.adaptiq.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val queueManager = SyncQueueManager.getInstance(applicationContext)
        val pendingSessions = queueManager.getPendingSessions()

        if (pendingSessions.isEmpty()) {
            return@withContext Result.success()
        }

        var anyFailed = false

        for (session in pendingSessions) {
            try {
                // 1. Obtain anonymous participant token
                val tokenResponse = AdaptIqHttpClient.api.startParticipantSession(
                    ParticipantSessionStartRequest(
                        participantType = "ADULT",
                        condition = "SITTING",
                        ageGroup = "18–25",
                        deviceModel = session.deviceModel ?: "Normalized Android Device"
                    )
                )

                val token = if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                    "Bearer ${tokenResponse.body()!!.token}"
                } else {
                    anyFailed = true
                    continue
                }

                val startTimeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(session.startTime))

                // 2. Register session (idempotent upsert on backend)
                val regRes = AdaptIqHttpClient.api.registerSession(
                    token = token,
                    request = RegisterSessionRequest(
                        sessionId = session.sessionId,
                        testId = session.testId,
                        startTime = startTimeStr,
                        orientation = session.orientation,
                        deviceModel = session.deviceModel,
                        availableSensors = "ACCELEROMETER,GYROSCOPE,PROXIMITY"
                    )
                )

                if (!regRes.isSuccessful && regRes.code() != 409) {
                    anyFailed = true
                    continue
                }

                // 3. Upload events and sensors in chunked batches
                val events = queueManager.getEventsForSession(session.sessionId)
                val sensors = queueManager.getSensorsForSession(session.sessionId)

                val eventChunks = events.chunked(250)
                val sensorChunks = sensors.chunked(500)
                val maxBatches = maxOf(eventChunks.size, sensorChunks.size, 1)

                var batchUploadSuccess = true
                for (batchIdx in 0 until maxBatches) {
                    val batchEvents = if (batchIdx < eventChunks.size) eventChunks[batchIdx] else emptyList()
                    val batchSensors = if (batchIdx < sensorChunks.size) sensorChunks[batchIdx] else emptyList()

                    val uploadRes = AdaptIqHttpClient.api.uploadBatch(
                        token = token,
                        sessionId = session.sessionId,
                        request = UploadBatchRequest(
                            batchIndex = batchIdx,
                            events = batchEvents,
                            sensorSamples = batchSensors
                        )
                    )

                    if (!uploadRes.isSuccessful) {
                        batchUploadSuccess = false
                        break
                    }
                }

                if (!batchUploadSuccess) {
                    anyFailed = true
                    continue
                }

                // 4. Finalize session
                val endTimeMs = session.endTime ?: System.currentTimeMillis()
                val endTimeStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(endTimeMs))
                AdaptIqHttpClient.api.finalizeSession(
                    token = token,
                    sessionId = session.sessionId,
                    request = FinalizeSessionRequest(endTime = endTimeStr)
                )

                // 5. Mark session SYNCED in local queue
                queueManager.markSessionSynced(session.sessionId)

            } catch (e: Exception) {
                anyFailed = true
            }
        }

        if (anyFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }
}
