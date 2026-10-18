package com.example.adaptiq.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

enum class GamingMode {
    SELECTION,
    RUNNER,
    DRIVING
}

data class GamingTrialResults(
    val gameMode: String,
    val durationSeconds: Float,
    val score: Int,
    val encountersTotal: Int,
    val avoidedTotal: Int,
    val collisionsTotal: Int,
    val actionsCount: Int,
    val averageLatencyMs: Long,
    val extraMetricLabel: String,
    val extraMetricValue: String,
    val completionStatus: String
)

@Composable
fun GamingActivityScreen(
    onLogEvent: (
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String,
        gameMode: String
    ) -> Unit,
    onGameModeSelected: (gameMode: String) -> Unit = {},
    onActivityCompleted: () -> Unit,
    onBack: () -> Unit
) {
    var gameMode by remember { mutableStateOf(GamingMode.SELECTION) }
    var isPractice by remember { mutableStateOf(true) }

    // Monotonic Clock Active Trial Timing
    var isGameRunning by remember { mutableStateOf(false) }
    var isGamePaused by remember { mutableStateOf(false) }
    var isGameFinished by remember { mutableStateOf(false) }

    var accumulatedActiveMs by remember { mutableLongStateOf(0L) }
    var activeStartRealtime by remember { mutableLongStateOf(0L) }
    var timeRemainingSeconds by remember { mutableIntStateOf(30) }

    var trialResults by remember { mutableStateOf<GamingTrialResults?>(null) }
    var currentAttemptId by remember { mutableIntStateOf(1) }

    // Exactly 30 Seconds Active Gameplay Loop using Android Monotonic Clock (SystemClock.elapsedRealtime)
    LaunchedEffect(isGameRunning, isGamePaused) {
        if (isGameRunning && !isGamePaused) {
            activeStartRealtime = SystemClock.elapsedRealtime()

            while (isGameRunning && !isGamePaused) {
                val now = SystemClock.elapsedRealtime()
                val currentSessionActiveMs = accumulatedActiveMs + (now - activeStartRealtime)
                val remainingMs = (30_000L - currentSessionActiveMs).coerceAtLeast(0L)
                timeRemainingSeconds = (remainingMs / 1000L).toInt()

                if (currentSessionActiveMs >= 30_000L) {
                    accumulatedActiveMs = 30_000L
                    timeRemainingSeconds = 0
                    isGameRunning = false
                    isGameFinished = true
                    break
                }
                delay(50L) // 20 Hz timer update precision
            }
        } else if (isGamePaused && activeStartRealtime > 0L) {
            accumulatedActiveMs += (SystemClock.elapsedRealtime() - activeStartRealtime)
            activeStartRealtime = 0L
        }
    }

    fun startOfficialTrial(modeStr: String) {
        isPractice = false
        accumulatedActiveMs = 0L
        activeStartRealtime = 0L
        timeRemainingSeconds = 30
        isGameRunning = true
        isGamePaused = false
        isGameFinished = false
        trialResults = null
        currentAttemptId++

        // Log official trial started event
        onLogEvent(
            "start",
            "start",
            "successful",
            currentAttemptId,
            0L,
            "mode=$modeStr,trial_type=official_30s",
            modeStr
        )
    }

    fun handlePauseResume(modeStr: String) {
        if (isGameRunning) {
            val newPaused = !isGamePaused
            isGamePaused = newPaused
            val currentActive = if (newPaused) {
                accumulatedActiveMs + (SystemClock.elapsedRealtime() - activeStartRealtime)
            } else {
                accumulatedActiveMs
            }
            if (!isPractice) {
                onLogEvent(
                    if (newPaused) "pause" else "resume",
                    if (newPaused) "pause" else "resume",
                    if (newPaused) "paused" else "resumed",
                    currentAttemptId,
                    currentActive,
                    "activeElapsedMs=$currentActive",
                    modeStr
                )
            }
        }
    }

    fun handleExitMiniGame(modeStr: String) {
        if (isGameRunning && !isPractice && !isGameFinished) {
            val elapsed = accumulatedActiveMs + if (activeStartRealtime > 0L) (SystemClock.elapsedRealtime() - activeStartRealtime) else 0L
            onLogEvent(
                "complete_trial",
                "user_interrupted",
                "interrupted",
                currentAttemptId,
                elapsed,
                "interrupted_at_ms=$elapsed",
                modeStr
            )
        }
        isGameRunning = false
        isGamePaused = false
        trialResults = null
        gameMode = GamingMode.SELECTION
    }

    // 1. RUNNER GAME: Dedicated Full-Screen Immersive Arcade View (Portrait)
    if (gameMode == GamingMode.RUNNER) {
        PortraitRunnerGameView(
            trialAttemptId = currentAttemptId,
            isPractice = isPractice,
            isGameRunning = isGameRunning,
            isGamePaused = isGamePaused,
            timeRemainingSeconds = timeRemainingSeconds,
            trialResults = trialResults,
            onStartPractice = {
                isGameRunning = true
                isGamePaused = false
            },
            onStartOfficialTrial = {
                startOfficialTrial("runner")
            },
            onPauseResume = {
                handlePauseResume("runner")
            },
            onLogEvent = { exp, det, res, att, dur, touch ->
                onLogEvent(exp, det, res, att, dur, touch, "runner")
            },
            onTrialFinished = { results ->
                trialResults = results
                if (!isPractice) {
                    onLogEvent(
                        "complete_trial",
                        "completed_30s",
                        "successful",
                        currentAttemptId,
                        30_000L,
                        "score=${results.score},avoided=${results.avoidedTotal},collisions=${results.collisionsTotal}",
                        "runner"
                    )
                }
            },
            onActivityCompleted = onActivityCompleted,
            onBack = {
                handleExitMiniGame("runner")
            }
        )
        return
    }

    // 2. DRIVING GAME: Dedicated Full-Screen Immersive Arcade View (Landscape)
    if (gameMode == GamingMode.DRIVING) {
        LandscapeDrivingGameView(
            trialAttemptId = currentAttemptId,
            isPractice = isPractice,
            isGameRunning = isGameRunning,
            isGamePaused = isGamePaused,
            timeRemainingSeconds = timeRemainingSeconds,
            trialResults = trialResults,
            onStartPractice = {
                isGameRunning = true
                isGamePaused = false
            },
            onStartOfficialTrial = {
                startOfficialTrial("driving")
            },
            onPauseResume = {
                handlePauseResume("driving")
            },
            onLogEvent = { exp, det, res, att, dur, touch ->
                onLogEvent(exp, det, res, att, dur, touch, "driving")
            },
            onTrialFinished = { results ->
                trialResults = results
                if (!isPractice) {
                    onLogEvent(
                        "complete_trial",
                        "completed_30s",
                        "successful",
                        currentAttemptId,
                        30_000L,
                        "score=${results.score},avoided=${results.avoidedTotal},collisions=${results.collisionsTotal},distance=${results.extraMetricValue}",
                        "driving"
                    )
                }
            },
            onActivityCompleted = onActivityCompleted,
            onBack = {
                handleExitMiniGame("driving")
            }
        )
        return
    }

    // 3. SELECTION MENU: Clean, Modern Research Protocol Menu
    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Gaming-style Interaction",
                subtitle = "30-second playable research tasks evaluating motor reflexes, obstacle avoidance, steering stability, and response latency",
                badgeText = "ACTIVITY 13 OF 13"
            )

            Text(
                text = "Select 30-Second Research Mini-Game",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AccentTeal
            )

            // Runner Mini-Game Card
            AdaptIQSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    gameMode = GamingMode.RUNNER
                    onGameModeSelected("runner")
                    accumulatedActiveMs = 0L
                    timeRemainingSeconds = 30
                    isGameRunning = false
                    isGamePaused = false
                    isGameFinished = false
                    isPractice = true
                    trialResults = null
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AccentTeal.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🏃", fontSize = 26.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Temple Explorer 3D",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = BadgeShape,
                                color = AccentTeal.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "3D RUNNER",
                                    color = AccentTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Temple Run–style 3D endless runner. Navigate 3 stone-paved lanes through ancient ruins. Swipe to leap fallen tree logs, slide under ruined stone arches, and dodge giant carved idol monoliths.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Text("➔", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            // Driving Mini-Game Card
            AdaptIQSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    gameMode = GamingMode.DRIVING
                    onGameModeSelected("driving")
                    accumulatedActiveMs = 0L
                    timeRemainingSeconds = 30
                    isGameRunning = false
                    isGamePaused = false
                    isGameFinished = false
                    isPractice = true
                    trialResults = null
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AccentBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🏎️", fontSize = 26.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Dr. Driving Research Lab",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = BadgeShape,
                                color = AccentBlue.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "LANDSCAPE",
                                    color = AccentBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Highway driving simulation in horizontal landscape orientation with oncoming traffic, lane changes, interactive steering wheel, and dual floor pedals.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Text("➔", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AdaptIQOutlinedButton(
                text = "Back to Dashboard",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                borderColor = BorderSubtle,
                textColor = TextSecondary
            )
        }
    }
}

/**
 * Runner Obstacle Model for Temple Run–Style 3D Experience
 */
enum class RunnerObstacleType {
    JUMP_HURDLE,      // Fallen Ancient Mossy Tree Trunk / Log -> Avoided by JUMP
    SLIDE_ARCH,       // Ancient Ruined Stone Archway / Beam -> Avoided by SLIDE
    DODGE_ROADBLOCK   // Giant Carved Totem Idol / Massive Boulder -> Avoided ONLY by changing lanes
}

data class RunnerObstacle(
    val obstacleId: Int,
    val lane: Int, // 0=Left, 1=Center, 2=Right
    val type: RunnerObstacleType,
    var depth: Float = 0.0f, // 0.0 (horizon) to 1.0 (player position)
    val onsetElapsedRealtime: Long = 0L,
    var evaluated: Boolean = false,
    var hasCollided: Boolean = false,
    var isAvoided: Boolean = false
)

enum class RunnerActionState {
    RUNNING,
    JUMPING,
    SLIDING
}

data class RunnerCollisionResult(
    val hasCollided: Boolean,
    val isAvoided: Boolean,
    val reason: String
)

/**
 * Deterministic Logical Collision Engine for Runner Gameplay.
 * Solves the lane-separation false injury bug:
 * 1. Checks depth collision window [0.82f, 0.98f].
 * 2. Checks lateral lane clearance (|displayLane - obstacleLane| < 0.45f).
 * 3. Never produces collisions for obstacles in different lanes!
 * 4. Checks action state against obstacle category (jump avoids low hurdle, slide avoids overhead arch).
 */
object RunnerCollisionEngine {
    const val COLLISION_ZONE_START = 0.82f
    const val COLLISION_ZONE_END = 0.98f
    const val LATERAL_LANE_THRESHOLD = 0.45f

    fun evaluate(
        playerDisplayLane: Float,
        playerState: RunnerActionState,
        obstacle: RunnerObstacle
    ): RunnerCollisionResult {
        // 1. Outside depth collision zone: no interaction yet or passed
        if (obstacle.depth < COLLISION_ZONE_START || obstacle.depth > COLLISION_ZONE_END) {
            return RunnerCollisionResult(
                hasCollided = false,
                isAvoided = false,
                reason = "Outside depth collision zone (depth=${obstacle.depth})"
            )
        }

        // 2. Lateral lane clearance: check if player is in the obstacle's lane
        val lateralDist = abs(playerDisplayLane - obstacle.lane.toFloat())
        val inSameLane = lateralDist < LATERAL_LANE_THRESHOLD

        if (!inSameLane) {
            // Player is in a different lane: SAFE PASS, ZERO COLLISION!
            return RunnerCollisionResult(
                hasCollided = false,
                isAvoided = true,
                reason = "Safe pass: player in lane $playerDisplayLane, obstacle in lane ${obstacle.lane}"
            )
        }

        // 3. Same lane & within collision depth zone: evaluate by obstacle type and action state
        return when (obstacle.type) {
            RunnerObstacleType.JUMP_HURDLE -> {
                if (playerState == RunnerActionState.JUMPING) {
                    RunnerCollisionResult(
                        hasCollided = false,
                        isAvoided = true,
                        reason = "Jumped over low hurdle"
                    )
                } else {
                    RunnerCollisionResult(
                        hasCollided = true,
                        isAvoided = false,
                        reason = "Hit low hurdle (action: $playerState)"
                    )
                }
            }
            RunnerObstacleType.SLIDE_ARCH -> {
                if (playerState == RunnerActionState.SLIDING) {
                    RunnerCollisionResult(
                        hasCollided = false,
                        isAvoided = true,
                        reason = "Slid under overhead arch"
                    )
                } else {
                    RunnerCollisionResult(
                        hasCollided = true,
                        isAvoided = false,
                        reason = "Hit overhead arch (action: $playerState)"
                    )
                }
            }
            RunnerObstacleType.DODGE_ROADBLOCK -> {
                // Giant totem / boulder blocks entire lane; neither jump nor slide clears it
                RunnerCollisionResult(
                    hasCollided = true,
                    isAvoided = false,
                    reason = "Crashed into lane-blocking boulder/totem"
                )
            }
        }
    }
}

/**
 * 1. PORTRAIT RUNNER GAME VIEW
 * Fully redesigned, immersive, full-screen arcade runner featuring:
 * - Full-screen viewport utilizing 100% of the display area (no outer scrollview padding or height clamping)
 * - 3D perspective cyber-track with neon laser guardrails, animated speed lines, and glowing lane dividers
 * - Fully animated Cyber Runner character with fluid running stride, athletic jump tuck, and crouching slide
 * - Clearly distinguished 3D obstacles (Laser Hurdles, Overhead Arch Gantries, and Hazard Roadblocks)
 * - Pure swipe gestures: Up (jump), Down (slide), Left/Right (lane change) with zero movement buttons
 * - Exact 30-second monotonic active research timing
 */
@Composable
private fun PortraitRunnerGameView(
    trialAttemptId: Int = 1,
    isPractice: Boolean,
    isGameRunning: Boolean,
    isGamePaused: Boolean = false,
    timeRemainingSeconds: Int,
    trialResults: GamingTrialResults?,
    onStartPractice: () -> Unit,
    onStartOfficialTrial: () -> Unit,
    onPauseResume: () -> Unit,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit,
    onTrialFinished: (GamingTrialResults) -> Unit,
    onActivityCompleted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    BackHandler {
        onBack()
    }

    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var playerDisplayLane by remember { mutableFloatStateOf(1f) } // Smooth lerp between lanes
    var isJumping by remember { mutableStateOf(false) }
    var jumpProgress by remember { mutableFloatStateOf(0f) }
    var isSliding by remember { mutableStateOf(false) }
    var slideProgress by remember { mutableFloatStateOf(0f) }

    var score by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Swipe on track: ◀ Left • Right ▶ • ▲ Jump • ▼ Slide") }
    var collisionFlash by remember { mutableStateOf(false) }

    var obstaclesEncountered by remember { mutableIntStateOf(0) }
    var obstaclesAvoided by remember { mutableIntStateOf(0) }
    var collisionsCount by remember { mutableIntStateOf(0) }
    var totalLatencySum by remember { mutableLongStateOf(0L) }
    var latencyEventsCount by remember { mutableIntStateOf(0) }
    var actionsCount by remember { mutableIntStateOf(0) }

    val obstacles = remember { mutableStateListOf<RunnerObstacle>() }
    var obstacleIdCounter by remember { mutableIntStateOf(1) }

    var pendingReactionTime by remember { mutableLongStateOf(0L) }
    var pendingDetectedAction by remember { mutableStateOf<String?>(null) }
    var roadScrollOffset by remember { mutableFloatStateOf(0f) }
    var runCycle by remember { mutableFloatStateOf(0f) }

    // Cleanly reset state when starting an official trial or new attempt
    LaunchedEffect(trialAttemptId) {
        obstacles.clear()
        collisionFlash = false
        score = 0
        obstaclesEncountered = 0
        obstaclesAvoided = 0
        collisionsCount = 0
        totalLatencySum = 0L
        latencyEventsCount = 0
        actionsCount = 0
        pendingReactionTime = 0L
        pendingDetectedAction = null
    }

    // Jump Animation Coroutine
    LaunchedEffect(isJumping) {
        if (isJumping) {
            val start = SystemClock.elapsedRealtime()
            val duration = 520L
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - start
                val p = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                jumpProgress = p
                if (p >= 1f) break
                delay(16L)
            }
            isJumping = false
            jumpProgress = 0f
        }
    }

    // Slide Animation Coroutine
    LaunchedEffect(isSliding) {
        if (isSliding) {
            val start = SystemClock.elapsedRealtime()
            val duration = 520L
            while (true) {
                val elapsed = SystemClock.elapsedRealtime() - start
                val p = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                slideProgress = p
                if (p >= 1f) break
                delay(16L)
            }
            isSliding = false
            slideProgress = 0f
        }
    }

    // Collision Flash Reset
    LaunchedEffect(collisionFlash) {
        if (collisionFlash) {
            delay(350L)
            collisionFlash = false
        }
    }

    // Runner 60fps Game Loop
    LaunchedEffect(isGameRunning, isGamePaused) {
        var lastSpawnTime = SystemClock.elapsedRealtime()

        while (isGameRunning && !isGamePaused) {
            val now = SystemClock.elapsedRealtime()

            // Smooth forward road scroll & character run stride
            roadScrollOffset = (roadScrollOffset + 0.038f) % 1f
            runCycle = (runCycle + 0.28f) % (2f * Math.PI.toFloat())

            // Smooth lane interpolation for athletic transition
            playerDisplayLane += (playerLane.toFloat() - playerDisplayLane) * 0.26f

            // Spawn obstacles every ~1.75 seconds
            if (now - lastSpawnTime > 1750L) {
                lastSpawnTime = now
                val spawnLane = Random.nextInt(0, 3)
                val type = when (Random.nextInt(0, 3)) {
                    0 -> RunnerObstacleType.JUMP_HURDLE
                    1 -> RunnerObstacleType.SLIDE_ARCH
                    else -> RunnerObstacleType.DODGE_ROADBLOCK
                }
                obstacles.add(
                    RunnerObstacle(
                        obstacleId = obstacleIdCounter++,
                        lane = spawnLane,
                        type = type,
                        depth = 0.0f,
                        onsetElapsedRealtime = now
                    )
                )
            }

            // Advance & evaluate obstacles
            val iterator = obstacles.listIterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.depth += 0.022f // Smooth forward motion

                // Collision evaluation zone: depth in [0.82, 0.98]
                if (obs.depth >= RunnerCollisionEngine.COLLISION_ZONE_START && !obs.evaluated) {
                    obs.evaluated = true
                    obstaclesEncountered++

                    val playerState = when {
                        isJumping -> RunnerActionState.JUMPING
                        isSliding -> RunnerActionState.SLIDING
                        else -> RunnerActionState.RUNNING
                    }

                    val eval = RunnerCollisionEngine.evaluate(playerDisplayLane, playerState, obs)
                    val expectedAction = when (obs.type) {
                        RunnerObstacleType.JUMP_HURDLE -> "jump"
                        RunnerObstacleType.SLIDE_ARCH -> "roll"
                        RunnerObstacleType.DODGE_ROADBLOCK -> "swipe_dodge"
                    }

                    val qualifyingResponseTime = pendingReactionTime
                    val latency = if (qualifyingResponseTime > 0L && qualifyingResponseTime >= obs.onsetElapsedRealtime) {
                        (qualifyingResponseTime - obs.onsetElapsedRealtime).coerceIn(40L, 1600L)
                    } else if (!eval.hasCollided) {
                        (now - obs.onsetElapsedRealtime).coerceIn(100L, 1600L)
                    } else {
                        -1L
                    }

                    if (latency > 0L) {
                        totalLatencySum += latency
                        latencyEventsCount++
                    }

                    val detectedStr = pendingDetectedAction ?: when {
                        isJumping -> "jump"
                        isSliding -> "roll"
                        eval.hasCollided -> "none"
                        else -> "swipe_dodge"
                    }

                    if (eval.hasCollided) {
                        obs.hasCollided = true
                        collisionsCount++
                        score = (score - 10).coerceAtLeast(0)
                        collisionFlash = true // RED FLASH ONLY ON GENUINE CONFIRMED COLLISION!
                        statusText = "💥 Collision: ${eval.reason}"

                        if (!isPractice) {
                            onLogEvent(
                                expectedAction,
                                detectedStr,
                                "collision",
                                trialAttemptId,
                                -1L,
                                "obstacleId=${obs.obstacleId},lane=${obs.lane},playerLane=$playerLane,type=${obs.type}"
                            )
                        }
                    } else {
                        obs.isAvoided = true
                        obstaclesAvoided++
                        score += 30
                        collisionFlash = false // NEVER SHOW FLASH ON AVOIDANCE OR DIFFERENT LANE!
                        statusText = "✓ Avoided: ${eval.reason}"

                        if (!isPractice) {
                            onLogEvent(
                                expectedAction,
                                detectedStr,
                                "successful",
                                trialAttemptId,
                                latency,
                                "obstacleId=${obs.obstacleId},lane=${obs.lane},playerLane=$playerLane,type=${obs.type}"
                            )
                        }
                    }

                    pendingReactionTime = 0L
                    pendingDetectedAction = null
                } else if (obs.depth > 1.20f) {
                    iterator.remove()
                }
            }

            delay(20L) // ~50 fps update rate
        }

        // Emit final results when 30s official trial concludes
        if (!isGameRunning && timeRemainingSeconds == 0 && obstaclesEncountered > 0 && trialResults == null) {
            val avgLat = if (latencyEventsCount > 0) totalLatencySum / latencyEventsCount else -1L
            onTrialFinished(
                GamingTrialResults(
                    gameMode = "Runner (3D Perspective)",
                    durationSeconds = 30.0f,
                    score = score,
                    encountersTotal = obstaclesEncountered,
                    avoidedTotal = obstaclesAvoided,
                    collisionsTotal = collisionsCount,
                    actionsCount = actionsCount,
                    averageLatencyMs = avgLat,
                    extraMetricLabel = "Accuracy",
                    extraMetricValue = "${if (obstaclesEncountered > 0) (obstaclesAvoided * 100 / obstaclesEncountered) else 0}%",
                    completionStatus = "COMPLETED"
                )
            )
        }
    }

    fun handleJump() {
        if (!isJumping && isGameRunning && !isGamePaused) {
            isJumping = true
            actionsCount++
            pendingReactionTime = SystemClock.elapsedRealtime()
            pendingDetectedAction = "jump"
            statusText = "▲ Leaping over hurdle!"
        }
    }

    fun handleSlide() {
        if (!isSliding && isGameRunning && !isGamePaused) {
            isSliding = true
            actionsCount++
            pendingReactionTime = SystemClock.elapsedRealtime()
            pendingDetectedAction = "roll"
            statusText = "▼ Sliding under arch!"
        }
    }

    fun handleMoveLeft() {
        if (isGameRunning && !isGamePaused) {
            actionsCount++
            pendingReactionTime = SystemClock.elapsedRealtime()
            pendingDetectedAction = "swipe_left"
            if (playerLane > 0) {
                playerLane--
                statusText = "◀ Switched to Lane ${playerLane + 1}"
            } else {
                statusText = "◀ Already at Leftmost Lane"
            }
        }
    }

    fun handleMoveRight() {
        if (isGameRunning && !isGamePaused) {
            actionsCount++
            pendingReactionTime = SystemClock.elapsedRealtime()
            pendingDetectedAction = "swipe_right"
            if (playerLane < 2) {
                playerLane++
                statusText = "Switched to Lane ${playerLane + 1} ▶"
            } else {
                statusText = "Already at Rightmost Lane ▶"
            }
        }
    }

    // FULL SCREEN VIEWPORT (No artificial padding or height clamping!)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B18))
            .pointerInput(isGameRunning, isGamePaused) {
                if (isGameRunning && !isGamePaused) {
                    var dragX = 0f
                    var dragY = 0f
                    var gestureHandled = false
                    val swipeThreshold = 40f

                    detectDragGestures(
                        onDragStart = {
                            dragX = 0f
                            dragY = 0f
                            gestureHandled = false
                        },
                        onDragEnd = { gestureHandled = false },
                        onDragCancel = { gestureHandled = false },
                        onDrag = { _, dragAmount ->
                            if (!gestureHandled) {
                                dragX += dragAmount.x
                                dragY += dragAmount.y
                                val absX = abs(dragX)
                                val absY = abs(dragY)

                                if (absX >= swipeThreshold || absY >= swipeThreshold) {
                                    // Dominant axis checking (>1.3x) to reject ambiguous diagonals
                                    if (absX > absY * 1.3f) {
                                        gestureHandled = true
                                        if (dragX > 0f) handleMoveRight() else handleMoveLeft()
                                    } else if (absY > absX * 1.3f) {
                                        gestureHandled = true
                                        if (dragY < 0f) handleJump() else handleSlide()
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val horizonY = height * 0.28f
            val bottomY = height

            // 1. SKY GRADIENT (Ancient Tropical Dusk - Amber Gold to Deep Jungle)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFFB45309)),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset(0f, 0f),
                size = Size(width, horizonY)
            )

            // Distant Ancient Stepped Mayan/Temple Pyramids
            val pyramidBaseY = horizonY
            // Left Pyramid
            val p1X = width * 0.22f
            for (step in 0..4) {
                val stepW = 90f - step * 16f
                val stepH = 9f
                val stepY = pyramidBaseY - (step + 1) * stepH
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(p1X - stepW / 2f, stepY),
                    size = Size(stepW, stepH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            // Center-Right Majestic Pyramid
            val p2X = width * 0.70f
            for (step in 0..6) {
                val stepW = 140f - step * 18f
                val stepH = 10f
                val stepY = pyramidBaseY - (step + 1) * stepH
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(p2X - stepW / 2f, stepY),
                    size = Size(stepW, stepH),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
            // Temple Capstone Sun Glow
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = 0.45f),
                radius = 12f,
                center = Offset(p2X, pyramidBaseY - 70f)
            )

            // 2. FLANKING JUNGLE CANOPY & VEGETATION GROUND
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF022C22)),
                    startY = horizonY,
                    endY = bottomY
                ),
                topLeft = Offset(0f, horizonY),
                size = Size(width, bottomY - horizonY)
            )

            // Weathered Ancient Stone Steles / Pillars Flanking Track Edges
            for (pIdx in 0..5) {
                val depth = (pIdx * 0.18f + roadScrollOffset * 0.18f) % 1.0f
                val py = horizonY + (bottomY - horizonY) * depth
                val pScale = 0.25f + depth * 0.75f
                val pillarW = 20f * pScale
                val pillarH = 75f * pScale

                // Left flanking totem
                val lx = 20f + (width * 0.10f) * depth
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(lx - pillarW / 2f, py - pillarH),
                    size = Size(pillarW, pillarH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Vines / Moss
                drawCircle(color = Color(0xFF22C55E), radius = 4f * pScale, center = Offset(lx, py - pillarH * 0.6f))

                // Right flanking totem
                val rx = width - 20f - (width * 0.10f) * depth
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(rx - pillarW / 2f, py - pillarH),
                    size = Size(pillarW, pillarH),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawCircle(color = Color(0xFF22C55E), radius = 4f * pScale, center = Offset(rx, py - pillarH * 0.6f))
            }

            // 3. ANCIENT STONE-PAVED RUNNING TRACK (Expansive 3D Perspective)
            val bottomRoadWidth = width * 0.92f
            val horizonRoadWidth = width * 0.24f

            val leftBottomX = (width - bottomRoadWidth) / 2f
            val rightBottomX = leftBottomX + bottomRoadWidth
            val leftHorizonX = (width - horizonRoadWidth) / 2f
            val rightHorizonX = leftHorizonX + horizonRoadWidth

            // Main Stone Flagstone Surface
            val trackPath = Path().apply {
                moveTo(leftHorizonX, horizonY)
                lineTo(rightHorizonX, horizonY)
                lineTo(rightBottomX, bottomY)
                lineTo(leftBottomX, bottomY)
                close()
            }
            drawPath(
                path = trackPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF262626), Color(0xFF1E242B), Color(0xFF171B21)),
                    startY = horizonY,
                    endY = bottomY
                )
            )

            // Ancient Mossy Curbstones Along Track Edges
            val curbSegments = 10
            for (c in 0 until curbSegments) {
                val d1 = (c.toFloat() / curbSegments + roadScrollOffset * (1f / curbSegments)) % 1.0f
                val d2 = (d1 + 0.8f / curbSegments).coerceAtMost(1.0f)
                if (d2 > d1) {
                    val y1 = horizonY + (bottomY - horizonY) * d1
                    val y2 = horizonY + (bottomY - horizonY) * d2
                    val cw1 = 8f + d1 * 18f
                    val cw2 = 8f + d2 * 18f

                    val lx1 = leftHorizonX + (leftBottomX - leftHorizonX) * d1
                    val lx2 = leftHorizonX + (leftBottomX - leftHorizonX) * d2
                    val rx1 = rightHorizonX + (rightBottomX - rightHorizonX) * d1
                    val rx2 = rightHorizonX + (rightBottomX - rightHorizonX) * d2

                    val mossColor = if (c % 2 == 0) Color(0xFF166534) else Color(0xFF334155)

                    // Left curb block
                    val lCurb = Path().apply {
                        moveTo(lx1 - cw1, y1)
                        lineTo(lx1, y1)
                        lineTo(lx2, y2)
                        lineTo(lx2 - cw2, y2)
                        close()
                    }
                    drawPath(lCurb, mossColor)

                    // Right curb block
                    val rCurb = Path().apply {
                        moveTo(rx1, y1)
                        lineTo(rx1 + cw1, y1)
                        lineTo(rx2 + cw2, y2)
                        lineTo(rx2, y2)
                        close()
                    }
                    drawPath(rCurb, mossColor)
                }
            }

            // Horizontal Flagstone Grooves (Forward Scrolling Ancient Pavers)
            for (i in 0..8) {
                val lineDepth = (i * 0.12f + roadScrollOffset * 0.12f) % 1.0f
                val lineY = horizonY + (bottomY - horizonY) * lineDepth
                val currentW = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * lineDepth
                val leftX = (width - currentW) / 2f

                drawLine(
                    color = Color(0xFF0F172A).copy(alpha = 0.70f),
                    start = Offset(leftX, lineY),
                    end = Offset(leftX + currentW, lineY),
                    strokeWidth = 2f + lineDepth * 4.5f
                )
            }

            // 4. THREE PLAYABLE LANES (Separated by Carved Stone Grooves & Ancient Golden Runic Lines)
            for (laneDiv in 1..2) {
                val topX = leftHorizonX + (horizonRoadWidth / 3f) * laneDiv
                val botX = leftBottomX + (bottomRoadWidth / 3f) * laneDiv

                // Continuous Ancient Carved Stone Groove
                drawLine(
                    color = Color(0xFF0A0F1D),
                    start = Offset(topX, horizonY),
                    end = Offset(botX, bottomY),
                    strokeWidth = 4f
                )

                // Glowing Golden Amber Rune Markers
                for (dashIdx in 0..6) {
                    val depth = (dashIdx * 0.15f + roadScrollOffset * 0.15f) % 1.0f
                    val dashY = horizonY + (bottomY - horizonY) * depth
                    val dashX = topX + (botX - topX) * depth
                    val dashH = 14f + depth * 35f
                    val dashW = 3f + depth * 5.5f

                    drawRoundRect(
                        color = Color(0xFFF59E0B).copy(alpha = 0.5f + depth * 0.45f),
                        topLeft = Offset(dashX - dashW / 2f, dashY),
                        size = Size(dashW, dashH),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            // 5. SUBSTANTIAL, RECOGNIZABLE TEMPLE RUN OBSTACLES (3D Perspective Scaling)
            for (obs in obstacles) {
                val obsY = horizonY + (bottomY - horizonY) * obs.depth
                val currentRoadW = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * obs.depth
                val currentLeftX = (width - currentRoadW) / 2f
                val laneW = currentRoadW / 3f
                val obsCenterX = currentLeftX + laneW * obs.lane + (laneW / 2f)

                val obsBaseW = laneW * 0.88f
                val obsScale = 0.22f + obs.depth * 0.78f // Smooth 3D growth

                when (obs.type) {
                    RunnerObstacleType.JUMP_HURDLE -> {
                        // ==========================================
                        // 1. FALLEN MOSSY ANCIENT TREE TRUNK / LOG
                        // Clear low hurdle: Jump over!
                        // ==========================================
                        val logH = 34f * obsScale
                        val logW = obsBaseW

                        // Drop Shadow on Flagstones
                        drawOval(
                            color = Color.Black.copy(alpha = 0.55f),
                            topLeft = Offset(obsCenterX - logW * 0.52f, obsY - logH * 0.2f),
                            size = Size(logW * 1.04f, 16f * obsScale)
                        )

                        // Main Cylindrical Tree Trunk (Deep bark brown)
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF5C4033), Color(0xFF382315), Color(0xFF26180E))
                            ),
                            topLeft = Offset(obsCenterX - logW / 2f, obsY - logH),
                            size = Size(logW, logH),
                            cornerRadius = CornerRadius(8f * obsScale, 8f * obsScale)
                        )

                        // Left and Right Cut Ends (Concentric Growth Rings)
                        val endW = 12f * obsScale
                        drawOval(
                            color = Color(0xFF8D6E63),
                            topLeft = Offset(obsCenterX - logW / 2f, obsY - logH),
                            size = Size(endW, logH)
                        )
                        drawOval(
                            color = Color(0xFF5D4037),
                            topLeft = Offset(obsCenterX - logW / 2f + 2f, obsY - logH + 4f),
                            size = Size(endW * 0.65f, logH - 8f)
                        )
                        drawOval(
                            color = Color(0xFF8D6E63),
                            topLeft = Offset(obsCenterX + logW / 2f - endW, obsY - logH),
                            size = Size(endW, logH)
                        )

                        // Lush Green Moss Patches & Ferns on Top
                        val mossCount = 4
                        for (m in 0 until mossCount) {
                            val mx = obsCenterX - logW * 0.35f + m * (logW * 0.22f)
                            drawOval(
                                color = Color(0xFF22C55E),
                                topLeft = Offset(mx, obsY - logH - 4f * obsScale),
                                size = Size(20f * obsScale, 8f * obsScale)
                            )
                        }
                    }

                    RunnerObstacleType.SLIDE_ARCH -> {
                        // ==========================================
                        // 2. ANCIENT RUINED STONE ARCHWAY / BEAM
                        // Overhead obstacle: Slide under!
                        // ==========================================
                        val archTotalH = 125f * obsScale
                        val pillarW = 20f * obsScale
                        val beamH = 36f * obsScale
                        val archW = obsBaseW

                        // Pillar Ground Shadows
                        drawOval(color = Color.Black.copy(alpha = 0.50f), topLeft = Offset(obsCenterX - archW / 2f - 4f, obsY - 6f), size = Size(pillarW + 8f, 14f * obsScale))
                        drawOval(color = Color.Black.copy(alpha = 0.50f), topLeft = Offset(obsCenterX + archW / 2f - pillarW - 4f, obsY - 6f), size = Size(pillarW + 8f, 14f * obsScale))

                        // Left Carved Stone Pillar
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF475569), Color(0xFF1E293B))),
                            topLeft = Offset(obsCenterX - archW / 2f, obsY - archTotalH),
                            size = Size(pillarW, archTotalH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                        // Right Carved Stone Pillar
                        drawRoundRect(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF475569))),
                            topLeft = Offset(obsCenterX + archW / 2f - pillarW, obsY - archTotalH),
                            size = Size(pillarW, archTotalH),
                            cornerRadius = CornerRadius(4f, 4f)
                        )

                        // Heavy Stone Overhead Lintel Beam
                        drawRoundRect(
                            brush = Brush.verticalGradient(listOf(Color(0xFF64748B), Color(0xFF334155))),
                            topLeft = Offset(obsCenterX - archW / 2f - 4f, obsY - archTotalH),
                            size = Size(archW + 8f, beamH),
                            cornerRadius = CornerRadius(6f * obsScale, 6f * obsScale)
                        )

                        // Inscribed Glowing Mayan Glyphs along Beam
                        for (g in 0..3) {
                            val gx = obsCenterX - archW * 0.30f + g * (archW * 0.20f)
                            drawRoundRect(
                                color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                                topLeft = Offset(gx, obsY - archTotalH + beamH * 0.30f),
                                size = Size(14f * obsScale, 14f * obsScale),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }

                        // Hanging Jungle Vines and Roots
                        for (v in 0..4) {
                            val vx = obsCenterX - archW * 0.36f + v * (archW * 0.18f)
                            val vineLen = (18f + ((v * 7) % 24)) * obsScale
                            drawLine(
                                color = Color(0xFF16A34A),
                                start = Offset(vx, obsY - archTotalH + beamH),
                                end = Offset(vx + (if (v % 2 == 0) 3f else -3f), obsY - archTotalH + beamH + vineLen),
                                strokeWidth = 3f * obsScale,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    RunnerObstacleType.DODGE_ROADBLOCK -> {
                        // ==========================================
                        // 3. GIANT CARVED ANCIENT IDOL TOTEM / BOULDER
                        // Complete Lane Blocker: Swipe Left/Right to dodge!
                        // ==========================================
                        val totemH = 130f * obsScale
                        val totemW = obsBaseW

                        // Massive Ground Shadow
                        drawOval(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(obsCenterX - totemW * 0.55f, obsY - 10f),
                            size = Size(totemW * 1.10f, 20f * obsScale)
                        )

                        // Weathered Monolithic Stone Body
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
                            ),
                            topLeft = Offset(obsCenterX - totemW / 2f, obsY - totemH),
                            size = Size(totemW, totemH),
                            cornerRadius = CornerRadius(10f * obsScale, 10f * obsScale)
                        )

                        // Ancient Carved Menacing Idol Face (Tiki / Mayan relief)
                        val faceCenterY = obsY - totemH * 0.62f
                        val eyeW = 16f * obsScale
                        val eyeH = 10f * obsScale

                        // Glowing Menacing Ruby Eyes
                        drawOval(color = Color(0xFFDC2626), topLeft = Offset(obsCenterX - totemW * 0.28f - eyeW / 2f, faceCenterY - eyeH / 2f), size = Size(eyeW, eyeH))
                        drawCircle(color = Color.White, radius = 2.5f * obsScale, center = Offset(obsCenterX - totemW * 0.28f, faceCenterY))

                        drawOval(color = Color(0xFFDC2626), topLeft = Offset(obsCenterX + totemW * 0.28f - eyeW / 2f, faceCenterY - eyeH / 2f), size = Size(eyeW, eyeH))
                        drawCircle(color = Color.White, radius = 2.5f * obsScale, center = Offset(obsCenterX + totemW * 0.28f, faceCenterY))

                        // Carved Stone Mouth with Fangs
                        val mouthW = totemW * 0.55f
                        val mouthH = 16f * obsScale
                        val mouthY = faceCenterY + 22f * obsScale
                        drawRoundRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(obsCenterX - mouthW / 2f, mouthY),
                            size = Size(mouthW, mouthH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        // Stone Teeth Fangs
                        for (t in 0..3) {
                            val tx = obsCenterX - mouthW * 0.35f + t * (mouthW * 0.24f)
                            drawRect(
                                color = Color(0xFFE2E8F0),
                                topLeft = Offset(tx, mouthY),
                                size = Size(5f * obsScale, 7f * obsScale)
                            )
                        }

                        // Creeping Moss & Deep Stone Fissures
                        drawCircle(color = Color(0xFF16A34A), radius = 12f * obsScale, center = Offset(obsCenterX - totemW * 0.36f, obsY - totemH * 0.20f))
                        drawCircle(color = Color(0xFF16A34A), radius = 14f * obsScale, center = Offset(obsCenterX + totemW * 0.34f, obsY - totemH * 0.88f))
                    }
                }
            }

            // 6. PROMINENT ARTICULATED ADVENTURER RUNNER (Third-Person View from Behind)
            val playerLaneW = bottomRoadWidth / 3f
            val playerCenterX = leftBottomX + playerLaneW * playerDisplayLane + (playerLaneW / 2f)
            val playerBaseY = bottomY - 110f

            drawRunnerCharacter(
                scope = this,
                centerX = playerCenterX,
                baseY = playerBaseY,
                isJumping = isJumping,
                jumpProgress = jumpProgress,
                isSliding = isSliding,
                slideProgress = slideProgress,
                runCycle = runCycle,
                lateralLean = (playerDisplayLane - playerLane),
                collisionFlash = collisionFlash
            )

            // 7. COLLISION IMPACT VIGNETTE (Only triggers after genuine confirmed collision!)
            if (collisionFlash) {
                drawRect(
                    color = Color(0xFFDC2626).copy(alpha = 0.50f),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, height)
                )
            }
        }

        // TOP FLOATING ARCADE HUD
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit Button
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = BackgroundDark.copy(alpha = 0.75f)),
                    border = BorderStroke(1.dp, BorderSubtle),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("✕ Exit", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Practice / Official Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPractice) Color(0xFFCA8A04) else Color(0xFF0D9488)
                ) {
                    Text(
                        text = if (isPractice) "PRACTICE RUN" else "OFFICIAL 30s TRIAL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Monotonic Countdown Timer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevatedVariant,
                    border = BorderStroke(1.dp, if (isPractice) StatusWarning else AccentTeal)
                ) {
                    Text(
                        text = if (isPractice && !isGameRunning) "PRACTICE" else "⏱️ 00:%02d".format(timeRemainingSeconds),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPractice) StatusWarning else AccentTeal
                    )
                }

                // Pause / Resume Button
                if (isGameRunning) {
                    Button(
                        onClick = onPauseResume,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isGamePaused) Color(0xFF16A34A) else Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (isGamePaused) "▶ Resume" else "⏸ Pause", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Real-Time Score & Avoidance HUD Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BackgroundDark.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Score: $score pts", color = StatusSuccess, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Avoided: $obstaclesAvoided", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Hits: $collisionsCount", color = if (collisionsCount > 0) StatusError else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Floating Dynamic Gesture Status Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // FLOATING PRACTICE LAUNCH OVERLAY
        if (isPractice && !isGameRunning && trialResults == null) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = AccentTeal
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🏃 Cyber Runner 3D",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Swipe ▲ Up to jump over laser hurdles.\n• Swipe ▼ Down to slide under overhead arches.\n• Swipe ◀ Left and Right ▶ to change lanes.\n• Avoid all obstacles to maintain maximum response score.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdaptIQOutlinedButton(
                            text = "Practice Run",
                            onClick = onStartPractice,
                            modifier = Modifier.weight(1f),
                            borderColor = BorderSubtle,
                            textColor = TextPrimary
                        )

                        AdaptIQPrimaryButton(
                            text = "Start Official 30s",
                            onClick = onStartOfficialTrial,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // FROSTED PAUSE OVERLAY
        if (isGamePaused) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = BorderSubtle
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⏸️ TRIAL PAUSED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "The 30-second active research timer is frozen.\nTap 'Resume' to continue the trial.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    AdaptIQPrimaryButton(
                        text = "Resume Gameplay",
                        onClick = onPauseResume,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // TRIAL COMPLETED SCORED RESULTS DIALOG
        if (trialResults != null) {
            val res = trialResults
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = StatusSuccess
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "✓ 30-Second Trial Completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "Status: ${res.completionStatus} • Active Duration: 30.00s",
                        fontWeight = FontWeight.SemiBold,
                        color = AccentTeal,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Game Score:", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${res.score} pts", fontWeight = FontWeight.Bold, color = StatusSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Obstacles Avoided / Total:", color = TextSecondary)
                        Text("${res.avoidedTotal} / ${res.encountersTotal}", color = AccentBlue, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Collisions Recorded:", color = TextSecondary)
                        Text("${res.collisionsTotal}", color = if (res.collisionsTotal > 0) StatusError else StatusSuccess, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Average Response Latency:", color = TextSecondary)
                        Text(if (res.averageLatencyMs > 0L) "${res.averageLatencyMs} ms" else "NA", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Motor Reflex Accuracy:", color = TextSecondary)
                        Text(res.extraMetricValue, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AdaptIQPrimaryButton(
                        text = "Complete Activity & Return to Dashboard",
                        onClick = onActivityCompleted,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdaptIQOutlinedButton(
                        text = "Retry 30-Second Scored Trial",
                        onClick = onStartOfficialTrial,
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderSubtle,
                        textColor = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Procedural Articulated 3D Explorer Adventurer Runner Character Drawing
 * Authentic Temple Run–inspired third-person adventurer viewed from behind:
 * - Substantial size (185px height x 92px width) anchored in the foreground
 * - Weathered leather expedition vest, khaki field shirt, and utility harness with brass buckles
 * - Mounted canvas expedition backpack with top-lashed bedroll and utility canteen
 * - Articulated muscular legs in olive safari cargo pants with reinforced knee pads
 * - Rugged leather trekking boots with rubber tread soles
 * - Dynamic ground shadow decoupled during leaps to provide authentic 3D depth
 * - Athletic sliding pose tucked low to the pavement with friction dust
 * - Lateral banking tilt during lane transitions
 */
private fun drawRunnerCharacter(
    scope: DrawScope,
    centerX: Float,
    baseY: Float,
    isJumping: Boolean,
    jumpProgress: Float,
    isSliding: Boolean,
    slideProgress: Float,
    runCycle: Float,
    lateralLean: Float = 0f,
    collisionFlash: Boolean
) {
    with(scope) {
        val characterHeight = 185f
        val characterWidth = 92f

        // Stride dynamics
        val jumpHeight = if (isJumping) sin(jumpProgress * Math.PI.toFloat()) * 135f else 0f
        val verticalBob = if (!isJumping && !isSliding) abs(sin(runCycle)) * 8f else 0f
        val currentY = baseY - jumpHeight - verticalBob

        // 1. Dynamic Ground Shadow (Remains anchored on the stone path track!)
        val shadowScale = if (isJumping) {
            (1f - (jumpHeight / 160f) * 0.55f).coerceAtLeast(0.35f)
        } else if (isSliding) {
            1.40f
        } else {
            1.05f
        }
        val shadowAlpha = if (isJumping) 0.28f else 0.58f

        drawOval(
            color = Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(centerX - (characterWidth * 0.52f) * shadowScale, baseY + 10f),
            size = Size((characterWidth * 1.04f) * shadowScale, 20f * shadowScale)
        )

        // Palette definitions
        val vestColor = if (collisionFlash) Color(0xFFDC2626) else Color(0xFF78350F)
        val vestDarkColor = if (collisionFlash) Color(0xFF991B1B) else Color(0xFF451A03)
        val shirtColor = if (collisionFlash) Color(0xFFFCA5A5) else Color(0xFFD4A373)
        val pantsColor = if (collisionFlash) Color(0xFFB91C1C) else Color(0xFF3F4D3C)
        val bootsColor = if (collisionFlash) Color(0xFF7F1D1D) else Color(0xFF29180E)
        val skinColor = if (collisionFlash) Color(0xFFF87171) else Color(0xFFD97706)
        val backpackColor = if (collisionFlash) Color(0xFF991B1B) else Color(0xFF382315)
        val bedrollColor = if (collisionFlash) Color(0xFFEF4444) else Color(0xFF1E3A1E)
        val bandanaColor = if (collisionFlash) Color.White else Color(0xFFB91C1C)

        // Banking tilt during lateral lane transition
        val bankingAngle = (lateralLean.coerceIn(-1f, 1f)) * -7.5f

        rotate(degrees = bankingAngle, pivot = Offset(centerX, baseY)) {
            if (isSliding) {
                // ==========================================
                // ATHLETIC LOW GROUND SLIDE POSE
                // ==========================================
                val slideW = characterWidth * 1.50f
                val slideH = 44f
                val slideY = baseY - slideH + 12f

                // Flagstone friction dust & sparks
                for (sp in 0..5) {
                    val spX = centerX - slideW / 2f - sp * 16f
                    val spY = baseY + 8f + ((sp * 7) % 6)
                    drawCircle(color = Color(0xFFFBBF24), radius = 3.5f, center = Offset(spX, spY))
                    drawCircle(color = Color(0xFFE2E8F0).copy(alpha = 0.45f), radius = 6f, center = Offset(spX - 8f, spY - 2f))
                }

                // Extended Sliding Legs (Pants & Boots)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(bootsColor, pantsColor)),
                    topLeft = Offset(centerX - slideW / 2f, slideY + 12f),
                    size = Size(slideW * 0.65f, slideH * 0.65f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Boot Sole
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(centerX - slideW / 2f, slideY + 22f),
                    size = Size(28f, 12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Torso & Weathered Vest Tucked Low
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(vestDarkColor, vestColor)),
                    topLeft = Offset(centerX - slideW * 0.12f, slideY),
                    size = Size(slideW * 0.55f, slideH),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Expedition Backpack nestled on back
                drawRoundRect(
                    color = backpackColor,
                    topLeft = Offset(centerX + slideW * 0.05f, slideY - 10f),
                    size = Size(36f, 30f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                // Bedroll on pack
                drawRoundRect(
                    color = bedrollColor,
                    topLeft = Offset(centerX + slideW * 0.08f, slideY - 18f),
                    size = Size(30f, 12f),
                    cornerRadius = CornerRadius(5f, 5f)
                )

                // Explorer Head & Crimson Bandana Tucked Low
                val headR = 19f
                val headX = centerX + slideW * 0.36f
                val headY = slideY + 8f
                drawCircle(color = skinColor, radius = headR, center = Offset(headX, headY))
                drawRoundRect(
                    color = bandanaColor,
                    topLeft = Offset(headX - headR * 0.9f, headY - headR * 0.8f),
                    size = Size(headR * 1.8f, headR * 0.85f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            } else {
                // ==========================================
                // STANDING / RUNNING / JUMPING FULL POSE
                // ==========================================
                val legSwing = if (isJumping) 0f else sin(runCycle) * 32f
                val armSwing = if (isJumping) 0f else -sin(runCycle) * 36f

                val hipY = currentY - characterHeight * 0.44f
                val footY = currentY

                if (isJumping) {
                    // Tucked knees during dramatic athletic leap
                    val tuckY = currentY - characterHeight * 0.20f
                    val kneeL = centerX - 24f
                    val kneeR = centerX + 24f

                    // Left Leg (Tucked)
                    drawLine(color = pantsColor, start = Offset(centerX - 16f, hipY), end = Offset(kneeL, tuckY), strokeWidth = 22f, cap = StrokeCap.Round)
                    drawLine(color = pantsColor, start = Offset(kneeL, tuckY), end = Offset(centerX - 14f, tuckY + 12f), strokeWidth = 20f, cap = StrokeCap.Round)
                    drawRoundRect(color = bootsColor, topLeft = Offset(centerX - 24f, tuckY + 8f), size = Size(24f, 16f), cornerRadius = CornerRadius(5f, 5f))

                    // Right Leg (Tucked)
                    drawLine(color = pantsColor, start = Offset(centerX + 16f, hipY), end = Offset(kneeR, tuckY), strokeWidth = 22f, cap = StrokeCap.Round)
                    drawLine(color = pantsColor, start = Offset(kneeR, tuckY), end = Offset(centerX + 14f, tuckY + 12f), strokeWidth = 20f, cap = StrokeCap.Round)
                    drawRoundRect(color = bootsColor, topLeft = Offset(centerX, tuckY + 8f), size = Size(24f, 16f), cornerRadius = CornerRadius(5f, 5f))
                } else {
                    // Running Stride Legs
                    val leftFootX = centerX - 20f - (legSwing * 0.50f)
                    val rightFootX = centerX + 20f + (legSwing * 0.50f)

                    // Left Leg
                    drawLine(
                        color = pantsColor,
                        start = Offset(centerX - 16f, hipY),
                        end = Offset(leftFootX, footY),
                        strokeWidth = 22f,
                        cap = StrokeCap.Round
                    )
                    // Left Knee Patch
                    drawCircle(color = vestDarkColor, radius = 9f, center = Offset((centerX - 16f + leftFootX) / 2f, (hipY + footY) / 2f))
                    // Left Trekking Boot
                    drawRoundRect(
                        color = bootsColor,
                        topLeft = Offset(leftFootX - 14f, footY - 14f),
                        size = Size(28f, 16f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    drawRect(color = Color.Black, topLeft = Offset(leftFootX - 14f, footY - 2f), size = Size(28f, 4f))

                    // Right Leg
                    drawLine(
                        color = pantsColor,
                        start = Offset(centerX + 16f, hipY),
                        end = Offset(rightFootX, footY),
                        strokeWidth = 22f,
                        cap = StrokeCap.Round
                    )
                    // Right Knee Patch
                    drawCircle(color = vestDarkColor, radius = 9f, center = Offset((centerX + 16f + rightFootX) / 2f, (hipY + footY) / 2f))
                    // Right Trekking Boot
                    drawRoundRect(
                        color = bootsColor,
                        topLeft = Offset(rightFootX - 14f, footY - 14f),
                        size = Size(28f, 16f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    drawRect(color = Color.Black, topLeft = Offset(rightFootX - 14f, footY - 2f), size = Size(28f, 4f))
                }

                // Torso with Khaki Field Shirt & Leather Vest
                val torsoTopY = currentY - characterHeight * 0.82f
                val torsoW = characterWidth * 0.76f
                val torsoH = hipY - torsoTopY

                // Heavy Canvas Explorer Vest
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(vestColor, vestDarkColor)),
                    topLeft = Offset(centerX - torsoW / 2f, torsoTopY),
                    size = Size(torsoW, torsoH),
                    cornerRadius = CornerRadius(14f, 14f)
                )

                // Field Shirt Collar Peak
                drawRoundRect(
                    color = shirtColor,
                    topLeft = Offset(centerX - torsoW * 0.28f, torsoTopY - 4f),
                    size = Size(torsoW * 0.56f, 12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Expedition Backpack (Mounted prominently on back facing viewer!)
                val packW = torsoW * 0.74f
                val packH = torsoH * 0.70f
                val packY = torsoTopY + 12f

                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(backpackColor, vestDarkColor)),
                    topLeft = Offset(centerX - packW / 2f, packY),
                    size = Size(packW, packH),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Backpack Flap & Brass Buckle
                drawRoundRect(
                    color = vestColor,
                    topLeft = Offset(centerX - packW * 0.40f, packY),
                    size = Size(packW * 0.80f, packH * 0.45f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawCircle(color = Color(0xFFFBBF24), radius = 3.5f, center = Offset(centerX, packY + packH * 0.42f))

                // Rolled Bedroll Lashed to Top of Pack
                val bedrollW = packW * 1.15f
                val bedrollH = 18f
                drawRoundRect(
                    color = bedrollColor,
                    topLeft = Offset(centerX - bedrollW / 2f, packY - bedrollH * 0.65f),
                    size = Size(bedrollW, bedrollH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Leather Lash Straps with Brass Rivets
                drawLine(color = vestDarkColor, start = Offset(centerX - bedrollW * 0.28f, packY - bedrollH * 0.65f), end = Offset(centerX - bedrollW * 0.28f, packY + bedrollH * 0.35f), strokeWidth = 4f)
                drawLine(color = vestDarkColor, start = Offset(centerX + bedrollW * 0.28f, packY - bedrollH * 0.65f), end = Offset(centerX + bedrollW * 0.28f, packY + bedrollH * 0.35f), strokeWidth = 4f)

                // Canteen attached to pack side
                drawRoundRect(
                    color = Color(0xFF64748B),
                    topLeft = Offset(centerX + packW / 2f - 3f, packY + packH * 0.25f),
                    size = Size(10f, 18f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawCircle(color = Color(0xFFFBBF24), radius = 2.5f, center = Offset(centerX + packW / 2f + 2f, packY + packH * 0.25f))

                // Utility Belt at Waist
                val beltH = 11f
                drawRect(
                    color = vestDarkColor,
                    topLeft = Offset(centerX - torsoW / 2f + 2f, hipY - beltH),
                    size = Size(torsoW - 4f, beltH)
                )
                // Brass Belt Buckle
                drawRoundRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(centerX - 8f, hipY - beltH - 1f),
                    size = Size(16f, beltH + 2f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Muscular Arms with Rolled Sleeves
                if (isJumping) {
                    // Arms raised up and out for balance
                    val lHandX = centerX - torsoW / 2f - 24f
                    val lHandY = torsoTopY - 26f
                    drawLine(color = shirtColor, start = Offset(centerX - torsoW / 2f, torsoTopY + 12f), end = Offset(lHandX + 10f, torsoTopY - 6f), strokeWidth = 14f, cap = StrokeCap.Round)
                    drawLine(color = skinColor, start = Offset(lHandX + 10f, torsoTopY - 6f), end = Offset(lHandX, lHandY), strokeWidth = 13f, cap = StrokeCap.Round)
                    drawCircle(color = skinColor, radius = 7.5f, center = Offset(lHandX, lHandY))

                    val rHandX = centerX + torsoW / 2f + 24f
                    val rHandY = torsoTopY - 26f
                    drawLine(color = shirtColor, start = Offset(centerX + torsoW / 2f, torsoTopY + 12f), end = Offset(rHandX - 10f, torsoTopY - 6f), strokeWidth = 14f, cap = StrokeCap.Round)
                    drawLine(color = skinColor, start = Offset(rHandX - 10f, torsoTopY - 6f), end = Offset(rHandX, rHandY), strokeWidth = 13f, cap = StrokeCap.Round)
                    drawCircle(color = skinColor, radius = 7.5f, center = Offset(rHandX, rHandY))
                } else {
                    // Running Athletic Arm Swings
                    val lHandY = torsoTopY + torsoH * 0.50f + armSwing * 0.55f
                    val lHandX = centerX - torsoW / 2f - 16f
                    drawLine(color = shirtColor, start = Offset(centerX - torsoW / 2f + 2f, torsoTopY + 12f), end = Offset(lHandX + 6f, lHandY - 14f), strokeWidth = 14f, cap = StrokeCap.Round)
                    drawLine(color = skinColor, start = Offset(lHandX + 6f, lHandY - 14f), end = Offset(lHandX, lHandY), strokeWidth = 13f, cap = StrokeCap.Round)
                    // Leather wristband
                    drawLine(color = vestDarkColor, start = Offset(lHandX + 2f, lHandY - 6f), end = Offset(lHandX + 5f, lHandY - 10f), strokeWidth = 4f)
                    drawCircle(color = skinColor, radius = 7.5f, center = Offset(lHandX, lHandY))

                    val rHandY = torsoTopY + torsoH * 0.50f - armSwing * 0.55f
                    val rHandX = centerX + torsoW / 2f + 16f
                    drawLine(color = shirtColor, start = Offset(centerX + torsoW / 2f - 2f, torsoTopY + 12f), end = Offset(rHandX - 6f, rHandY - 14f), strokeWidth = 14f, cap = StrokeCap.Round)
                    drawLine(color = skinColor, start = Offset(rHandX - 6f, rHandY - 14f), end = Offset(rHandX, rHandY), strokeWidth = 13f, cap = StrokeCap.Round)
                    // Leather wristband
                    drawLine(color = vestDarkColor, start = Offset(rHandX - 2f, rHandY - 6f), end = Offset(rHandX - 5f, rHandY - 10f), strokeWidth = 4f)
                    drawCircle(color = skinColor, radius = 7.5f, center = Offset(rHandX, rHandY))
                }

                // Head, Dark Textured Hair & Crimson Explorer Bandana
                val headR = 21f
                val headCenterY = currentY - characterHeight + headR + 4f

                // Neck
                drawRect(color = skinColor, topLeft = Offset(centerX - 8f, torsoTopY - 8f), size = Size(16f, 10f))

                // Head Base
                drawCircle(color = skinColor, radius = headR, center = Offset(centerX, headCenterY))

                // Dark Textured Hair (Viewed from Behind)
                drawOval(
                    color = Color(0xFF1C1917),
                    topLeft = Offset(centerX - headR, headCenterY - headR),
                    size = Size(headR * 2f, headR * 1.5f)
                )

                // Crimson Adventurer Bandana / Headwrap
                drawRoundRect(
                    color = bandanaColor,
                    topLeft = Offset(centerX - headR * 0.95f, headCenterY - headR * 0.40f),
                    size = Size(headR * 1.90f, headR * 0.65f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Bandana Tied Knot Tails Streaming Behind
                drawLine(
                    color = bandanaColor,
                    start = Offset(centerX + 6f, headCenterY),
                    end = Offset(centerX + 18f, headCenterY + 14f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = bandanaColor,
                    start = Offset(centerX + 10f, headCenterY + 2f),
                    end = Offset(centerX + 24f, headCenterY + 10f),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Driving Traffic Vehicle Model
 */
data class DrivingTrafficVehicle(
    val vehicleId: Int,
    val lane: Int, // 0=Left, 1=Center, 2=Right
    var depth: Float, // 0.0 (horizon) to 1.0 (player)
    val color: Color,
    val vehicleType: Int = 0, // 0=Sedan/Taxi, 1=Coupe, 2=Van, 3=SUV
    val onsetElapsedRealtime: Long,
    var evaluated: Boolean = false
)

/**
 * 2. LANDSCAPE DRIVING GAME VIEW
 * Fully redesigned, arcade-grade Dr. Driving simulation featuring:
 * - Substantial, highly detailed GT Sports Car (190px x 265px) with visible body panels, cabin glass,
 *   aerodynamic rear wing, wide rubber tires, and animated front steering wheel angle
 * - High-intensity LED brake lighting system that casts expansive red volumetric glow when braking
 * - Multiple recognizable traffic vehicles (Sedans, Delivery Vans, Sports Coupes) with perspective scaling
 * - 3-Lane curving highway with realistic roadside curbs, guardrails, and scenery
 * - Continuous interactive steering wheel on lower-left and tactile gas/brake pedals on lower-right
 * - Exact 30-second monotonic active research timing
 */
@Composable
private fun LandscapeDrivingGameView(
    trialAttemptId: Int = 1,
    isPractice: Boolean,
    isGameRunning: Boolean,
    isGamePaused: Boolean,
    timeRemainingSeconds: Int,
    trialResults: GamingTrialResults?,
    onStartPractice: () -> Unit,
    onStartOfficialTrial: () -> Unit,
    onPauseResume: () -> Unit,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit,
    onTrialFinished: (GamingTrialResults) -> Unit,
    onActivityCompleted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    BackHandler {
        onBack()
    }

    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var carLateralOffset by remember { mutableFloatStateOf(0f) } // -100f to +100f
    var speedKmH by remember { mutableIntStateOf(60) }
    var wheelRotationAngle by remember { mutableFloatStateOf(0f) } // -110f to +110f
    var isDraggingWheel by remember { mutableStateOf(false) }
    var isAccelerating by remember { mutableStateOf(false) }
    var isBraking by remember { mutableStateOf(false) }

    var steeringStatus by remember { mutableStateOf("Steer with the wheel to stay centered & avoid traffic") }
    var collisionFlash by remember { mutableStateOf(false) }

    var pendingSteerTime by remember { mutableLongStateOf(0L) }
    var pendingSteerAction by remember { mutableStateOf<String?>(null) }
    var lastSteerLogTime by remember { mutableLongStateOf(0L) }

    var distanceTraveledMeters by remember { mutableFloatStateOf(0f) }
    var normalizedDeviation by remember { mutableFloatStateOf(0f) }
    var deviationCount by remember { mutableIntStateOf(0) }
    var wasDeviated by remember { mutableStateOf(false) }

    var trafficEncountered by remember { mutableIntStateOf(0) }
    var trafficAvoided by remember { mutableIntStateOf(0) }
    var trafficCollisions by remember { mutableIntStateOf(0) }
    var totalLatencySum by remember { mutableLongStateOf(0L) }
    var latencyEventsCount by remember { mutableIntStateOf(0) }
    var actionsCount by remember { mutableIntStateOf(0) }
    var drivingScore by remember { mutableIntStateOf(0) }

    val trafficCars = remember { mutableStateListOf<DrivingTrafficVehicle>() }
    var trafficIdCounter by remember { mutableIntStateOf(1) }

    var roadScrollOffset by remember { mutableFloatStateOf(0f) }
    var roadCurveOffset by remember { mutableFloatStateOf(0f) }

    // Reset game state whenever a new official trial attempt is initiated
    LaunchedEffect(trialAttemptId) {
        if (!isPractice && isGameRunning) {
            trafficCars.clear()
            carLateralOffset = 0f
            playerLane = 1
            wheelRotationAngle = 0f
            trafficEncountered = 0
            trafficAvoided = 0
            trafficCollisions = 0
            drivingScore = 0
            distanceTraveledMeters = 0f
            totalLatencySum = 0L
            latencyEventsCount = 0
            actionsCount = 0
            collisionFlash = false
        }
    }

    // Driving Simulation Loop
    LaunchedEffect(isGameRunning, isGamePaused) {
        var lastTrafficSpawn = SystemClock.elapsedRealtime()

        while (isGameRunning && !isGamePaused) {
            val now = SystemClock.elapsedRealtime()

            // Smooth spring auto-centering of steering wheel when released
            if (!isDraggingWheel && abs(wheelRotationAngle) > 0.5f) {
                wheelRotationAngle *= 0.84f
                if (abs(wheelRotationAngle) < 0.8f) wheelRotationAngle = 0f
            }

            // Acceleration & Braking dynamics
            if (isAccelerating) {
                speedKmH = (speedKmH + 2).coerceAtMost(120)
            } else if (isBraking) {
                speedKmH = (speedKmH - 3).coerceAtLeast(20)
            } else {
                if (speedKmH > 60) speedKmH = (speedKmH - 1).coerceAtLeast(60)
                else if (speedKmH < 60) speedKmH = (speedKmH + 1).coerceAtMost(60)
            }

            // Steering dynamics: wheel rotation drives lateral car position
            val steerDelta = (wheelRotationAngle / 70f) * (speedKmH / 60f) * 3.8f
            carLateralOffset = (carLateralOffset + steerDelta).coerceIn(-100f, 100f)

            // Dynamic player lane assignment
            playerLane = when {
                carLateralOffset < -33f -> 0 // Left lane
                carLateralOffset > 33f -> 2  // Right lane
                else -> 1                    // Center lane
            }

            // Distance & Score progression
            val distanceDelta = (speedKmH * (1000f / 3600f) * 0.035f)
            distanceTraveledMeters += distanceDelta
            drivingScore += (distanceDelta * 0.25f).toInt()

            // Road scroll & curving
            roadScrollOffset = (roadScrollOffset + (speedKmH / 400f) * 0.05f) % 1.0f
            roadCurveOffset = sin(distanceTraveledMeters * 0.012f) * 50f

            // Traffic generation
            if (now - lastTrafficSpawn > 2200L) {
                lastTrafficSpawn = now
                val lane = Random.nextInt(0, 3)
                val type = Random.nextInt(0, 4)
                val carColor = when (type) {
                    0 -> Color(0xFFEAB308) // Yellow taxi
                    1 -> Color(0xFF3B82F6) // Blue coupe
                    2 -> Color(0xFF10B981) // Green delivery van
                    else -> Color(0xFFA855F7) // Purple crossover
                }
                trafficCars.add(
                    DrivingTrafficVehicle(
                        vehicleId = trafficIdCounter++,
                        lane = lane,
                        depth = 0.0f,
                        color = carColor,
                        vehicleType = type,
                        onsetElapsedRealtime = now
                    )
                )
            }

            // Advance & evaluate traffic
            val trafficIterator = trafficCars.listIterator()
            while (trafficIterator.hasNext()) {
                val traffic = trafficIterator.next()
                val approachSpeed = 0.016f + (speedKmH / 100f) * 0.018f
                traffic.depth += approachSpeed

                // Collision impact zone: depth between 0.84 and 0.98
                if (traffic.depth >= 0.84f && !traffic.evaluated) {
                    traffic.evaluated = true
                    trafficEncountered++

                    val isSameLane = (playerLane == traffic.lane)
                    if (isSameLane) {
                        trafficCollisions++
                        speedKmH = (speedKmH - 30).coerceAtLeast(20)
                        drivingScore = (drivingScore - 30).coerceAtLeast(0)
                        collisionFlash = true
                        steeringStatus = "💥 Collision with traffic in Lane ${traffic.lane + 1}!"

                        if (!isPractice) {
                            onLogEvent(
                                "avoid_traffic",
                                "stayed_in_lane",
                                "collision",
                                trialAttemptId,
                                -1L,
                                "encounter=${traffic.vehicleId},trafficLane=${traffic.lane},playerLane=$playerLane,speed=$speedKmH"
                            )
                        }
                    } else {
                        trafficAvoided++
                        drivingScore += 45

                        val qualifyingSteerTime = pendingSteerTime
                        val steerLatency = if (qualifyingSteerTime > 0L && qualifyingSteerTime >= traffic.onsetElapsedRealtime) {
                            qualifyingSteerTime - traffic.onsetElapsedRealtime
                        } else {
                            -1L
                        }

                        if (steerLatency > 0L) {
                            totalLatencySum += steerLatency
                            latencyEventsCount++
                        }

                        val detectedAction = if (steerLatency > 0L) (pendingSteerAction ?: "steer_around") else "steer_around"
                        steeringStatus = "✓ Overtook traffic car smoothly!"

                        if (!isPractice) {
                            onLogEvent(
                                "avoid_traffic",
                                detectedAction,
                                "successful",
                                trialAttemptId,
                                steerLatency,
                                "encounter=${traffic.vehicleId},trafficLane=${traffic.lane},playerLane=$playerLane,speed=$speedKmH"
                            )
                        }

                        pendingSteerTime = 0L
                        pendingSteerAction = null
                    }
                } else if (traffic.depth > 1.15f) {
                    trafficIterator.remove()
                }
            }

            // Road lane deviation calculation
            val roadBound = 110f
            val dev = abs(carLateralOffset) / roadBound
            normalizedDeviation = dev.coerceIn(0f, 2f)

            if (normalizedDeviation > 1.0f && !wasDeviated) {
                deviationCount++
                wasDeviated = true
                steeringStatus = "⚠️ Paved Road Boundary Exceeded!"
                if (!isPractice) {
                    onLogEvent("stay_in_lane", "lane_deviation", "deviation", trialAttemptId, -1L, "offset=$carLateralOffset,dev=$normalizedDeviation")
                }
            } else if (normalizedDeviation <= 1.0f && wasDeviated) {
                wasDeviated = false
            }

            delay(35L)
        }

        // On 30-second official trial expired
        if (!isGameRunning && timeRemainingSeconds == 0 && distanceTraveledMeters > 0f && trialResults == null) {
            val avgLat = if (latencyEventsCount > 0) totalLatencySum / latencyEventsCount else -1L
            onTrialFinished(
                GamingTrialResults(
                    gameMode = "Driving (Dr. Driving–Inspired)",
                    durationSeconds = 30.0f,
                    score = drivingScore,
                    encountersTotal = trafficEncountered,
                    avoidedTotal = trafficAvoided,
                    collisionsTotal = trafficCollisions,
                    actionsCount = actionsCount,
                    averageLatencyMs = avgLat,
                    extraMetricLabel = "Distance Traveled",
                    extraMetricValue = "${distanceTraveledMeters.toInt()} meters",
                    completionStatus = "COMPLETED"
                )
            )
        }
    }

    LaunchedEffect(collisionFlash) {
        if (collisionFlash) {
            delay(300L)
            collisionFlash = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val horizonY = height * 0.22f
            val bottomY = height

            // 1. SKY & SUNSET HORIZON
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81)),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset(0f, 0f),
                size = Size(width, horizonY)
            )

            // Distant Mountains
            val mtnPath = Path().apply {
                moveTo(0f, horizonY)
                lineTo(width * 0.15f, horizonY - 35f)
                lineTo(width * 0.32f, horizonY - 12f)
                lineTo(width * 0.52f, horizonY - 48f)
                lineTo(width * 0.74f, horizonY - 20f)
                lineTo(width * 0.88f, horizonY - 40f)
                lineTo(width, horizonY)
                close()
            }
            drawPath(mtnPath, Color(0xFF1E293B))

            // 2. ROADSIDE TERRAIN GROUND
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF022C22)),
                    startY = horizonY,
                    endY = bottomY
                ),
                topLeft = Offset(0f, horizonY),
                size = Size(width, bottomY - horizonY)
            )

            // 3. ROAD PERSPECTIVE GEOMETRY (Landscape Highway)
            val roadBottomW = width * 0.65f
            val roadHorizonW = width * 0.18f
            val horizonCenter = width / 2f + roadCurveOffset * 0.35f
            val bottomCenter = width / 2f + roadCurveOffset

            val leftBottomX = bottomCenter - roadBottomW / 2f
            val rightBottomX = bottomCenter + roadBottomW / 2f
            val leftHorizonX = horizonCenter - roadHorizonW / 2f
            val rightHorizonX = horizonCenter + roadHorizonW / 2f

            // Asphalt Road Surface
            val roadPath = Path().apply {
                moveTo(leftHorizonX, horizonY)
                lineTo(rightHorizonX, horizonY)
                lineTo(rightBottomX, bottomY)
                lineTo(leftBottomX, bottomY)
                close()
            }
            drawPath(
                path = roadPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                    startY = horizonY,
                    endY = bottomY
                )
            )

            // Roadside Alternating Red/White Curbs
            val curbSegments = 12
            for (cIdx in 0..curbSegments) {
                val depth1 = ((cIdx.toFloat() / curbSegments) + roadScrollOffset * (1f / curbSegments)) % 1.0f
                val depth2 = (depth1 + (0.8f / curbSegments)).coerceAtMost(1.0f)
                if (depth2 > depth1) {
                    val y1 = horizonY + (bottomY - horizonY) * depth1
                    val y2 = horizonY + (bottomY - horizonY) * depth2

                    val w1 = roadHorizonW + (roadBottomW - roadHorizonW) * depth1
                    val w2 = roadHorizonW + (roadBottomW - roadHorizonW) * depth2
                    val c1 = horizonCenter + (bottomCenter - horizonCenter) * depth1
                    val c2 = horizonCenter + (bottomCenter - horizonCenter) * depth2

                    val curbColor = if (cIdx % 2 == 0) Color(0xFFEF4444) else Color(0xFFF8FAFC)
                    val curbWidth1 = 6f + depth1 * 16f
                    val curbWidth2 = 6f + depth2 * 16f

                    // Left curb
                    val lCurb = Path().apply {
                        moveTo(c1 - w1 / 2f - curbWidth1, y1)
                        lineTo(c1 - w1 / 2f, y1)
                        lineTo(c2 - w2 / 2f, y2)
                        lineTo(c2 - w2 / 2f - curbWidth2, y2)
                        close()
                    }
                    drawPath(lCurb, curbColor)

                    // Right curb
                    val rCurb = Path().apply {
                        moveTo(c1 + w1 / 2f, y1)
                        lineTo(c1 + w1 / 2f + curbWidth1, y1)
                        lineTo(c2 + w2 / 2f + curbWidth2, y2)
                        lineTo(c2 + w2 / 2f, y2)
                        close()
                    }
                    drawPath(rCurb, curbColor)
                }
            }

            // 4. THREE ROAD LANES WITH ANIMATED DASHED LINES
            for (laneDiv in 1..2) {
                val topX = leftHorizonX + (roadHorizonW / 3f) * laneDiv
                val botX = leftBottomX + (roadBottomW / 3f) * laneDiv

                for (dashIdx in 0..7) {
                    val depth = (dashIdx * 0.14f + roadScrollOffset * 0.14f) % 1.0f
                    val dashY = horizonY + (bottomY - horizonY) * depth
                    val dashX = topX + (botX - topX) * depth
                    val dashH = 10f + depth * 32f
                    val dashW = 3f + depth * 5.5f

                    drawRoundRect(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(dashX - dashW / 2f, dashY),
                        size = Size(dashW, dashH),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            // 5. DETAILED TRAFFIC VEHICLES WITH DEPTH SCALING
            for (traffic in trafficCars) {
                val obsY = horizonY + (bottomY - horizonY) * traffic.depth
                val currentRoadW = roadHorizonW + (roadBottomW - roadHorizonW) * traffic.depth
                val currentCenter = horizonCenter + (bottomCenter - horizonCenter) * traffic.depth
                val currentLeftX = currentCenter - currentRoadW / 2f
                val laneW = currentRoadW / 3f
                val trafficCenterX = currentLeftX + laneW * traffic.lane + (laneW / 2f)

                val carW = laneW * 0.68f
                val carH = carW * 1.35f

                drawTrafficVehicle(
                    scope = this,
                    centerX = trafficCenterX,
                    centerY = obsY,
                    width = carW,
                    height = carH,
                    color = traffic.color,
                    type = traffic.vehicleType
                )
            }

            // 6. PLAYER CAR MODEL: SUBSTANTIAL, ELEVATED 3D SPORTS CAR
            val playerLaneW = roadBottomW / 3f
            val playerCarCenterX = bottomCenter + (carLateralOffset / 100f) * (playerLaneW * 1.12f)
            val playerCarH = (height * 0.38f).coerceIn(160f, 220f)
            val playerCarW = playerCarH * 0.72f
            val playerCarY = bottomY - playerCarH * 0.54f

            drawPlayerVehicle(
                scope = this,
                centerX = playerCarCenterX,
                centerY = playerCarY,
                width = playerCarW,
                height = playerCarH,
                wheelAngle = wheelRotationAngle,
                isBraking = isBraking,
                isAccelerating = isAccelerating,
                collisionFlash = collisionFlash
            )

            // Full-screen Collision Impact Vignette
            if (collisionFlash) {
                drawRect(
                    color = Color(0xFFDC2626).copy(alpha = 0.45f),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, height)
                )
            }
        }

        // TOP LANDSCAPE FLOATING HUD BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button & Mode Badge
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, BorderSubtle),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("✕ Exit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPractice) Color(0xFFCA8A04) else Color(0xFF2563EB)
                ) {
                    Text(
                        text = if (isPractice) "PRACTICE DRIVE" else "OFFICIAL 30s TRIAL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Center: Speedometer, Monotonic Countdown Timer & Score
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, if (speedKmH > 90) Color(0xFFEF4444) else BorderSubtle)
                ) {
                    Text(
                        text = "$speedKmH km/h",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = if (speedKmH > 90) Color(0xFFF87171) else Color(0xFF4ADE80),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, AccentTeal)
                ) {
                    Text(
                        text = if (isPractice && !isGameRunning) "PRACTICE" else "⏱️ 00:%02d".format(timeRemainingSeconds),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "Score: $drivingScore • ${distanceTraveledMeters.toInt()}m",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Traffic Stats & Pause/Resume
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "✓ $trafficAvoided   💥 $trafficCollisions",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isGameRunning) {
                    Button(
                        onClick = onPauseResume,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isGamePaused) Color(0xFF16A34A) else Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(if (isGamePaused) "▶ Resume" else "⏸ Pause", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Floating Dynamic Steering Status Pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = steeringStatus,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                color = if (normalizedDeviation > 1.0f) Color(0xFFF87171) else Color(0xFF38BDF8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // LOWER-LEFT: INTERACTIVE DRAGGABLE RACING STEERING WHEEL
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 14.dp)
                .size(175.dp)
                .pointerInput(isGameRunning, isPractice) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var dragStartAngle = 0f
                    var dragStartWheelAngle = 0f

                    detectDragGestures(
                        onDragStart = { offset ->
                            isDraggingWheel = true
                            dragStartAngle = Math.toDegrees(atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())).toFloat()
                            dragStartWheelAngle = wheelRotationAngle
                        },
                        onDragEnd = { isDraggingWheel = false },
                        onDragCancel = { isDraggingWheel = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curAngle = Math.toDegrees(atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())).toFloat()
                            var deltaAngle = curAngle - dragStartAngle
                            while (deltaAngle > 180f) deltaAngle -= 360f
                            while (deltaAngle < -180f) deltaAngle += 360f

                            val horizontalAssist = dragAmount.x * 0.45f
                            val newAngle = (dragStartWheelAngle + deltaAngle + horizontalAssist).coerceIn(-110f, 110f)
                            wheelRotationAngle = newAngle

                            // Research event logging on significant steering action
                            if (abs(newAngle) > 28f) {
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastSteerLogTime > 350L) {
                                    lastSteerLogTime = now
                                    actionsCount++
                                    val action = if (newAngle > 0) "steer_right" else "steer_left"
                                    pendingSteerTime = now
                                    pendingSteerAction = action
                                    if (!isPractice && isGameRunning) {
                                        onLogEvent(
                                            action,
                                            action,
                                            "successful",
                                            trialAttemptId,
                                            -1L,
                                            "wheelAngle=${newAngle.toInt()},lane=$playerLane,speed=$speedKmH"
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val rOuter = w * 0.46f

                // Wheel Drop Shadow
                drawCircle(color = Color.Black.copy(alpha = 0.55f), radius = rOuter, center = Offset(cx, cy + 8f))

                rotate(wheelRotationAngle, pivot = Offset(cx, cy)) {
                    val rimStroke = rOuter * 0.22f

                    // Outer Leather Grip Rim
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = rOuter - rimStroke / 2f,
                        style = Stroke(width = rimStroke)
                    )

                    // Textured grip notches at 9 & 3 o'clock
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = 158f,
                        sweepAngle = 44f,
                        useCenter = false,
                        style = Stroke(width = rimStroke * 1.15f),
                        topLeft = Offset(cx - rOuter + rimStroke / 2f, cy - rOuter + rimStroke / 2f),
                        size = Size((rOuter - rimStroke / 2f) * 2f, (rOuter - rimStroke / 2f) * 2f)
                    )
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = -22f,
                        sweepAngle = 44f,
                        useCenter = false,
                        style = Stroke(width = rimStroke * 1.15f),
                        topLeft = Offset(cx - rOuter + rimStroke / 2f, cy - rOuter + rimStroke / 2f),
                        size = Size((rOuter - rimStroke / 2f) * 2f, (rOuter - rimStroke / 2f) * 2f)
                    )

                    // Racing Teal Center Alignment Strip at 12 o'clock
                    drawArc(
                        color = AccentTeal,
                        startAngle = 262f,
                        sweepAngle = 16f,
                        useCenter = false,
                        style = Stroke(width = rimStroke * 1.25f),
                        topLeft = Offset(cx - rOuter + rimStroke / 2f, cy - rOuter + rimStroke / 2f),
                        size = Size((rOuter - rimStroke / 2f) * 2f, (rOuter - rimStroke / 2f) * 2f)
                    )

                    // 3 Brushed Aluminum Structural Spokes
                    val spokeWidth = 15f
                    val hubRadius = rOuter * 0.38f
                    drawLine(color = Color(0xFF64748B), start = Offset(cx - hubRadius, cy), end = Offset(cx - rOuter + rimStroke, cy), strokeWidth = spokeWidth)
                    drawLine(color = Color(0xFF64748B), start = Offset(cx + hubRadius, cy), end = Offset(cx + rOuter - rimStroke, cy), strokeWidth = spokeWidth)
                    drawLine(color = Color(0xFF64748B), start = Offset(cx, cy + hubRadius), end = Offset(cx, cy + rOuter - rimStroke), strokeWidth = spokeWidth)

                    // Center Hub with Adapt-IQ Emblem
                    drawCircle(color = Color(0xFF0F172A), radius = hubRadius, center = Offset(cx, cy))
                    drawCircle(color = Color(0xFF94A3B8), radius = hubRadius, center = Offset(cx, cy), style = Stroke(width = 3f))
                    drawCircle(color = AccentTeal, radius = hubRadius * 0.42f, center = Offset(cx, cy))
                }
            }
        }

        // LOWER-RIGHT: GAS & BRAKE PEDAL CONTROLS
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Brake Pedal
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(88.dp)
                    .background(
                        color = if (isBraking) StatusError else Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isBraking) Color(0xFFFCA5A5) else StatusError,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(isGameRunning) {
                        detectTapGestures(
                            onPress = {
                                isBraking = true
                                actionsCount++
                                if (!isPractice && isGameRunning) {
                                    onLogEvent("brake", "brake", "successful", trialAttemptId, -1L, "speed=$speedKmH")
                                }
                                tryAwaitRelease()
                                isBraking = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(46.dp)
                                .height(3.dp)
                                .background(Color(0xFFB91C1C).copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                    Text("BRAKE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Gas (Accelerator) Pedal
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(118.dp)
                    .background(
                        color = if (isAccelerating) StatusSuccess else Color(0xFF14532D),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isAccelerating) Color(0xFF86EFAC) else StatusSuccess,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(isGameRunning) {
                        detectTapGestures(
                            onPress = {
                                isAccelerating = true
                                actionsCount++
                                if (!isPractice && isGameRunning) {
                                    onLogEvent("accelerate", "accelerate", "successful", trialAttemptId, -1L, "speed=$speedKmH")
                                }
                                tryAwaitRelease()
                                isAccelerating = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(3.dp)
                                .background(Color(0xFF15803D).copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                    Text("GAS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // PRACTICE LAUNCH OVERLAY
        if (isPractice && !isGameRunning && trialResults == null) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(440.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = AccentBlue
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🏎️ Dr. Driving Research Simulation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "• Drag the steering wheel (lower-left) to steer continuously.\n• Hold GAS to accelerate and BRAKE to slow down.\n• Avoid oncoming traffic across the 3 perspective lanes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdaptIQOutlinedButton(
                            text = "Practice Drive",
                            onClick = onStartPractice,
                            modifier = Modifier.weight(1f),
                            borderColor = BorderSubtle,
                            textColor = TextPrimary
                        )

                        AdaptIQPrimaryButton(
                            text = "Start Official 30s",
                            onClick = onStartOfficialTrial,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // FROSTED PAUSE OVERLAY
        if (isGamePaused) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(380.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = BorderSubtle
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⏸️ GAME PAUSED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Active trial countdown is frozen.\nTap 'Resume' to continue gameplay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    AdaptIQPrimaryButton(
                        text = "Resume Gameplay",
                        onClick = onPauseResume,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // TRIAL COMPLETED RESULTS DIALOG
        if (trialResults != null) {
            val res = trialResults
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(480.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = StatusSuccess
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✓ 30-Second Trial Completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                    Text(
                        text = "Status: ${res.completionStatus} • Active Duration: 30.00s",
                        fontWeight = FontWeight.SemiBold,
                        color = AccentTeal,
                        fontSize = 12.sp
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BorderSubtle)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Game Score:", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${res.score} pts", fontWeight = FontWeight.Bold, color = StatusSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Traffic Avoided / Total:", color = TextSecondary)
                        Text("${res.avoidedTotal} / ${res.encountersTotal}", color = AccentBlue)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Collisions Recorded:", color = TextSecondary)
                        Text("${res.collisionsTotal}", color = if (res.collisionsTotal > 0) StatusError else StatusSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Steering & Pedal Actions:", color = TextSecondary)
                        Text("${res.actionsCount}", color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Average Reaction Latency:", color = TextSecondary)
                        Text(if (res.averageLatencyMs > 0L) "${res.averageLatencyMs} ms" else "NA", color = TextPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Distance Traveled:", color = TextSecondary)
                        Text(res.extraMetricValue, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    AdaptIQPrimaryButton(
                        text = "Complete Activity & Return to Dashboard",
                        onClick = onActivityCompleted,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdaptIQOutlinedButton(
                        text = "Retry 30-Second Scored Trial",
                        onClick = onStartOfficialTrial,
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderSubtle,
                        textColor = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Detailed Canvas Drawing of Player GT Sports Car
 * Elevated Rear 3D Perspective (190px x 265px)
 */
private fun drawPlayerVehicle(
    scope: DrawScope,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    wheelAngle: Float,
    isBraking: Boolean,
    isAccelerating: Boolean,
    collisionFlash: Boolean
) {
    with(scope) {
        val rollAngle = (wheelAngle / 110f) * 11f

        rotate(rollAngle, pivot = Offset(centerX, centerY)) {
            // 1. Ambient Ground Shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(centerX - width * 0.52f, centerY - height * 0.45f + 12f),
                size = Size(width * 1.04f, height * 0.95f),
                cornerRadius = CornerRadius(22f, 22f)
            )

            // 2. Wide Tires
            val tireW = width * 0.14f
            val tireH = height * 0.22f

            // Rear Left & Right Tires (Straight)
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(centerX - width * 0.48f, centerY + height * 0.16f),
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(centerX + width * 0.48f - tireW, centerY + height * 0.16f),
                size = Size(tireW, tireH),
                cornerRadius = CornerRadius(5f, 5f)
            )

            // Front Left & Right Tires (Steered with wheel angle!)
            val frontSteerAngle = (wheelAngle / 110f) * 22f
            rotate(frontSteerAngle, pivot = Offset(centerX - width * 0.48f + tireW / 2f, centerY - height * 0.26f)) {
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerX - width * 0.48f, centerY - height * 0.36f),
                    size = Size(tireW, tireH),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }
            rotate(frontSteerAngle, pivot = Offset(centerX + width * 0.48f - tireW / 2f, centerY - height * 0.26f)) {
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerX + width * 0.48f - tireW, centerY - height * 0.36f),
                    size = Size(tireW, tireH),
                    cornerRadius = CornerRadius(5f, 5f)
                )
            }

            // 3. Widebody Chassis Lower Diffuser
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(centerX - width * 0.42f, centerY + height * 0.30f),
                size = Size(width * 0.84f, height * 0.15f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Quad Chrome Exhaust Pipes
            val exhaustRadius = 5.5f
            val exhaustY = centerY + height * 0.44f
            drawCircle(color = Color(0xFF94A3B8), radius = exhaustRadius, center = Offset(centerX - width * 0.26f, exhaustY))
            drawCircle(color = Color(0xFF0F172A), radius = exhaustRadius * 0.6f, center = Offset(centerX - width * 0.26f, exhaustY))
            drawCircle(color = Color(0xFF94A3B8), radius = exhaustRadius, center = Offset(centerX - width * 0.18f, exhaustY))
            drawCircle(color = Color(0xFF0F172A), radius = exhaustRadius * 0.6f, center = Offset(centerX - width * 0.18f, exhaustY))

            drawCircle(color = Color(0xFF94A3B8), radius = exhaustRadius, center = Offset(centerX + width * 0.18f, exhaustY))
            drawCircle(color = Color(0xFF0F172A), radius = exhaustRadius * 0.6f, center = Offset(centerX + width * 0.18f, exhaustY))
            drawCircle(color = Color(0xFF94A3B8), radius = exhaustRadius, center = Offset(centerX + width * 0.26f, exhaustY))
            drawCircle(color = Color(0xFF0F172A), radius = exhaustRadius * 0.6f, center = Offset(centerX + width * 0.26f, exhaustY))

            // 4. Main Sculpted Body (Vibrant Candy Red Sports Car)
            val bodyBrush = Brush.verticalGradient(
                colors = if (collisionFlash) {
                    listOf(Color(0xFFEF4444), Color(0xFF7F1D1D))
                } else {
                    listOf(Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFF7F1D1D))
                },
                startY = centerY - height * 0.5f,
                endY = centerY + height * 0.4f
            )

            drawRoundRect(
                brush = bodyBrush,
                topLeft = Offset(centerX - width * 0.44f, centerY - height * 0.45f),
                size = Size(width * 0.88f, height * 0.85f),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Dual Racing Center Stripes
            val stripeW = 8f
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(centerX - stripeW - 3f, centerY - height * 0.45f),
                size = Size(stripeW, height * 0.85f)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.85f),
                topLeft = Offset(centerX + 3f, centerY - height * 0.45f),
                size = Size(stripeW, height * 0.85f)
            )

            // Front Hood Air Extractor Vents
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(centerX - width * 0.28f, centerY - height * 0.38f),
                size = Size(width * 0.18f, height * 0.08f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(centerX + width * 0.10f, centerY - height * 0.38f),
                size = Size(width * 0.18f, height * 0.08f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 5. Aerodynamic Cabin Glasshouse
            val cabinW = width * 0.62f
            val cabinH = height * 0.44f
            val cabinY = centerY - height * 0.22f

            // Front Windshield
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF38BDF8).copy(alpha = 0.7f))),
                topLeft = Offset(centerX - cabinW * 0.44f, cabinY),
                size = Size(cabinW * 0.88f, cabinH * 0.32f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Carbon-Fiber Roof Panel
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(centerX - cabinW * 0.42f, cabinY + cabinH * 0.28f),
                size = Size(cabinW * 0.84f, cabinH * 0.34f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Rear Windshield Glass with Sky Reflection
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0B1222))),
                topLeft = Offset(centerX - cabinW * 0.44f, cabinY + cabinH * 0.60f),
                size = Size(cabinW * 0.88f, cabinH * 0.38f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Side Mirrors
            val mirrorW = 16f
            val mirrorH = 9f
            drawRoundRect(color = Color(0xFF991B1B), topLeft = Offset(centerX - width * 0.48f - mirrorW, cabinY + 8f), size = Size(mirrorW, mirrorH), cornerRadius = CornerRadius(3f, 3f))
            drawRoundRect(color = Color(0xFF38BDF8), topLeft = Offset(centerX - width * 0.48f - mirrorW + 2f, cabinY + 9f), size = Size(mirrorW - 4f, mirrorH - 2f), cornerRadius = CornerRadius(2f, 2f))

            drawRoundRect(color = Color(0xFF991B1B), topLeft = Offset(centerX + width * 0.48f, cabinY + 8f), size = Size(mirrorW, mirrorH), cornerRadius = CornerRadius(3f, 3f))
            drawRoundRect(color = Color(0xFF38BDF8), topLeft = Offset(centerX + width * 0.48f + 2f, cabinY + 9f), size = Size(mirrorW - 4f, mirrorH - 2f), cornerRadius = CornerRadius(2f, 2f))

            // 6. High-Downforce Carbon Rear Wing / Spoiler
            val wingW = width * 0.82f
            val wingH = 16f
            val wingY = centerY + height * 0.32f

            // Dual Spoiler Mounting Pylons
            drawRect(color = Color(0xFF0F172A), topLeft = Offset(centerX - width * 0.24f, wingY - 6f), size = Size(6f, 14f))
            drawRect(color = Color(0xFF0F172A), topLeft = Offset(centerX + width * 0.24f - 6f, wingY - 6f), size = Size(6f, 14f))

            // Main Wing Blade
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(centerX - wingW / 2f, wingY),
                size = Size(wingW, wingH),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 7. Dynamic LED Taillights System
            val tailLightY = centerY + height * 0.36f
            val tailLightW = width * 0.28f
            val tailLightH = 9f

            val lightGlowColor = if (isBraking) Color(0xFFEF4444) else Color(0xFFDC2626)
            val lightCoreColor = if (isBraking) Color.White else Color(0xFFFCA5A5)

            // Volumetric Braking Glow on Pavement
            if (isBraking) {
                drawOval(
                    color = Color(0xFFDC2626).copy(alpha = 0.55f),
                    topLeft = Offset(centerX - width * 0.55f, tailLightY - 8f),
                    size = Size(width * 1.10f, 42f)
                )
            }

            // Left Taillight Blade
            drawRoundRect(
                color = lightGlowColor,
                topLeft = Offset(centerX - width * 0.38f, tailLightY),
                size = Size(tailLightW, tailLightH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = lightCoreColor,
                topLeft = Offset(centerX - width * 0.36f, tailLightY + 2f),
                size = Size(tailLightW - 4f, tailLightH - 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Right Taillight Blade
            drawRoundRect(
                color = lightGlowColor,
                topLeft = Offset(centerX + width * 0.38f - tailLightW, tailLightY),
                size = Size(tailLightW, tailLightH),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = lightCoreColor,
                topLeft = Offset(centerX + width * 0.38f - tailLightW + 2f, tailLightY + 2f),
                size = Size(tailLightW - 4f, tailLightH - 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Illuminated License Plate "ADAPT-IQ"
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(centerX - 24f, tailLightY + 10f),
                size = Size(48f, 13f),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
    }
}

/**
 * Detailed Canvas Drawing of Traffic Vehicles
 */
private fun drawTrafficVehicle(
    scope: DrawScope,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    color: Color,
    type: Int
) {
    with(scope) {
        // Shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.50f),
            topLeft = Offset(centerX - width * 0.50f, centerY - height * 0.45f + 6f),
            size = Size(width, height),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Tires
        val tireW = width * 0.16f
        val tireH = height * 0.20f
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(centerX - width * 0.48f, centerY - height * 0.30f), size = Size(tireW, tireH), cornerRadius = CornerRadius(3f, 3f))
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(centerX + width * 0.48f - tireW, centerY - height * 0.30f), size = Size(tireW, tireH), cornerRadius = CornerRadius(3f, 3f))
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(centerX - width * 0.48f, centerY + height * 0.15f), size = Size(tireW, tireH), cornerRadius = CornerRadius(3f, 3f))
        drawRoundRect(color = Color(0xFF0F172A), topLeft = Offset(centerX + width * 0.48f - tireW, centerY + height * 0.15f), size = Size(tireW, tireH), cornerRadius = CornerRadius(3f, 3f))

        // Vehicle Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.75f))),
            topLeft = Offset(centerX - width * 0.44f, centerY - height * 0.45f),
            size = Size(width * 0.88f, height * 0.90f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Roof / Cabin Glasshouse
        val cabinW = width * 0.65f
        val cabinH = height * 0.45f
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(centerX - cabinW / 2f, centerY - height * 0.20f),
            size = Size(cabinW, cabinH),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Taxi roof sign if taxi
        if (type == 0) {
            drawRoundRect(
                color = Color(0xFFFDE047),
                topLeft = Offset(centerX - 16f, centerY - height * 0.05f),
                size = Size(32f, 10f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }

        // Dual Red Taillights
        val tailLightRadius = width * 0.08f
        val tailY = centerY + height * 0.38f
        drawCircle(color = Color(0xFFEF4444), radius = tailLightRadius, center = Offset(centerX - width * 0.30f, tailY))
        drawCircle(color = Color.White, radius = tailLightRadius * 0.45f, center = Offset(centerX - width * 0.30f, tailY))

        drawCircle(color = Color(0xFFEF4444), radius = tailLightRadius, center = Offset(centerX + width * 0.30f, tailY))
        drawCircle(color = Color.White, radius = tailLightRadius * 0.45f, center = Offset(centerX + width * 0.30f, tailY))
    }
}
