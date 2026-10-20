package com.example.adaptiq.admin.auth

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.adaptiq.admin.BuildConfig

class AdminAuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "adaptiq_admin_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_ADMIN_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_ADMIN_TOKEN, null)
    }

    fun clearToken() {
        sharedPreferences.edit().remove(KEY_ADMIN_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean {
        return !getToken().isNullOrBlank()
    }

    fun saveServerUrl(url: String) {
        sharedPreferences.edit().putString(KEY_SERVER_URL, url).apply()
    }

    fun getServerUrl(): String {
        val saved = sharedPreferences.getString(KEY_SERVER_URL, null)
        if (!saved.isNullOrBlank()) {
            // If on a physical device, 10.0.2.2 will never connect.
            // If it was saved from the old default, migrate to physical device default URL.
            if (!isEmulator() && saved.contains("10.0.2.2")) {
                val upgraded = BuildConfig.DEFAULT_DEVICE_URL
                saveServerUrl(upgraded)
                return upgraded
            }
            return saved
        }
        return if (isEmulator()) BuildConfig.DEFAULT_EMULATOR_URL else BuildConfig.DEFAULT_DEVICE_URL
    }

    companion object {
        private const val KEY_ADMIN_TOKEN = "admin_jwt_token"
        private const val KEY_SERVER_URL = "admin_server_url"

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
    }
}
