package com.example.adaptiq.sync

import android.os.Build
import com.example.adaptiq.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

fun isEmulator(): Boolean {
    return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu"))
}

fun validateProductionEndpoint(url: String): String {
    val trimmed = url.trim()
    if (trimmed.isBlank()) {
        throw IllegalStateException("FATAL: Production release build requires an explicitly configured HTTPS backend URL.")
    }
    if (!trimmed.startsWith("https://", ignoreCase = true)) {
        throw IllegalStateException("FATAL: Production URL must use HTTPS. Insecure HTTP is forbidden in release builds.")
    }
    try {
        val uri = java.net.URI(trimmed)
        val host = uri.host?.lowercase() ?: throw IllegalStateException("FATAL: Production URL is malformed: missing valid host.")
        if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2" ||
            host.startsWith("192.168.") || host.startsWith("10.") ||
            host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")) ||
            host.endsWith(".local") || host.endsWith(".internal")) {
            throw IllegalStateException("FATAL: Production URL cannot target localhost, emulator, or private LAN address ($host).")
        }
    } catch (e: Exception) {
        if (e is IllegalStateException) throw e
        throw IllegalStateException("FATAL: Production URL is invalid: ${e.message}")
    }
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

fun getDefaultBaseUrl(): String {
    if (BuildConfig.IS_RELEASE) {
        return validateProductionEndpoint(BuildConfig.PROD_URL)
    }
    return if (isEmulator()) BuildConfig.DEFAULT_EMULATOR_URL else BuildConfig.DEFAULT_DEVICE_URL
}

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
    @Volatile
    var baseUrl: String = getDefaultBaseUrl()
        set(value) {
            val normalized = if (value.endsWith("/")) value else "$value/"
            if (BuildConfig.IS_RELEASE) {
                validateProductionEndpoint(normalized)
            }
            field = normalized
            _api = createApi(normalized)
        }

    private var _api: AdaptIqApi = createApi(baseUrl)

    val api: AdaptIqApi
        get() = _api

    fun setServerUrl(url: String) {
        baseUrl = url
    }

    private fun createApi(url: String): AdaptIqApi {
        return Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdaptIqApi::class.java)
    }
}

