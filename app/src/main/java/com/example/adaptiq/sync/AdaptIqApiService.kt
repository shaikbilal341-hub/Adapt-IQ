package com.example.adaptiq.sync

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class ParticipantSessionStartRequest(
    @SerializedName("participantType") val participantType: String,
    @SerializedName("condition") val condition: String,
    @SerializedName("ageGroup") val ageGroup: String,
    @SerializedName("deviceModel") val deviceModel: String
)

data class ParticipantSessionStartResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("testId") val testId: String,
    @SerializedName("token") val token: String
)

data class RegisterSessionRequest(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("testId") val testId: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("orientation") val orientation: String,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("availableSensors") val availableSensors: String?
)

data class FinalizeSessionRequest(
    @SerializedName("endTime") val endTime: String
)

data class ActionEventDto(
    @SerializedName("eventId") val eventId: Long,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("elapsedMs") val elapsedMs: Long,
    @SerializedName("activity") val activity: String,
    @SerializedName("orientation") val orientation: String,
    @SerializedName("expectedAction") val expectedAction: String,
    @SerializedName("detectedAction") val detectedAction: String,
    @SerializedName("result") val result: String,
    @SerializedName("attemptId") val attemptId: Int,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("touchInfo") val touchInfo: String?
)

data class SensorSampleDto(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("elapsedMs") val elapsedMs: Long,
    @SerializedName("activity") val activity: String,
    @SerializedName("accX") val accX: Float,
    @SerializedName("accY") val accY: Float,
    @SerializedName("accZ") val accZ: Float,
    @SerializedName("gyroX") val gyroX: Float,
    @SerializedName("gyroY") val gyroY: Float,
    @SerializedName("gyroZ") val gyroZ: Float,
    @SerializedName("proximity") val proximity: Float,
    @SerializedName("accMagnitude") val accMagnitude: Float,
    @SerializedName("gyroMagnitude") val gyroMagnitude: Float
)

data class UploadBatchRequest(
    @SerializedName("batchIndex") val batchIndex: Int,
    @SerializedName("events") val events: List<ActionEventDto>,
    @SerializedName("sensorSamples") val sensorSamples: List<SensorSampleDto>
)

interface AdaptIqApi {
    @POST("api/v1/auth/participant/session/start")
    suspend fun startParticipantSession(@Body request: ParticipantSessionStartRequest): Response<ParticipantSessionStartResponse>

    @POST("api/v1/sessions")
    suspend fun registerSession(
        @Header("Authorization") token: String,
        @Body request: RegisterSessionRequest
    ): Response<Map<String, Any>>

    @POST("api/v1/sessions/{sessionId}/finalize")
    suspend fun finalizeSession(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String,
        @Body request: FinalizeSessionRequest
    ): Response<Map<String, Any>>

    @POST("api/v1/sessions/{sessionId}/upload")
    suspend fun uploadBatch(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String,
        @Body request: UploadBatchRequest
    ): Response<Map<String, Any>>
}

object AdaptIqHttpClient {
    // Configured to Windows PC Wi-Fi LAN address for physical mobile testing.
    // (Ensure your phone and PC are on the same Wi-Fi network).
    private const val BASE_URL = "http://192.168.0.102:4000/"

    val api: AdaptIqApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdaptIqApi::class.java)
    }
}
