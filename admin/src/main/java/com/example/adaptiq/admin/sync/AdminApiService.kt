package com.example.adaptiq.admin.sync

import com.example.adaptiq.admin.BuildConfig
import com.example.adaptiq.admin.auth.AdminAuthManager
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

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
    @GET("health")
    suspend fun checkHealth(): Response<JsonObject>

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
    @Volatile
    var baseUrl: String = if (BuildConfig.IS_RELEASE && BuildConfig.PROD_URL.isNotBlank()) {
        BuildConfig.PROD_URL
    } else if (AdminAuthManager.isEmulator()) {
        BuildConfig.DEFAULT_EMULATOR_URL
    } else {
        BuildConfig.DEFAULT_DEVICE_URL
    }
        set(value) {
            val normalized = if (value.endsWith("/")) value else "$value/"
            field = normalized
            _api = createApi(normalized)
        }

    private var _api: AdminApi = createApi(baseUrl)

    val api: AdminApi
        get() = _api

    fun setServerUrl(url: String) {
        baseUrl = url
    }

    private fun createApi(url: String): AdminApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdminApi::class.java)
    }
}

