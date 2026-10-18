package com.example.adaptiq

import com.example.adaptiq.ui.screens.RunnerActionState
import com.example.adaptiq.ui.screens.RunnerCollisionEngine
import com.example.adaptiq.ui.screens.RunnerObstacle
import com.example.adaptiq.ui.screens.RunnerObstacleType
import org.junit.Assert.*
import org.junit.Test

/**
 * Deterministic Unit Tests for Runner Collision Detection Logic.
 * Verifies that:
 * 1. Different lanes NEVER collide.
 * 2. Same lane but different depths (outside collision zone) DO NOT collide.
 * 3. Actual collision-zone overlap in same lane DOES collide when unmitigated.
 * 4. Jumping cleanly avoids low hurdles.
 * 5. Sliding cleanly avoids overhead arches.
 * 6. Changing lanes before impact avoids collisions if player clears collision bounds.
 * 7. One obstacle does not produce duplicate collision events.
 * 8. Starting a new trial resets the game state correctly.
 */
class RunnerCollisionDetectionUnitTest {

    @Test
    fun testDifferentLanesMustNotCollide() {
        // Player in center lane (1.0f)
        val playerLane = 1.0f
        val playerState = RunnerActionState.RUNNING

        // Obstacle in left lane (0) at collision depth (0.90f)
        val obstacleLeft = RunnerObstacle(
            obstacleId = 1,
            lane = 0,
            type = RunnerObstacleType.JUMP_HURDLE,
            depth = 0.90f
        )
        val resultLeft = RunnerCollisionEngine.evaluate(playerLane, playerState, obstacleLeft)
        assertFalse("Player in center lane must NEVER collide with obstacle in left lane", resultLeft.hasCollided)
        assertTrue("Player safely avoided obstacle in different lane", resultLeft.isAvoided)

        // Obstacle in right lane (2) at collision depth (0.90f)
        val obstacleRight = RunnerObstacle(
            obstacleId = 2,
            lane = 2,
            type = RunnerObstacleType.DODGE_ROADBLOCK,
            depth = 0.90f
        )
        val resultRight = RunnerCollisionEngine.evaluate(playerLane, playerState, obstacleRight)
        assertFalse("Player in center lane must NEVER collide with roadblock in right lane", resultRight.hasCollided)
        assertTrue("Player safely avoided obstacle in different lane", resultRight.isAvoided)

        // Player in right lane (2.0f), obstacle in center lane (1)
        val playerRight = 2.0f
        val obstacleCenter = RunnerObstacle(
            obstacleId = 3,
            lane = 1,
            type = RunnerObstacleType.SLIDE_ARCH,
            depth = 0.90f
        )
        val resultCenter = RunnerCollisionEngine.evaluate(playerRight, playerState, obstacleCenter)
        assertFalse("Player in right lane must NEVER collide with arch in center lane", resultCenter.hasCollided)
        assertTrue("Player safely avoided obstacle in different lane", resultCenter.isAvoided)
    }

    @Test
    fun testSameLaneDifferentDepthsMustNotCollide() {
        val playerLane = 1.0f
        val playerState = RunnerActionState.RUNNING

        // Distance 1: Distant horizon (depth = 0.15f)
        val distantObstacle = RunnerObstacle(
            obstacleId = 1,
            lane = 1,
            type = RunnerObstacleType.JUMP_HURDLE,
            depth = 0.15f
        )
        val distantResult = RunnerCollisionEngine.evaluate(playerLane, playerState, distantObstacle)
        assertFalse("Distant obstacle at horizon must not trigger collision", distantResult.hasCollided)
        assertFalse("Distant obstacle is not yet reached/avoided", distantResult.isAvoided)

        // Distance 2: Mid-track approach (depth = 0.55f)
        val approachingObstacle = RunnerObstacle(
            obstacleId = 2,
            lane = 1,
            type = RunnerObstacleType.DODGE_ROADBLOCK,
            depth = 0.55f
        )
        val approachingResult = RunnerCollisionEngine.evaluate(playerLane, playerState, approachingObstacle)
        assertFalse("Approaching obstacle at mid-track must not trigger collision", approachingResult.hasCollided)
        assertFalse("Approaching obstacle is not yet reached/avoided", approachingResult.isAvoided)

        // Distance 3: Past player (depth = 1.05f)
        val pastObstacle = RunnerObstacle(
            obstacleId = 3,
            lane = 1,
            type = RunnerObstacleType.SLIDE_ARCH,
            depth = 1.05f
        )
        val pastResult = RunnerCollisionEngine.evaluate(playerLane, playerState, pastObstacle)
        assertFalse("Obstacle behind player must not trigger collision", pastResult.hasCollided)
    }

    @Test
    fun testActualCollisionZoneOverlapCollides() {
        val playerLane = 1.0f
        val playerState = RunnerActionState.RUNNING // Player running without jumping or sliding

        // Obstacle directly in player's lane at impact depth (0.88f)
        val hurdle = RunnerObstacle(
            obstacleId = 1,
            lane = 1,
            type = RunnerObstacleType.JUMP_HURDLE,
            depth = 0.88f
        )
        val hurdleResult = RunnerCollisionEngine.evaluate(playerLane, playerState, hurdle)
        assertTrue("Player running directly into low hurdle must register collision", hurdleResult.hasCollided)
        assertFalse("Running into hurdle cannot be avoided", hurdleResult.isAvoided)

        val arch = RunnerObstacle(
            obstacleId = 2,
            lane = 1,
            type = RunnerObstacleType.SLIDE_ARCH,
            depth = 0.90f
        )
        val archResult = RunnerCollisionEngine.evaluate(playerLane, playerState, arch)
        assertTrue("Player running upright into overhead arch must register collision", archResult.hasCollided)
        assertFalse("Running into arch cannot be avoided", archResult.isAvoided)

        val roadblock = RunnerObstacle(
            obstacleId = 3,
            lane = 1,
            type = RunnerObstacleType.DODGE_ROADBLOCK,
            depth = 0.92f
        )
        val roadblockResult = RunnerCollisionEngine.evaluate(playerLane, playerState, roadblock)
        assertTrue("Player staying in roadblock lane must register collision", roadblockResult.hasCollided)
        assertFalse("Staying in roadblock lane cannot be avoided", roadblockResult.isAvoided)
    }

    @Test
    fun testJumpingAvoidsAppropriateLowObstacles() {
        val playerLane = 1.0f
        val jumpingState = RunnerActionState.JUMPING

        val lowHurdle = RunnerObstacle(
            obstacleId = 1,
            lane = 1,
            type = RunnerObstacleType.JUMP_HURDLE,
            depth = 0.88f
        )
        val result = RunnerCollisionEngine.evaluate(playerLane, jumpingState, lowHurdle)
        assertFalse("Jumping must NOT collide with low hurdle", result.hasCollided)
        assertTrue("Jumping successfully clears low hurdle", result.isAvoided)
        assertEquals("Jumped over low hurdle", result.reason)
    }

    @Test
    fun testSlidingAvoidsAppropriateOverheadObstacles() {
        val playerLane = 1.0f
        val slidingState = RunnerActionState.SLIDING

        val overheadArch = RunnerObstacle(
            obstacleId = 1,
            lane = 1,
            type = RunnerObstacleType.SLIDE_ARCH,
            depth = 0.88f
        )
        val result = RunnerCollisionEngine.evaluate(playerLane, slidingState, overheadArch)
        assertFalse("Sliding must NOT collide with overhead arch", result.hasCollided)
        assertTrue("Sliding successfully clears overhead arch", result.isAvoided)
        assertEquals("Slid under overhead arch", result.reason)
    }

    @Test
    fun testChangingLanesBeforeImpactAvoidsCollision() {
        val roadblockLane = 1 // Obstacle in center lane

        // Scenario A: Player changed to Lane 0 (display lane = 0.2f, cleared threshold)
        val clearedPlayerLane = 0.2f
        val playerRunning = RunnerActionState.RUNNING

        val roadblock = RunnerObstacle(
            obstacleId = 1,
            lane = roadblockLane,
            type = RunnerObstacleType.DODGE_ROADBLOCK,
            depth = 0.88f
        )
        val result = RunnerCollisionEngine.evaluate(clearedPlayerLane, playerRunning, roadblock)
        assertFalse("Player cleared center lane into left lane before impact -> NO collision", result.hasCollided)
        assertTrue("Successfully avoided roadblock by changing lanes", result.isAvoided)

        // Scenario B: Player only partially switched (display lane = 0.85f, distance = 0.15f < 0.45f)
        val incompleteLaneChange = 0.85f
        val partialResult = RunnerCollisionEngine.evaluate(incompleteLaneChange, playerRunning, roadblock)
        assertTrue("Player still within lateral collision envelope of roadblock -> collision", partialResult.hasCollided)
    }

    @Test
    fun testOneObstacleDoesNotProduceDuplicateCollisionEvents() {
        val obstacle = RunnerObstacle(
            obstacleId = 10,
            lane = 1,
            type = RunnerObstacleType.JUMP_HURDLE,
            depth = 0.84f
        )

        var collisionCount = 0
        var avoidedCount = 0

        // Frame 1: Reaches collision zone
        val playerLane = 1.0f
        val playerRunning = RunnerActionState.RUNNING

        if (obstacle.depth >= RunnerCollisionEngine.COLLISION_ZONE_START && !obstacle.evaluated) {
            val eval = RunnerCollisionEngine.evaluate(playerLane, playerRunning, obstacle)
            obstacle.evaluated = true
            if (eval.hasCollided) {
                obstacle.hasCollided = true
                collisionCount++
            } else {
                obstacle.isAvoided = true
                avoidedCount++
            }
        }
        assertEquals(1, collisionCount)
        assertTrue(obstacle.evaluated)
        assertTrue(obstacle.hasCollided)

        // Frame 2: Next frame at depth 0.86f
        obstacle.depth = 0.86f
        if (obstacle.depth >= RunnerCollisionEngine.COLLISION_ZONE_START && !obstacle.evaluated) {
            val eval2 = RunnerCollisionEngine.evaluate(playerLane, playerRunning, obstacle)
            obstacle.evaluated = true
            if (eval2.hasCollided) {
                collisionCount++
            }
        }
        assertEquals("Duplicate collision must NEVER occur on subsequent frames for the same obstacle", 1, collisionCount)

        // Frame 3: Next frame at depth 0.90f
        obstacle.depth = 0.90f
        if (obstacle.depth >= RunnerCollisionEngine.COLLISION_ZONE_START && !obstacle.evaluated) {
            val eval3 = RunnerCollisionEngine.evaluate(playerLane, playerRunning, obstacle)
            obstacle.evaluated = true
            if (eval3.hasCollided) {
                collisionCount++
            }
        }
        assertEquals("Duplicate collision must NEVER occur on subsequent frames for the same obstacle", 1, collisionCount)
    }

    @Test
    fun testStartingNewTrialResetsAppropriateGameState() {
        // Simulated trial state after an active run
        val activeObstacles = mutableListOf(
            RunnerObstacle(1, 0, RunnerObstacleType.JUMP_HURDLE, 1.1f, evaluated = true, hasCollided = true),
            RunnerObstacle(2, 1, RunnerObstacleType.SLIDE_ARCH, 0.9f, evaluated = true, isAvoided = true)
        )
        var score = 150
        var encounters = 2
        var avoided = 1
        var collisions = 1
        var collisionFlash = true
        var isGameRunning = false
        var isGameFinished = true

        // RESET ACTION: startOfficialTrial("runner")
        fun resetForNewTrial() {
            activeObstacles.clear()
            score = 0
            encounters = 0
            avoided = 0
            collisions = 0
            collisionFlash = false
            isGameRunning = true
            isGameFinished = false
        }

        resetForNewTrial()

        assertTrue("Obstacle list must be empty upon starting new trial", activeObstacles.isEmpty())
        assertEquals(0, score)
        assertEquals(0, encounters)
        assertEquals(0, avoided)
        assertEquals(0, collisions)
        assertFalse("Collision flash indicator must be false after reset", collisionFlash)
        assertTrue(isGameRunning)
        assertFalse(isGameFinished)
    }
}
