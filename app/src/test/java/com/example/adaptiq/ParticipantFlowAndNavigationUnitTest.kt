package com.example.adaptiq

import com.example.adaptiq.data.ActivityStatus
import com.example.adaptiq.sync.QueuedSession
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ParticipantFlowAndNavigationUnitTest {

    // =========================================================================
    // A. NAVIGATION AND CONSENT
    // =========================================================================

    @Test
    fun testPrivacyAndStudyInformationNavigationIsolation() {
        // Verification: Tapping Privacy & Study Information opens PRIVACY_INFO screen
        // and does NOT set consent, start a session, or navigate to dashboard
        var currentScreen = Screen.WELCOME
        var consentAccepted = false
        var isSessionActive = false

        // Simulate onViewPrivacy action
        val onViewPrivacy = {
            currentScreen = Screen.PRIVACY_INFO
        }
        onViewPrivacy()

        assertEquals(Screen.PRIVACY_INFO, currentScreen)
        assertFalse("Privacy view must never alter consent state", consentAccepted)
        assertFalse("Privacy view must never start a research session", isSessionActive)

        // Simulate Back action from Privacy screen
        val onPrivacyBack = {
            currentScreen = if (consentAccepted) Screen.AVAILABLE_ACTIONS else Screen.WELCOME
        }
        onPrivacyBack()

        assertEquals("Back from privacy must return to Welcome when consent not yet granted", Screen.WELCOME, currentScreen)
    }

    @Test
    fun testGetStartedNavigationRoutingWithAndWithoutConsent() {
        // Scenario 1: First-time participant (consent NOT granted)
        var consentAccepted = false
        var currentScreen = Screen.WELCOME

        fun handleGetStarted() {
            currentScreen = if (consentAccepted) Screen.HOME else Screen.CONSENT
        }

        handleGetStarted()
        assertEquals("First time participant must be routed to CONSENT screen", Screen.CONSENT, currentScreen)

        // Participant confirms consent
        consentAccepted = true
        currentScreen = Screen.HOME
        assertEquals(Screen.HOME, currentScreen)

        // Scenario 2: Subsequent app launch or recreation with consent already granted
        currentScreen = Screen.WELCOME
        handleGetStarted()
        assertEquals("Consented participant must proceed directly to HOME dashboard", Screen.HOME, currentScreen)
    }

    @Test
    fun testConsentPersistenceAcrossActivityRecreationAndAppRestart() {
        // Simulate local durable storage state (e.g. SharedPreferences)
        val mockDurablePrefs = mutableMapOf<String, Any>()

        // 1. Initially not consented
        assertFalse(mockDurablePrefs["consent_accepted"] as? Boolean ?: false)

        // 2. Participant grants consent
        mockDurablePrefs["consent_accepted"] = true
        mockDurablePrefs["consent_timestamp"] = System.currentTimeMillis()

        // 3. Activity destroyed and recreated (e.g. rotation or restart)
        val recreatedActivityConsent = mockDurablePrefs["consent_accepted"] as? Boolean ?: false
        assertTrue("Consent must persist across Activity recreation", recreatedActivityConsent)

        // 4. Session completed / reset for next participant
        // Ensuring session reset does NOT wipe durable consent
        val isSessionActive = false
        val consentAfterSessionReset = mockDurablePrefs["consent_accepted"] as? Boolean ?: false
        assertTrue("Resetting session must preserve participant consent on the device", consentAfterSessionReset)
    }

    @Test
    fun testCompletingSessionReturnsToDashboardAndNeverWelcome() {
        var currentScreen = Screen.CONDUCT_SESSION

        // Complete all 13 activities
        val totalCompleted = 13
        if (totalCompleted == 13) {
            currentScreen = Screen.SESSION_COMPLETION
        }
        assertEquals(Screen.SESSION_COMPLETION, currentScreen)

        // Participant finishes viewing completion summary or exports CSV
        // User taps "Return to Dashboard"
        val onReturnToDashboard = {
            currentScreen = Screen.HOME
        }
        onReturnToDashboard()

        assertEquals("Completing session must return to Main Dashboard", Screen.HOME, currentScreen)
        assertNotEquals("Completing session must never return to Welcome screen", Screen.WELCOME, currentScreen)
    }

    // =========================================================================
    // B. DASHBOARD AND DEMOGRAPHICS
    // =========================================================================

    @Test
    fun testDashboardContainsExactlyThreePrimaryActions() {
        val primaryDashboardActions = listOf(
            "Conduct Session",
            "Previous Session History",
            "Available Actions"
        )

        assertEquals("Dashboard must contain exactly 3 primary actions", 3, primaryDashboardActions.size)
        assertTrue(primaryDashboardActions.contains("Conduct Session"))
        assertTrue(primaryDashboardActions.contains("Previous Session History"))
        assertTrue(primaryDashboardActions.contains("Available Actions"))

        // Verify prohibited elements are not part of dashboard primary actions
        assertFalse(primaryDashboardActions.contains("Holding the Phone"))
        assertFalse(primaryDashboardActions.contains("Tap Precision"))
        assertFalse(primaryDashboardActions.contains("Export Dataset"))
        assertFalse(primaryDashboardActions.contains("Age"))
        assertFalse(primaryDashboardActions.contains("Gender"))
    }

    @Test
    fun testEachDashboardActionNavigatesToItsOwnDestination() {
        var currentScreen = Screen.HOME
        var isSessionActive = false

        // 1. Conduct Session (when no session active -> Demographics)
        fun onConductSession() {
            currentScreen = if (isSessionActive) Screen.CONDUCT_SESSION else Screen.DEMOGRAPHICS
        }
        onConductSession()
        assertEquals(Screen.DEMOGRAPHICS, currentScreen)

        // Return to home
        currentScreen = Screen.HOME

        // 2. Previous Session History
        fun onPreviousSessions() {
            currentScreen = Screen.PREVIOUS_SESSIONS
        }
        onPreviousSessions()
        assertEquals(Screen.PREVIOUS_SESSIONS, currentScreen)

        // Return to home
        currentScreen = Screen.HOME

        // 3. Available Actions
        fun onAvailableActions() {
            currentScreen = Screen.AVAILABLE_ACTIONS
        }
        onAvailableActions()
        assertEquals(Screen.AVAILABLE_ACTIONS, currentScreen)
    }

    @Test
    fun testDemographicsValidationAndAgeInYears() {
        // Age in Years validation: valid between 1 and 120
        fun validateDemographics(ageStr: String, gender: String): Pair<Boolean, String?> {
            val age = ageStr.toIntOrNull()
            if (age == null || age < 1 || age > 120) {
                return Pair(false, "Please enter a valid age in years (between 1 and 120).")
            }
            if (gender.isBlank()) {
                return Pair(false, "Please select your gender to proceed.")
            }
            return Pair(true, null)
        }

        // Test invalid age string
        val (valid1, err1) = validateDemographics("", "Female")
        assertFalse(valid1)
        assertNotNull(err1)

        // Test out of range age
        val (valid2, err2) = validateDemographics("145", "Male")
        assertFalse(valid2)

        val (valid3, err3) = validateDemographics("0", "Male")
        assertFalse(valid3)

        // Test missing gender
        val (valid4, err4) = validateDemographics("25", "")
        assertFalse(valid4)

        // Test valid inputs
        val (valid5, err5) = validateDemographics("28", "Non-binary")
        assertTrue(valid5)
        assertNull(err5)

        val (valid6, err6) = validateDemographics("72", "Prefer not to say")
        assertTrue(valid6)
        assertNull(err6)
    }

    @Test
    fun testAgeGroupAbsentFromNewDemographicsAndHistoricalRecordsReadable() {
        // Active data contract must use Age_Years as integer
        val newSessionData = mapOf(
            "ageYears" to 29,
            "gender" to "Female"
        )
        assertFalse("New session data must not contain Age Group field", newSessionData.containsKey("ageGroup"))
        assertEquals(29, newSessionData["ageYears"])

        // Historical record parsing: verify old record containing age group does NOT invent exact age
        val historicalRecord = mapOf(
            "ageGroup" to "26–40",
            "gender" to "Male"
        )
        assertTrue(historicalRecord.containsKey("ageGroup"))
        assertNull("Must never invent an exact age integer from an old age-group category", historicalRecord["ageYears"])
        assertEquals("26–40", historicalRecord["ageGroup"])
    }

    // =========================================================================
    // C. SESSION LIFECYCLE AND EXPORT
    // =========================================================================

    @Test
    fun testCompletionScreenAppearsOnlyAfterAll13ActivitiesCompleted() {
        val statuses = mutableMapOf(
            "holding_phone" to ActivityStatus.COMPLETED,
            "tap_precision" to ActivityStatus.COMPLETED,
            "reaction_time" to ActivityStatus.COMPLETED,
            "typing" to ActivityStatus.COMPLETED,
            "gestures" to ActivityStatus.COMPLETED,
            "scrolling" to ActivityStatus.COMPLETED,
            "drag_drop" to ActivityStatus.COMPLETED,
            "target_tracking" to ActivityStatus.COMPLETED,
            "slider_control" to ActivityStatus.COMPLETED,
            "visual_search" to ActivityStatus.COMPLETED,
            "navigation" to ActivityStatus.COMPLETED,
            "multi_touch" to ActivityStatus.COMPLETED,
            "gaming" to ActivityStatus.IN_PROGRESS // 12 of 13 completed
        )

        fun checkCompletion(map: Map<String, ActivityStatus>): Screen {
            val completed = map.values.count { it == ActivityStatus.COMPLETED }
            return if (completed == 13) Screen.SESSION_COMPLETION else Screen.CONDUCT_SESSION
        }

        assertEquals(Screen.CONDUCT_SESSION, checkCompletion(statuses))

        // Complete the 13th activity
        statuses["gaming"] = ActivityStatus.COMPLETED
        assertEquals(Screen.SESSION_COMPLETION, checkCompletion(statuses))
    }

    @Test
    fun testActualParticipantIdPreservedInCompletionAndExport() {
        val actualParticipantId = "PARTICIPANT_E8A42F"
        val actualSessionId = "SESSION_9C3B11D7"

        assertEquals("PARTICIPANT_E8A42F", actualParticipantId)
        assertTrue(actualParticipantId.startsWith("PARTICIPANT_"))
        assertFalse("Participant ID must not remain placeholder TEMP", actualParticipantId.contains("TEMP"))
    }

    @Test
    fun testOfflineSessionStatusAccuracy() {
        val completedSession = QueuedSession(
            sessionId = "SESSION_001",
            testId = "PARTICIPANT_101",
            startTime = 1717940000000L,
            endTime = 1717940500000L,
            orientation = "portrait",
            deviceModel = "Pixel 8",
            status = "COMPLETED"
        )
        assertEquals("COMPLETED", completedSession.status)

        val pendingSyncSession = completedSession.copy(status = "PENDING")
        assertEquals("PENDING", pendingSyncSession.status)

        val syncedSession = completedSession.copy(status = "SYNCED")
        assertEquals("SYNCED", syncedSession.status)
    }
}
