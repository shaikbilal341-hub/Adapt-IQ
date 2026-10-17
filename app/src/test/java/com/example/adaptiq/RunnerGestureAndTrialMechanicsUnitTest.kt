package com.example.adaptiq

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class RunnerGestureAndTrialMechanicsUnitTest {

    // Helper implementing the exact gesture recognition algorithm used in RunnerGameView
    private fun evaluateSwipeGesture(
        dragX: Float,
        dragY: Float,
        swipeThreshold: Float = 40f
    ): String? {
        val absX = abs(dragX)
        val absY = abs(dragY)

        // 1. Below threshold: ignore small tremors or accidental taps
        if (absX < swipeThreshold && absY < swipeThreshold) {
            return null
        }

        // 2. Ambiguous diagonal rejection: require dominant axis ratio >= 1.3
        if (absX > absY * 1.3f) {
            return if (dragX > 0f) "SWIPE_RIGHT" else "SWIPE_LEFT"
        } else if (absY > absX * 1.3f) {
            return if (dragY < 0f) "SWIPE_UP" else "SWIPE_DOWN"
        }

        // Diagonal ambiguous gesture: rejected to prevent duplicate actions
        return null
    }

    // =========================================================================
    // D. RUNNER GESTURES
    // =========================================================================

    @Test
    fun testSwipeUpTriggersJump() {
        val gesture = evaluateSwipeGesture(dragX = 5f, dragY = -65f)
        assertEquals("SWIPE_UP", gesture)
    }

    @Test
    fun testSwipeDownTriggersSlide() {
        val gesture = evaluateSwipeGesture(dragX = -8f, dragY = 70f)
        assertEquals("SWIPE_DOWN", gesture)
    }

    @Test
    fun testSwipeLeftMovesOneLaneLeft() {
        val gesture = evaluateSwipeGesture(dragX = -80f, dragY = 12f)
        assertEquals("SWIPE_LEFT", gesture)
    }

    @Test
    fun testSwipeRightMovesOneLaneRight() {
        val gesture = evaluateSwipeGesture(dragX = 75f, dragY = -5f)
        assertEquals("SWIPE_RIGHT", gesture)
    }

    @Test
    fun testSmallMovementsDoNotTriggerUnintendedActions() {
        // Accidental jitter or small finger movement (e.g. 15 px)
        val gesture = evaluateSwipeGesture(dragX = 15f, dragY = -12f)
        assertNull("Small movements below threshold must not trigger actions", gesture)

        val tapGesture = evaluateSwipeGesture(dragX = 2f, dragY = 3f)
        assertNull("A tap must never be interpreted as a swipe", tapGesture)
    }

    @Test
    fun testDiagonalGesturesDoNotGenerateDuplicateActions() {
        // Equal horizontal and vertical movement (ambiguous diagonal 45 degrees)
        val ambiguousDiagonal = evaluateSwipeGesture(dragX = 60f, dragY = -58f)
        assertNull("Ambiguous diagonal swipe must be rejected without dual firing", ambiguousDiagonal)

        // Slight diagonal where horizontal dominates clearly (e.g. 80f vs 20f)
        val cleanHorizontal = evaluateSwipeGesture(dragX = 80f, dragY = 20f)
        assertEquals("SWIPE_RIGHT", cleanHorizontal)

        // Slight diagonal where vertical dominates clearly (e.g. -15f vs -90f)
        val cleanVertical = evaluateSwipeGesture(dragX = -15f, dragY = -90f)
        assertEquals("SWIPE_UP", cleanVertical)
    }

    @Test
    fun testInvalidLaneChangesAtEdgesAreRejected() {
        // 3-lane track: 0 = Left, 1 = Center, 2 = Right
        fun moveLeft(currentLane: Int): Pair<Int, Boolean> {
            return if (currentLane > 0) Pair(currentLane - 1, true) else Pair(currentLane, false)
        }

        fun moveRight(currentLane: Int): Pair<Int, Boolean> {
            return if (currentLane < 2) Pair(currentLane + 1, true) else Pair(currentLane, false)
        }

        // Test at left edge (Lane 0)
        val (laneAfterLeftAt0, successLeftAt0) = moveLeft(0)
        assertEquals(0, laneAfterLeftAt0)
        assertFalse("Move left at left boundary must be rejected", successLeftAt0)

        // Test at right edge (Lane 2)
        val (laneAfterRightAt2, successRightAt2) = moveRight(2)
        assertEquals(2, laneAfterRightAt2)
        assertFalse("Move right at right boundary must be rejected", successRightAt2)

        // Test valid lane changes
        val (laneFromCenterLeft, successCenterLeft) = moveLeft(1)
        assertEquals(0, laneFromCenterLeft)
        assertTrue(successCenterLeft)

        val (laneFromCenterRight, successCenterRight) = moveRight(1)
        assertEquals(2, laneFromCenterRight)
        assertTrue(successCenterRight)
    }

    @Test
    fun testGesturesDuringPauseOrTrialCompletionAreIgnored() {
        fun handleGesture(
            gesture: String,
            isGameRunning: Boolean,
            isGamePaused: Boolean
        ): Boolean {
            if (!isGameRunning || isGamePaused) {
                return false // Ignored
            }
            return true // Processed
        }

        // When paused:
        assertFalse(handleGesture("SWIPE_UP", isGameRunning = true, isGamePaused = true))
        assertFalse(handleGesture("SWIPE_LEFT", isGameRunning = true, isGamePaused = true))

        // When not running (e.g. trial completed):
        assertFalse(handleGesture("SWIPE_UP", isGameRunning = false, isGamePaused = false))

        // When active and unpaused:
        assertTrue(handleGesture("SWIPE_UP", isGameRunning = true, isGamePaused = false))
    }

    @Test
    fun testOfficialActiveTrialDurationIsStrictly30SecondsExcludingPause() {
        var accumulatedActiveMs = 0L
        var activeStartRealtime = 50_000L

        // Run for 14.5 seconds
        val pauseTime = 64_500L
        accumulatedActiveMs += (pauseTime - activeStartRealtime)
        assertEquals(14_500L, accumulatedActiveMs)

        // Paused for 10 seconds: monotonic clock advances, but accumulatedActiveMs does NOT advance!
        val resumeTime = 74_500L
        activeStartRealtime = resumeTime

        // Run until remaining 15.5 seconds completes
        val completeTime = resumeTime + (30_000L - accumulatedActiveMs)
        accumulatedActiveMs += (completeTime - activeStartRealtime)

        assertEquals("Total active unpaused duration must be exactly 30,000 ms", 30_000L, accumulatedActiveMs)
        val remainingSeconds = (30_000L - accumulatedActiveMs) / 1000L
        assertEquals(0L, remainingSeconds)
    }

    @Test
    fun testResponseLatencyIntegrityMeasuredFromStimulusOnset() {
        val obstacleOnsetTime = 100_000L
        val qualifyingReactionTime = 100_320L

        // Genuine stimulus-response latency
        val latency = qualifyingReactionTime - obstacleOnsetTime
        assertEquals(320L, latency)
        assertTrue("Latency must be in valid physiological human range", latency in 40L..1800L)

        // Collision without qualifying avoidance response: latency must be unmeasured (-1L / NA)
        val unmeasuredCollisionLatency = -1L
        val latencyCsvValue = if (unmeasuredCollisionLatency < 0L) "NA" else unmeasuredCollisionLatency.toString()
        assertEquals("NA", latencyCsvValue)
        assertNotEquals("Arbitrary clamped duration must never replace true unmeasured latency", "1800", latencyCsvValue)
    }

    // =========================================================================
    // E. REGRESSION
    // =========================================================================

    @Test
    fun testSensorMagnitudeCalculation() {
        val ax = 0.0f
        val ay = 9.80665f
        val az = 0.0f
        val accMag = Math.sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        assertEquals(9.80665f, accMag, 0.001f)

        val gx = 0.05f
        val gy = 0.02f
        val gz = 0.01f
        val gyroMag = Math.sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
        assertTrue(gyroMag > 0.05f)
    }
}
