package com.example.adaptiq.data

import android.content.Context

/**
 * Manages durable local storage for participant research consent.
 * Ensures consent is persisted across Activity recreation, process restart,
 * and session completions.
 */
object ConsentManager {
    private const val PREFS_NAME = "adaptiq_consent_prefs"
    private const val KEY_CONSENT_ACCEPTED = "consent_accepted"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"

    fun isConsentAccepted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Also check legacy recording prefs for seamless upgrade
        if (prefs.getBoolean(KEY_CONSENT_ACCEPTED, false)) {
            return true
        }
        val legacyPrefs = context.getSharedPreferences("adaptiq_recording", Context.MODE_PRIVATE)
        return legacyPrefs.getBoolean(KEY_CONSENT_ACCEPTED, false)
    }

    fun setConsentAccepted(context: Context, accepted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONSENT_ACCEPTED, accepted)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()

        // Sync with legacy prefs to ensure any component checking the old key remains consistent
        context.getSharedPreferences("adaptiq_recording", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONSENT_ACCEPTED, accepted)
            .apply()
    }
}
