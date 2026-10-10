package com.example.adaptiq.admin.sync

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class AdminLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AdminLoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("role") val role: String,
    @SerializedName("email") val email: String
)

data class AdminSessionDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("testId") val testId: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("orientation") val orientation: String,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("status") val status: String,
    @SerializedName("_count") val count: SessionCountDto?
)

data class SessionCountDto(
    @SerializedName("events") val events: Int,
    @SerializedName("sensorSamples") val sensorSamples: Int
)

data class ActionEventDetailDto(
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

data class SensorSampleDetailDto(
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

data class SessionDetailDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("testId") val testId: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("orientation") val orientation: String,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("status") val status: String,
    @SerializedName("events") val events: List<ActionEventDetailDto>,
    @SerializedName("sensorSamples") val sensorSamples: List<SensorSampleDetailDto>
)

interface AdminApi {
    @POST("api/v1/auth/admin/login")
    suspend fun adminLogin(@Body request: AdminLoginRequest): Response<AdminLoginResponse>

    @GET("api/v1/sessions")
    suspend fun getSessions(@Header("Authorization") token: String): Response<List<AdminSessionDto>>

    @GET("api/v1/sessions/{sessionId}")
    suspend fun getSessionDetail(
        @Header("Authorization") token: String,
        @Path("sessionId") sessionId: String
    ): Response<SessionDetailDto>

    @GET("api/v1/sessions/admin/export")
    suspend fun exportDataset(@Header("Authorization") token: String): Response<JsonObject>
}

object AdminApiClient {
    // Configured to Windows PC Wi-Fi LAN address for physical mobile testing.
    // (Ensure your phone and PC are on the same Wi-Fi network).
    private const val BASE_URL = "http://192.168.0.102:4000/"

    val api: AdminApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdminApi::class.java)
    }
}
