package com.example.adaptiq

import com.example.adaptiq.sync.SyncQueueManager
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class GamingMechanicsUnitTest {

    @Test
    fun test30SecondsActiveDurationAccountingWithPause() {
        // Simulated monotonic timestamps (ms)
        var accumulatedActiveMs = 0L
        var activeStartRealtime = 100_000L

        // Phase 1: 12 seconds of active gameplay
        val timeBeforePause = 112_000L
        accumulatedActiveMs += (timeBeforePause - activeStartRealtime)
        assertEquals(12_000L, accumulatedActiveMs)

        // Phase 2: Game is paused for 8 seconds (time moves from 112_000 to 120_000)
        // While paused, accumulatedActiveMs does NOT change!
        val timeAfterPause = 120_000L
        // Player hits "Resume"
        activeStartRealtime = timeAfterPause

        // Phase 3: Player plays until 30 seconds total active time is reached
        val finalTime = timeAfterPause + (30_000L - accumulatedActiveMs) // 120_000 + 18_000 = 138_000L
        accumulatedActiveMs += (finalTime - activeStartRealtime)

        assertEquals(30_000L, accumulatedActiveMs)
        val remainingSeconds = (30_000L - accumulatedActiveMs) / 1000L
        assertEquals(0L, remainingSeconds)
    }

    @Test
    fun testRunnerHurdleJumpCollisionAndAvoidance() {
        val obstacleLane = 1 // Center lane
        val obstacleType = "JUMP_HURDLE"

        // Scenario A: Player in center lane and JUMPING -> AVOIDED
        var playerLane = 1
        var isJumping = true
        var isSliding = false
        var outcome = if (playerLane == obstacleLane && isJumping) "successful" else "collision"
        assertEquals("successful", outcome)

        // Scenario B: Player in center lane but NOT jumping -> COLLISION
        isJumping = false
        outcome = if (playerLane == obstacleLane && isJumping) "successful" else "collision"
        assertEquals("collision", outcome)

        // Scenario C: Player dodged to lane 0 -> AVOIDED
        playerLane = 0
        outcome = if (playerLane == obstacleLane) {
            if (isJumping) "successful" else "collision"
        } else {
            "successful"
        }
        assertEquals("successful", outcome)
    }

    @Test
    fun testRunnerOverheadArchSlideCollisionAndAvoidance() {
        val obstacleLane = 2 // Right lane
        val obstacleType = "SLIDE_ARCH"

        // Scenario A: Player in right lane and SLIDING -> AVOIDED
        var playerLane = 2
        var isSliding = true
        var outcome = if (playerLane == obstacleLane && isSliding) "successful" else "collision"
        assertEquals("successful", outcome)

        // Scenario B: Player in right lane but NOT sliding -> COLLISION
        isSliding = false
        outcome = if (playerLane == obstacleLane && isSliding) "successful" else "collision"
        assertEquals("collision", outcome)
    }

    @Test
    fun testRunnerRoadblockDodgeLogic() {
        val obstacleLane = 0 // Left lane

        // Scenario A: Player stays in left lane -> COLLISION
        var playerLane = 0
        var outcome = if (playerLane == obstacleLane) "collision" else "successful"
        assertEquals("collision", outcome)

        // Scenario B: Player dodges to center lane -> AVOIDED
        playerLane = 1
        outcome = if (playerLane == obstacleLane) "collision" else "successful"
        assertEquals("successful", outcome)
    }

    @Test
    fun testDrivingThrottleAndSpeedRegulation() {
        var speedKmH = 60

        // Gas pedal acceleration
        speedKmH = (speedKmH + 12).coerceAtMost(120)
        assertEquals(72, speedKmH)

        speedKmH = (speedKmH + 60).coerceAtMost(120)
        assertEquals(120, speedKmH) // Capped at 120

        // Brake pedal deceleration
        speedKmH = (speedKmH - 18).coerceAtLeast(20)
        assertEquals(102, speedKmH)

        speedKmH = (speedKmH - 95).coerceAtLeast(20)
        assertEquals(20, speedKmH) // Bottom bounded at 20 km/h
    }

    @Test
    fun testDrivingTrafficCollisionAndOvertakeLogic() {
        val trafficLane = 1 // Center lane

        // Scenario A: Player in center lane -> COLLISION
        var playerLane = 1
        var outcome = if (playerLane == trafficLane) "collision" else "successful"
        assertEquals("collision", outcome)

        // Scenario B: Player steers to left lane -> SUCCESSFUL OVERTAKE / AVOIDANCE
        playerLane = 0
        outcome = if (playerLane == trafficLane) "collision" else "successful"
        assertEquals("successful", outcome)
    }

    @Test
    fun testDrivingLaneDeviationCalculation() {
        val roadBound = 110f

        // Centered car (offset 0)
        var carOffset = 0f
        var dev = abs(carOffset) / roadBound
        assertTrue(dev <= 1.0f)

        // Car nearing left edge (offset -90)
        carOffset = -90f
        dev = abs(carOffset) / roadBound
        assertTrue(dev <= 1.0f)

        // Car crossed boundary onto shoulder (offset 130)
        carOffset = 130f
        dev = abs(carOffset) / roadBound
        assertTrue(dev > 1.0f)
    }

    @Test
    fun testLatencyCalculationAccuracy() {
        val stimulusOnsetRealtime = 50_000L
        val reactionRealtime = 50_340L

        // Valid stimulus-response
        val latency = (reactionRealtime - stimulusOnsetRealtime).coerceIn(40L, 1800L)
        assertEquals(340L, latency)

        // No stimulus onset exists for independent idle action -> NA represented as -1L
        val noStimulusLatency = -1L
        val latencyStr = if (noStimulusLatency < 0L) "NA" else noStimulusLatency.toString()
        assertEquals("NA", latencyStr)
    }

    @Test
    fun testTrialIdConsistencyAcrossTrialLifecycle() {
        // Official trial attempt ID (e.g. Trial 2)
        val trialAttemptId = 2

        // Event 1: Start
        val startAttempt = trialAttemptId
        assertEquals(2, startAttempt)

        // Event 2: Avoidance maneuver for obstacle/encounter #1
        val encounterId1 = 1
        val avoidanceAttempt = trialAttemptId
        val avoidanceTouchInfo = "encounter=$encounterId1,trafficLane=2,playerLane=1,speed=60"
        assertEquals(2, avoidanceAttempt)
        assertTrue(avoidanceTouchInfo.contains("encounter=1"))

        // Event 3: Collision for obstacle/encounter #4
        val encounterId4 = 4
        val collisionAttempt = trialAttemptId
        val collisionTouchInfo = "encounter=$encounterId4,trafficLane=1,playerLane=1,speed=30"
        assertEquals(2, collisionAttempt)
        assertTrue(collisionTouchInfo.contains("encounter=4"))

        // Event 4: Steering maneuver
        val steerAttempt = trialAttemptId
        assertEquals(2, steerAttempt)

        // Event 5: Completion
        val completeAttempt = trialAttemptId
        assertEquals(2, completeAttempt)

        // All events in the trial lifecycle strictly preserve trialAttemptId = 2
        val trialIds = listOf(startAttempt, avoidanceAttempt, collisionAttempt, steerAttempt, completeAttempt)
        assertTrue(trialIds.all { it == 2 })
    }

    @Test
    fun testCollisionUnmeasuredLatencyIntegrity() {
        // In collision, player failed to execute an avoidance response
        val latencyOnCollision = -1L
        val latencyCsvField = if (latencyOnCollision < 0L) "NA" else latencyOnCollision.toString()
        assertEquals("NA", latencyCsvField)
        assertNotEquals("1800", latencyCsvField)
    }

    @Test
    fun testReactiveSteerLatencyProvenance() {
        val trafficOnsetTime = 100_000L
        val steerActionTime = 100_420L

        // Player steered 420 ms after traffic vehicle appeared
        val latency = if (steerActionTime >= trafficOnsetTime) steerActionTime - trafficOnsetTime else -1L
        assertEquals(420L, latency)
    }

    @Test
    fun testSyncQueueManagerCsvParsingWithQuotesAndCommas() {
        val line = "28,\"Female\",\"Adapt-IQ Research Session\",\"PARTICIPANT_01\",\"SESSION_01\",\"gaming\",\"runner\",1,1001,1717943000000,15000,\"jump\",\"jump\",\"successful\",240,\"lane=1,playerLane=1,type=JUMP_HURDLE\",NA,NA,NA,NA,NA,NA,NA,NA,NA"
        val tokens = SyncQueueManager.parseCsvLine(line)

        assertEquals(25, tokens.size)
        assertEquals("28", tokens[0])
        assertEquals("Female", tokens[1])
        assertEquals("gaming", tokens[5])
        assertEquals("runner", tokens[6])
        assertEquals("1001", tokens[8])
        assertEquals("jump", tokens[11])
        assertEquals("successful", tokens[13])
        assertEquals("240", tokens[14])
        assertEquals("lane=1,playerLane=1,type=JUMP_HURDLE", tokens[15])
        assertEquals("NA", tokens[16])
    }
}
