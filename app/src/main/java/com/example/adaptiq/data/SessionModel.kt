package com.example.adaptiq.data

/**
 * Supported research activities in Adapt-IQ
 */
enum class ActivityType(val id: String, val title: String) {
    TYPING("typing", "Typing Activity"),
    GESTURES("gestures", "Gesture Collection"),
    SCROLLING("scrolling", "Scrolling Activity"),
    PORTRAIT_GAMING_RUNNER("portrait_runner", "Portrait Runner Game"),
    PORTRAIT_GAMING_MATCH("portrait_match", "Portrait Match Game"),
    LANDSCAPE_GAMING_DRIVING("landscape_driving", "Landscape Driving Game"),
    MIXED_INTERACTION("mixed_interaction", "Mixed Interaction")
}

/**
 * Status of an activity in the research session
 */
enum class ActivityStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

/**
 * Standardized action event recorded during research sessions
 */
data class ActionEvent(
    val eventId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedMs: Long,
    val activity: String,
    val orientation: String = "portrait",
    val expectedAction: String,
    val detectedAction: String,
    val result: String, // "successful" or "incorrect"
    val attemptId: Int = 1,
    val durationMs: Long = 0L,
    val touchInfo: String = ""
)

/**
 * Research session metadata
 */
data class SessionMetadata(
    val testId: String = "TEST_001",
    val sessionId: String,
    val startTimeMs: Long = System.currentTimeMillis(),
    val availableSensors: String = "",
    val deviceModel: String = "",
    val isPractice: Boolean = false
)
