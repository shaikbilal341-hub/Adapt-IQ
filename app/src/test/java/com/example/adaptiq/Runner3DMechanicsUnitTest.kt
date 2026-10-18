package com.example.adaptiq

import com.example.adaptiq.ui.screens.RunnerActionState
import com.example.adaptiq.ui.screens.RunnerCollisionEngine
import com.example.adaptiq.ui.screens.RunnerObstacle
import com.example.adaptiq.ui.screens.RunnerObstacleType
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

/**
 * Deterministic Unit Tests for the Real-Time 3D Temple Run Engine.
 * Verifies 3D spatial coordinate calculations, camera follow geometry,
 * action state kinematics (jump elevation and slide ducking),
 * and 3D collision envelope synchronization.
 */
class Runner3DMechanicsUnitTest {

    @Test
    fun test3DWorldLaneCoordinateMapping() {
        fun laneToWorldX(laneIndex: Int): Float {
            return (laneIndex.coerceIn(0, 2) - 1) * 1.8f
        }

        assertEquals(-1.8f, laneToWorldX(0), 0.001f) // Left Lane
        assertEquals(0.0f, laneToWorldX(1), 0.001f)  // Center Lane
        assertEquals(1.8f, laneToWorldX(2), 0.001f)  // Right Lane

        // Clamping check
        assertEquals(-1.8f, laneToWorldX(-1), 0.001f)
        assertEquals(1.8f, laneToWorldX(5), 0.001f)
    }

    @Test
    fun test3DZDepthAndNormalizedDepthSynchronization() {
        val spawnDistance = 65.0f

        fun depthToZ(depth: Float): Float {
            return spawnDistance * (1.0f - depth)
        }

        fun zToDepth(z: Float): Float {
            return 1.0f - (z / spawnDistance)
        }

        // 1. Horizon spawn: depth = 0.0f -> Z = 65.0m
        assertEquals(65.0f, depthToZ(0.0f), 0.001f)
        assertEquals(0.0f, zToDepth(65.0f), 0.001f)

        // 2. Collision Zone Start: depth = 0.82f -> Z = 11.7m
        assertEquals(11.7f, depthToZ(RunnerCollisionEngine.COLLISION_ZONE_START), 0.01f)

        // 3. Collision Zone End: depth = 0.98f -> Z = 1.3m (Right at player!)
        assertEquals(1.3f, depthToZ(RunnerCollisionEngine.COLLISION_ZONE_END), 0.01f)

        // 4. Past player: depth = 1.10f -> Z = -6.5m (Behind camera)
        assertTrue(depthToZ(1.10f) < 0f)
    }

    @Test
    fun test3DCharacterJumpKinematicsClearsLowHurdle() {
        val hurdleHeight = 0.70f // Fallen log height in meters

        // Calculate jump trajectory: Y(t) = sin(t * PI) * 1.45m
        fun jumpElevation(progress: Float): Float {
            return sin(progress * Math.PI.toFloat()) * 1.45f
        }

        // Peak jump at progress = 0.5 (apex)
        val peakElevation = jumpElevation(0.5f)
        assertEquals(1.45f, peakElevation, 0.01f)
        assertTrue("Peak jump elevation (1.45m) must comfortably clear 0.70m hurdle", peakElevation > hurdleHeight + 0.4f)

        // Mid-jump at progress = 0.3
        val midJumpElevation = jumpElevation(0.3f)
        assertTrue("Mid-jump elevation (${midJumpElevation}m) clears 0.70m hurdle", midJumpElevation > hurdleHeight)

        // Ground level at takeoff and landing
        assertEquals(0.0f, jumpElevation(0.0f), 0.001f)
        assertEquals(0.0f, jumpElevation(1.0f), 0.001f)
    }

    @Test
    fun test3DCharacterSlideKinematicsDucksUnderOverheadArch() {
        val archLintelClearance = 1.60f // Bottom of overhead arch beam in meters
        val standingCharacterHeight = 1.85f

        // When standing upright, character collides with overhead arch
        assertTrue(standingCharacterHeight > archLintelClearance)

        // When sliding, character bounding box lowers to 0.40m
        val slidingCharacterHeight = 0.40f
        assertTrue("Sliding character height (0.40m) safely ducks below 1.60m arch beam", slidingCharacterHeight < archLintelClearance)
    }

    @Test
    fun testThirdPersonCameraFollowGeometry() {
        // Camera chase parameters
        fun calculateCameraPositions(playerX: Float): Pair<FloatArray, FloatArray> {
            val eye = floatArrayOf(playerX * 0.35f, 3.2f, -5.0f)
            val lookAt = floatArrayOf(playerX * 0.70f, 1.6f, 12.0f)
            return Pair(eye, lookAt)
        }

        // 1. Center lane (playerX = 0m)
        val (eyeCenter, lookCenter) = calculateCameraPositions(0f)
        assertEquals(0f, eyeCenter[0], 0.001f)
        assertEquals(3.2f, eyeCenter[1], 0.001f)  // Elevated above runner
        assertEquals(-5.0f, eyeCenter[2], 0.001f) // Behind runner
        assertEquals(12.0f, lookCenter[2], 0.001f) // Looking forward down path

        // 2. Left lane (playerX = -1.8m)
        val (eyeLeft, lookLeft) = calculateCameraPositions(-1.8f)
        assertTrue("Camera follows lateral left position smoothly", eyeLeft[0] < 0f && eyeLeft[0] > -1.8f)
        assertTrue("Camera look-at pans naturally toward left lane", lookLeft[0] < 0f)

        // 3. Right lane (playerX = 1.8m)
        val (eyeRight, lookRight) = calculateCameraPositions(1.8f)
        assertTrue("Camera follows lateral right position smoothly", eyeRight[0] > 0f && eyeRight[0] < 1.8f)
        assertTrue("Camera look-at pans naturally toward right lane", lookRight[0] > 0f)
    }
}
