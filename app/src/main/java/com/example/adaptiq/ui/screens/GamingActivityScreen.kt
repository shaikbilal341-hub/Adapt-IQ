package com.example.adaptiq.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
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

    fun handleBackNavigation(modeStr: String) {
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
        onBack()
    }

    // Phase 3: Landscape-First Dedicated Screen for Driving Game
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
                handleBackNavigation("driving")
                gameMode = GamingMode.SELECTION
            }
        )
        return
    }

    AdaptIQScreenContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdaptIQSectionHeader(
                title = "Gaming-style Interaction",
                subtitle = "30-second playable research tasks evaluating motor reflexes, obstacle avoidance, steering stability, and response latency",
                badgeText = "ACTIVITY 13 OF 13"
            )

            if (gameMode == GamingMode.SELECTION) {
                Text(
                    text = "Select 30-Second Research Mini-Game",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )

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
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentTeal.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.4f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏃", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "3D Perspective Runner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "3-lane runner. Swipe on the gameplay track to dodge roadblocks, jump track hurdles, and slide under overhead arches.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text("➔", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

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
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AccentBlue.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏎️", fontSize = 22.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dr. Driving Research Simulation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Highway driving with oncoming traffic, lane changes, continuous steering wheel rotation, acceleration, and braking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text("➔", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                AdaptIQOutlinedButton(
                    text = "Back to Dashboard",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderSubtle,
                    textColor = TextSecondary
                )
            } else {
                val currentModeStr = if (gameMode == GamingMode.RUNNER) "runner" else "driving"

                // Header with Practice / Official Mode Controls
                PracticeHeader(
                    isPractice = isPractice,
                    onStartOfficialCollection = {
                        startOfficialTrial(currentModeStr)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (gameMode) {
                            GamingMode.RUNNER -> "Runner Game"
                            GamingMode.DRIVING -> "Driving Game"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isGameRunning) {
                            AdaptIQOutlinedButton(
                                text = if (isGamePaused) "Resume" else "Pause",
                                onClick = { handlePauseResume(currentModeStr) },
                                height = 36.dp,
                                borderColor = if (isGamePaused) StatusSuccess else BorderSubtle,
                                textColor = if (isGamePaused) StatusSuccess else TextPrimary
                            )
                        }

                        Surface(
                            shape = BadgeShape,
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
                    }
                }

                if (trialResults == null) {
                    when (gameMode) {
                        GamingMode.RUNNER -> {
                            RunnerGameView(
                                trialAttemptId = currentAttemptId,
                                isGameRunning = isGameRunning,
                                isGamePaused = isGamePaused,
                                isPractice = isPractice,
                                onStartPractice = {
                                    isGameRunning = true
                                    isGamePaused = false
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
                                }
                            )
                        }

                        else -> {}
                    }
                }

                // Scored Results Screen
                if (trialResults != null) {
                    val res = trialResults!!
                    AdaptIQSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceElevatedVariant,
                        borderColor = StatusSuccess
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
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Game Score:", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${res.score} pts", fontWeight = FontWeight.Bold, color = StatusSuccess)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Encounters Total:", color = TextSecondary)
                            Text("${res.encountersTotal}", color = TextPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Successfully Avoided:", color = TextSecondary)
                            Text("${res.avoidedTotal}", color = AccentBlue)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Collisions Recorded:", color = TextSecondary)
                            Text("${res.collisionsTotal}", color = if (res.collisionsTotal > 0) StatusError else StatusSuccess)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Average Response Latency:", color = TextSecondary)
                            Text(if (res.averageLatencyMs > 0L) "${res.averageLatencyMs} ms" else "NA", color = TextPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${res.extraMetricLabel}:", color = TextSecondary)
                            Text(res.extraMetricValue, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    AdaptIQPrimaryButton(
                        text = "Complete Activity & Return to Dashboard",
                        onClick = onActivityCompleted,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdaptIQOutlinedButton(
                        text = "Retry 30-Second Scored Trial",
                        onClick = {
                            startOfficialTrial(currentModeStr)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BorderSubtle,
                        textColor = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AdaptIQOutlinedButton(
                    text = "Select Another Game",
                    onClick = {
                        isGameRunning = false
                        isGamePaused = false
                        trialResults = null
                        accumulatedActiveMs = 0L
                        timeRemainingSeconds = 30
                        gameMode = GamingMode.SELECTION
                    },
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BorderSubtle,
                    textColor = TextSecondary
                )
            }
        }
    }
}

/**
 * Runner Obstacle Model
 */
enum class RunnerObstacleType { JUMP_HURDLE, SLIDE_ARCH, DODGE_ROADBLOCK }

data class RunnerObstacle(
    val obstacleId: Int,
    val lane: Int, // 0=Left, 1=Center, 2=Right
    val type: RunnerObstacleType,
    var depth: Float, // 0.0 (horizon) to 1.0 (player)
    val onsetElapsedRealtime: Long,
    var evaluated: Boolean = false
)

/**
 * 1. RUNNER GAME VIEW (Swipe-Based 3D Perspective Mini-Game)
 */
@Composable
private fun RunnerGameView(
    trialAttemptId: Int = 1,
    isGameRunning: Boolean,
    isGamePaused: Boolean = false,
    isPractice: Boolean,
    onStartPractice: () -> Unit,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit,
    onTrialFinished: (GamingTrialResults) -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var isJumping by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
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

    // Runner Game Loop
    LaunchedEffect(isGameRunning) {
        var lastSpawnTime = SystemClock.elapsedRealtime()
        while (isGameRunning) {
            if (isGamePaused) {
                delay(100L)
                continue
            }
            val now = SystemClock.elapsedRealtime()
            roadScrollOffset = (roadScrollOffset + 0.045f) % 1f

            // Spawn obstacles every ~1.8 seconds
            if (now - lastSpawnTime > 1800L) {
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

            // Move and evaluate obstacles
            val iterator = obstacles.listIterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.depth += 0.025f // Advance toward player

                // Collision evaluation zone: depth in [0.82, 0.98]
                if (obs.depth >= 0.82f && !obs.evaluated) {
                    obs.evaluated = true
                    obstaclesEncountered++

                    val inSameLane = (playerLane == obs.lane)
                    val expectedAction: String
                    val matched: Boolean

                    when (obs.type) {
                        RunnerObstacleType.JUMP_HURDLE -> {
                            expectedAction = "jump"
                            matched = inSameLane && isJumping
                        }
                        RunnerObstacleType.SLIDE_ARCH -> {
                            expectedAction = "roll"
                            matched = inSameLane && isSliding
                        }
                        RunnerObstacleType.DODGE_ROADBLOCK -> {
                            expectedAction = "swipe_dodge"
                            matched = !inSameLane // Avoided if player is not in roadblock lane
                        }
                    }

                    val qualifyingResponseTime = pendingReactionTime
                    val latency = if (qualifyingResponseTime > 0L && qualifyingResponseTime >= obs.onsetElapsedRealtime) {
                        (qualifyingResponseTime - obs.onsetElapsedRealtime).coerceIn(40L, 1600L)
                    } else if (matched) {
                        // Action occurred just in time
                        (now - obs.onsetElapsedRealtime).coerceIn(100L, 1600L)
                    } else {
                        -1L // No qualifying response before collision
                    }

                    if (latency > 0L) {
                        totalLatencySum += latency
                        latencyEventsCount++
                    }

                    val detectedStr = pendingDetectedAction ?: when {
                        isJumping -> "jump"
                        isSliding -> "roll"
                        inSameLane -> "none"
                        else -> "swipe_dodge"
                    }

                    val resultStr = if (matched) "successful" else "collision"
                    if (matched) {
                        obstaclesAvoided++
                        score += 25
                        statusText = "✓ Avoided (${obs.type.name})!"
                    } else {
                        collisionsCount++
                        score = (score - 10).coerceAtLeast(0)
                        collisionFlash = true
                        statusText = "💥 Collision with ${obs.type.name}!"
                    }

                    if (!isPractice) {
                        onLogEvent(
                            expectedAction,
                            detectedStr,
                            resultStr,
                            trialAttemptId,
                            latency,
                            "obstacleId=${obs.obstacleId},lane=${obs.lane},playerLane=$playerLane,type=${obs.type}"
                        )
                    }

                    pendingReactionTime = 0L
                    pendingDetectedAction = null
                } else if (obs.depth > 1.15f) {
                    iterator.remove()
                }
            }

            delay(30L)
        }

        // On game stopped (when 30s expires), calculate final summary
        if (!isGameRunning && obstaclesEncountered > 0) {
            val avgLat = if (latencyEventsCount > 0) totalLatencySum / latencyEventsCount else -1L
            onTrialFinished(
                GamingTrialResults(
                    gameMode = "Runner (Temple Run–Inspired)",
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

    LaunchedEffect(collisionFlash) {
        if (collisionFlash) {
            delay(300)
            collisionFlash = false
        }
    }

    LaunchedEffect(isJumping) {
        if (isJumping) {
            delay(500)
            isJumping = false
        }
    }

    LaunchedEffect(isSliding) {
        if (isSliding) {
            delay(500)
            isSliding = false
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
                statusText = "◀ Switched to Lane $playerLane"
            } else {
                statusText = "◀ Already at leftmost lane"
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
                statusText = "Switched to Lane $playerLane ▶"
            } else {
                statusText = "Already at rightmost lane ▶"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Perspective 3D Runner Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    BorderStroke(1.dp, if (collisionFlash) StatusError else BorderSubtle),
                    RoundedCornerShape(16.dp)
                )
                .background(if (collisionFlash) StatusErrorContainer else Color(0xFF090E1D))
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
                            onDragEnd = {
                                gestureHandled = false
                            },
                            onDragCancel = {
                                gestureHandled = false
                            },
                            onDrag = { _, dragAmount ->
                                if (!gestureHandled) {
                                    dragX += dragAmount.x
                                    dragY += dragAmount.y
                                    val absX = abs(dragX)
                                    val absY = abs(dragY)

                                    if (absX >= swipeThreshold || absY >= swipeThreshold) {
                                        // Strict dominant axis checking (>= 1.3x) to reject ambiguous diagonal gestures
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
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Horizon & Sky
                val horizonY = height * 0.35f
                val bottomY = height

                // Midnight Sci-Fi Horizon Sky
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF050914), Color(0xFF0B1426)),
                        startY = 0f,
                        endY = horizonY
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, horizonY)
                )

                // Neon horizon grid line
                drawLine(
                    color = AccentTeal.copy(alpha = 0.5f),
                    start = Offset(0f, horizonY),
                    end = Offset(width, horizonY),
                    strokeWidth = 2f
                )

                // Ground Terrain
                drawRect(
                    color = Color(0xFF0D172A),
                    topLeft = Offset(0f, horizonY),
                    size = Size(width, height - horizonY)
                )

                val bottomRoadWidth = width * 0.88f
                val horizonRoadWidth = width * 0.22f

                val leftBottomX = (width - bottomRoadWidth) / 2f
                val rightBottomX = leftBottomX + bottomRoadWidth
                val leftHorizonX = (width - horizonRoadWidth) / 2f
                val rightHorizonX = leftHorizonX + horizonRoadWidth

                // Running Track Surface (Deep Carbon)
                val trackPath = Path().apply {
                    moveTo(leftHorizonX, horizonY)
                    lineTo(rightHorizonX, horizonY)
                    lineTo(rightBottomX, bottomY)
                    lineTo(leftBottomX, bottomY)
                    close()
                }
                drawPath(trackPath, Color(0xFF131D33))

                // Outer Glowing Road Borders
                drawLine(
                    color = AccentTeal.copy(alpha = 0.85f),
                    start = Offset(leftHorizonX, horizonY),
                    end = Offset(leftBottomX, bottomY),
                    strokeWidth = 3f
                )
                drawLine(
                    color = AccentTeal.copy(alpha = 0.85f),
                    start = Offset(rightHorizonX, horizonY),
                    end = Offset(rightBottomX, bottomY),
                    strokeWidth = 3f
                )

                // Forward Speed Ticks on Track
                for (i in 0..4) {
                    val lineDepth = (i * 0.22f + roadScrollOffset * 0.22f) % 1.0f
                    val lineY = horizonY + (bottomY - horizonY) * lineDepth
                    val currentW = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * lineDepth
                    val leftX = (width - currentW) / 2f
                    drawLine(
                        color = AccentBlue.copy(alpha = 0.25f + lineDepth * 0.25f),
                        start = Offset(leftX, lineY),
                        end = Offset(leftX + currentW, lineY),
                        strokeWidth = 1.5f + lineDepth * 3.5f
                    )
                }

                // 3 Lanes Dividers with Glowing Cyan Tints
                for (laneDiv in 1..2) {
                    val topX = leftHorizonX + (horizonRoadWidth / 3f) * laneDiv
                    val botX = leftBottomX + (bottomRoadWidth / 3f) * laneDiv
                    drawLine(
                        color = AccentTeal.copy(alpha = 0.5f),
                        start = Offset(topX, horizonY),
                        end = Offset(botX, bottomY),
                        strokeWidth = 2f
                    )
                }

                // Approaching Obstacles
                for (obs in obstacles) {
                    val obsY = horizonY + (bottomY - horizonY) * obs.depth
                    val currentRoadW = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * obs.depth
                    val currentLeftX = (width - currentRoadW) / 2f
                    val laneW = currentRoadW / 3f
                    val obsCenterX = currentLeftX + laneW * obs.lane + (laneW / 2f)

                    val obsBaseW = laneW * 0.78f
                    val obsBaseH = 28f * (0.3f + obs.depth * 0.7f)

                    when (obs.type) {
                        RunnerObstacleType.JUMP_HURDLE -> {
                            // Glowing Jump Hurdle (Neon Coral Red)
                            drawRoundRect(
                                color = StatusError,
                                topLeft = Offset(obsCenterX - obsBaseW / 2f, obsY - obsBaseH / 2f),
                                size = Size(obsBaseW, obsBaseH * 0.75f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            // Bright horizontal energy core
                            drawLine(
                                color = Color.White,
                                start = Offset(obsCenterX - obsBaseW * 0.4f, obsY - obsBaseH * 0.12f),
                                end = Offset(obsCenterX + obsBaseW * 0.4f, obsY - obsBaseH * 0.12f),
                                strokeWidth = 3f
                            )
                        }
                        RunnerObstacleType.SLIDE_ARCH -> {
                            // High-Tech Overhead Laser Arch (Neon Amber/Gold)
                            val archHeight = obsBaseH * 1.65f
                            drawRoundRect(
                                color = StatusWarning,
                                topLeft = Offset(obsCenterX - obsBaseW / 2f, obsY - archHeight),
                                size = Size(obsBaseW, obsBaseH * 0.65f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            // Supporting side magnetic pylons
                            drawLine(
                                color = StatusWarning,
                                start = Offset(obsCenterX - obsBaseW / 2f, obsY - archHeight),
                                end = Offset(obsCenterX - obsBaseW / 2f, obsY),
                                strokeWidth = 4f
                            )
                            drawLine(
                                color = StatusWarning,
                                start = Offset(obsCenterX + obsBaseW / 2f, obsY - archHeight),
                                end = Offset(obsCenterX + obsBaseW / 2f, obsY),
                                strokeWidth = 4f
                            )
                        }
                        RunnerObstacleType.DODGE_ROADBLOCK -> {
                            // High-Contrast Cyber Roadblock (Vibrant Orange with hazard stripes)
                            drawRoundRect(
                                color = Color(0xFFF97316),
                                topLeft = Offset(obsCenterX - obsBaseW / 2f, obsY - obsBaseH * 0.95f),
                                size = Size(obsBaseW, obsBaseH * 1.45f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                            // Hazard dark stripes
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(obsCenterX - obsBaseW * 0.35f, obsY - obsBaseH * 0.7f),
                                size = Size(obsBaseW * 0.7f, obsBaseH * 0.5f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // Player Runner Avatar (High-Tech Research Character)
                val playerLaneW = bottomRoadWidth / 3f
                val playerCenterX = leftBottomX + playerLaneW * playerLane + (playerLaneW / 2f)
                val playerBaseY = bottomY - 50f
                val playerRenderY = if (isJumping) playerBaseY - 60f else playerBaseY
                val avatarRadius = if (isSliding) 14f else 22f

                // Runner Ground Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = if (isJumping) 14f else 22f,
                    center = Offset(playerCenterX, playerBaseY + 12f)
                )

                // Runner Torso & Head (Teal Sci-Fi Suit)
                drawCircle(
                    color = if (collisionFlash) StatusError else AccentTeal,
                    radius = avatarRadius,
                    center = Offset(playerCenterX, playerRenderY)
                )

                // High-Tech Cyber Visor / Core
                drawCircle(
                    color = Color.White,
                    radius = avatarRadius * 0.45f,
                    center = Offset(playerCenterX, playerRenderY - avatarRadius * 0.25f)
                )
            }

            // Minimalist Floating Glass HUD Pill (Top Center)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BackgroundDark.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = statusText,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Score: $score",
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Avoided: $obstaclesAvoided",
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Collisions: $collisionsCount",
                            color = if (collisionsCount > 0) StatusError else StatusSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // In-Canvas Frosted Pause Overlay
            if (isGamePaused) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "⏸️ TRIAL PAUSED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Active trial countdown is frozen.\nTap 'Resume' in header to continue gameplay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Gesture Guide HUD (Pure Touch Swipe Controls: No On-Screen Movement Buttons)
        AdaptIQSurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = SurfaceElevatedVariant,
            borderColor = BorderSubtle
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◀ Left", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("Right ▶", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("▲ Jump", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusError)
                Text("▼ Slide", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusWarning)
            }
        }

        if (isPractice && !isGameRunning) {
            AdaptIQPrimaryButton(
                text = "Start Practice Run",
                onClick = onStartPractice,
                modifier = Modifier.fillMaxWidth()
            )
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
    val onsetElapsedRealtime: Long,
    var evaluated: Boolean = false
)

/**
 * 2. LANDSCAPE DRIVING GAME VIEW (Dr. Driving–Inspired Mini-Game)
 * Fully interactive landscape driving experience featuring:
 * - Landscape-locked viewport with automatic lifecycle cleanup and portrait restoration
 * - Behind-the-car 2.5D perspective road with roadside scenery, animated lane markings & horizon
 * - Draggable, continuously rotating steering wheel control on lower-left
 * - Dedicated gas (accelerator) and brake pedal controls on lower-right
 * - 30-second monotonic active unpaused countdown timer
 * - Collision & overtake avoidance research logging with parent study Trial_ID
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
    // 1. Enforce Landscape Orientation during driving gameplay and restore on exit
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
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

            // Acceleration & Braking physics
            if (isAccelerating) {
                speedKmH = (speedKmH + 2).coerceAtMost(120)
            } else if (isBraking) {
                speedKmH = (speedKmH - 3).coerceAtLeast(20)
            } else {
                if (speedKmH > 60) speedKmH = (speedKmH - 1).coerceAtLeast(60)
                else if (speedKmH < 60) speedKmH = (speedKmH + 1).coerceAtMost(60)
            }

            // Steering dynamics: wheel rotation drives car lateral movement
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

            // Traffic generation (approaching in 1 of 3 lanes)
            if (now - lastTrafficSpawn > 2200L) {
                lastTrafficSpawn = now
                val lane = Random.nextInt(0, 3)
                val carColor = when (Random.nextInt(0, 4)) {
                    0 -> Color(0xFF3B82F6) // Blue sedan
                    1 -> Color(0xFFEAB308) // Yellow taxi
                    2 -> Color(0xFF10B981) // Green delivery
                    else -> Color(0xFFA855F7) // Purple crossover
                }
                trafficCars.add(
                    DrivingTrafficVehicle(
                        vehicleId = trafficIdCounter++,
                        lane = lane,
                        depth = 0.0f,
                        color = carColor,
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
                        // Crash into traffic car! No qualifying avoidance maneuver executed
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
                                -1L, // Unmeasured/NA latency for collisions under task rules
                                "encounter=${traffic.vehicleId},trafficLane=${traffic.lane},playerLane=$playerLane,speed=$speedKmH"
                            )
                        }
                    } else {
                        // Avoided traffic successfully!
                        trafficAvoided++
                        drivingScore += 40

                        val qualifyingSteerTime = pendingSteerTime
                        val steerLatency = if (qualifyingSteerTime > 0L && qualifyingSteerTime >= traffic.onsetElapsedRealtime) {
                            qualifyingSteerTime - traffic.onsetElapsedRealtime
                        } else {
                            -1L // Pre-positioned or no qualifying maneuver needed
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

        // On 30-second official trial expired, emit final summary
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
            delay(300)
            collisionFlash = false
        }
    }

    // Full-Screen Landscape Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 2.5D Perspective Road Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val horizonY = height * 0.38f
            val bottomY = height

            // Sky & Twilight Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF020617), Color(0xFF0369A1)),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset(0f, 0f),
                size = Size(width, horizonY)
            )

            // Distant skyline / mountains silhouette on horizon
            for (i in 0..12) {
                val mX = (width / 12f) * i
                val mW = width / 10f
                val mH = 14f + sin(i.toFloat() * 1.5f) * 10f
                drawRoundRect(
                    color = Color(0xFF0C4A6E),
                    topLeft = Offset(mX - mW / 2f, horizonY - mH),
                    size = Size(mW, mH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            // Roadside Terrain Ground
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF022C22)),
                    startY = horizonY,
                    endY = bottomY
                ),
                topLeft = Offset(0f, horizonY),
                size = Size(width, bottomY - horizonY)
            )

            // Road Perspective Geometry
            val roadBottomW = width * 0.84f
            val roadHorizonW = width * 0.22f
            val horizonCenter = width / 2f + roadCurveOffset * 0.35f
            val bottomCenter = width / 2f + roadCurveOffset

            val leftBottomX = bottomCenter - roadBottomW / 2f
            val rightBottomX = bottomCenter + roadBottomW / 2f
            val leftHorizonX = horizonCenter - roadHorizonW / 2f
            val rightHorizonX = horizonCenter + roadHorizonW / 2f

            // Road Surface
            val roadPath = Path().apply {
                moveTo(leftHorizonX, horizonY)
                lineTo(rightHorizonX, horizonY)
                lineTo(rightBottomX, bottomY)
                lineTo(leftBottomX, bottomY)
                close()
            }
            drawPath(roadPath, Color(0xFF1E293B))

            // Roadside Curbs (Alternating red/white curbs on left & right road borders)
            val curbSegments = 10
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
                    val curbWidth1 = 6f + depth1 * 14f
                    val curbWidth2 = 6f + depth2 * 14f

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

            // Three Road Lanes with Animated Dashed Lines
            for (laneDiv in 1..2) {
                val topX = leftHorizonX + (roadHorizonW / 3f) * laneDiv
                val botX = leftBottomX + (roadBottomW / 3f) * laneDiv

                for (dashIdx in 0..6) {
                    val depth = (dashIdx * 0.16f + roadScrollOffset * 0.16f) % 1.0f
                    val dashY = horizonY + (bottomY - horizonY) * depth
                    val dashX = topX + (botX - topX) * depth
                    val dashH = 8f + depth * 22f
                    val dashW = 2.5f + depth * 4.5f

                    drawRoundRect(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(dashX - dashW / 2f, dashY),
                        size = Size(dashW, dashH),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            // Approaching Traffic Vehicles
            for (traffic in trafficCars) {
                val obsY = horizonY + (bottomY - horizonY) * traffic.depth
                val currentRoadW = roadHorizonW + (roadBottomW - roadHorizonW) * traffic.depth
                val currentCenter = horizonCenter + (bottomCenter - horizonCenter) * traffic.depth
                val currentLeftX = currentCenter - currentRoadW / 2f
                val laneW = currentRoadW / 3f
                val trafficCenterX = currentLeftX + laneW * traffic.lane + (laneW / 2f)

                val carW = laneW * 0.65f
                val carH = carW * 0.60f

                // Traffic shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(trafficCenterX - carW / 2f, obsY - carH / 2f + 4f),
                    size = Size(carW, carH),
                    cornerRadius = CornerRadius(5f, 5f)
                )

                // Traffic car body
                drawRoundRect(
                    color = traffic.color,
                    topLeft = Offset(trafficCenterX - carW / 2f, obsY - carH / 2f),
                    size = Size(carW, carH),
                    cornerRadius = CornerRadius(7f, 7f)
                )

                // Rear windshield & roof
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(trafficCenterX - carW * 0.36f, obsY - carH * 0.32f),
                    size = Size(carW * 0.72f, carH * 0.35f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Dual red taillights
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = carH * 0.12f,
                    center = Offset(trafficCenterX - carW * 0.36f, obsY + carH * 0.28f)
                )
                drawCircle(
                    color = Color(0xFFEF4444),
                    radius = carH * 0.12f,
                    center = Offset(trafficCenterX + carW * 0.36f, obsY + carH * 0.28f)
                )
            }

            // Player Car Model (Third-Person View from Behind)
            val playerLaneW = roadBottomW / 3f
            val playerCarCenterX = bottomCenter + (carLateralOffset / 100f) * (playerLaneW * 1.15f)
            val playerCarY = bottomY - 56f
            val playerCarW = 82f
            val playerCarH = 44f

            // Dynamic roll/banking angle when turning
            val rollAngle = (wheelRotationAngle / 110f) * 12f

            rotate(rollAngle, pivot = Offset(playerCarCenterX, playerCarY)) {
                // Car shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    topLeft = Offset(playerCarCenterX - playerCarW / 2f - 4f, playerCarY - playerCarH / 2f + 8f),
                    size = Size(playerCarW + 8f, playerCarH),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Wide rear tires
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(playerCarCenterX - playerCarW / 2f - 5f, playerCarY + playerCarH * 0.05f),
                    size = Size(12f, 18f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(playerCarCenterX + playerCarW / 2f - 7f, playerCarY + playerCarH * 0.05f),
                    size = Size(12f, 18f),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // Player car body (Sports Coupe)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = if (collisionFlash) listOf(Color(0xFFEF4444), Color(0xFF7F1D1D)) else listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                    ),
                    topLeft = Offset(playerCarCenterX - playerCarW / 2f, playerCarY - playerCarH / 2f),
                    size = Size(playerCarW, playerCarH),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Aerodynamic cabin roof & rear windshield
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0F172A))
                    ),
                    topLeft = Offset(playerCarCenterX - playerCarW * 0.32f, playerCarY - playerCarH * 0.38f),
                    size = Size(playerCarW * 0.64f, playerCarH * 0.40f),
                    cornerRadius = CornerRadius(5f, 5f)
                )

                // Dual High-Intensity LED Taillights (with extra bright glow when braking!)
                val tailLightColor = if (isBraking) Color(0xFFFFFFFF) else Color(0xFFFCA5A5)
                val tailGlowColor = if (isBraking) Color(0xFFEF4444) else Color(0xFFDC2626)
                drawCircle(color = tailGlowColor, radius = if (isBraking) 10f else 6f, center = Offset(playerCarCenterX - playerCarW * 0.38f, playerCarY + playerCarH * 0.22f))
                drawCircle(color = tailLightColor, radius = if (isBraking) 5.5f else 3.5f, center = Offset(playerCarCenterX - playerCarW * 0.38f, playerCarY + playerCarH * 0.22f))

                drawCircle(color = tailGlowColor, radius = if (isBraking) 10f else 6f, center = Offset(playerCarCenterX + playerCarW * 0.38f, playerCarY + playerCarH * 0.22f))
                drawCircle(color = tailLightColor, radius = if (isBraking) 5.5f else 3.5f, center = Offset(playerCarCenterX + playerCarW * 0.38f, playerCarY + playerCarH * 0.22f))

                // Dual chrome exhaust pipes
                drawCircle(color = Color(0xFF94A3B8), radius = 3.5f, center = Offset(playerCarCenterX - playerCarW * 0.20f, playerCarY + playerCarH * 0.45f))
                drawCircle(color = Color(0xFF94A3B8), radius = 3.5f, center = Offset(playerCarCenterX + playerCarW * 0.20f, playerCarY + playerCarH * 0.45f))

                // License plate "ADAPT-IQ"
                drawRoundRect(
                    color = Color(0xFFF8FAFC),
                    topLeft = Offset(playerCarCenterX - 14f, playerCarY + playerCarH * 0.20f),
                    size = Size(28f, 10f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // Full-screen Collision Impact Vignette
            if (collisionFlash) {
                drawRect(
                    color = Color(0xFFDC2626).copy(alpha = 0.45f),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, height)
                )
            }
        }

        // Floating Top Landscape HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button & Mode Badge
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Text("✕ Exit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPractice) Color(0xFFCA8A04) else Color(0xFF2563EB)
                ) {
                    Text(
                        text = if (isPractice) "PRACTICE DRIVE" else "OFFICIAL 30s TRIAL",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Center: Speedometer, Monotonic Timer & Score
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "$speedKmH km/h",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (speedKmH > 90) Color(0xFFF87171) else Color(0xFF4ADE80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (isPractice && !isGameRunning) "PRACTICE" else "⏱️ 00:%02d".format(timeRemainingSeconds),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Score: $drivingScore • ${distanceTraveledMeters.toInt()}m",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right: Traffic Stats & Pause/Resume
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "✓ $trafficAvoided  💥 $trafficCollisions",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isGameRunning) {
                    Button(
                        onClick = onPauseResume,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isGamePaused) Color(0xFF16A34A) else Color(0xFF334155))
                    ) {
                        Text(if (isGamePaused) "▶ Resume" else "⏸ Pause", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Floating Dynamic Guidance / Steering Status
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.55f)
        ) {
            Text(
                text = steeringStatus,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                color = if (normalizedDeviation > 1.0f) Color(0xFFF87171) else Color(0xFF38BDF8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Lower-Left Area: Interactive Draggable Steering Wheel Control
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 18.dp)
                .size(152.dp)
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
                        onDragEnd = {
                            isDraggingWheel = false
                        },
                        onDragCancel = {
                            isDraggingWheel = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val curAngle = Math.toDegrees(atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())).toFloat()
                            var deltaAngle = curAngle - dragStartAngle
                            while (deltaAngle > 180f) deltaAngle -= 360f
                            while (deltaAngle < -180f) deltaAngle += 360f

                            // Combine circular rotation angle and horizontal drag amount for maximum responsiveness
                            val horizontalAssist = dragAmount.x * 0.50f
                            val newAngle = (dragStartWheelAngle + deltaAngle + horizontalAssist).coerceIn(-110f, 110f)
                            wheelRotationAngle = newAngle

                            // Research event logging on significant steering input
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

                // Drop shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = rOuter,
                    center = Offset(cx, cy + 6f)
                )

                // Rotate the entire wheel assembly in response to drag
                rotate(wheelRotationAngle, pivot = Offset(cx, cy)) {
                    // Outer Steering Rim
                    val rimStroke = rOuter * 0.22f
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = rOuter - rimStroke / 2f,
                        style = Stroke(width = rimStroke)
                    )

                    // Textured grip notches at 9 & 3 o'clock
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = 160f,
                        sweepAngle = 40f,
                        useCenter = false,
                        style = Stroke(width = rimStroke * 1.15f),
                        topLeft = Offset(cx - rOuter + rimStroke / 2f, cy - rOuter + rimStroke / 2f),
                        size = Size((rOuter - rimStroke / 2f) * 2f, (rOuter - rimStroke / 2f) * 2f)
                    )
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = -20f,
                        sweepAngle = 40f,
                        useCenter = false,
                        style = Stroke(width = rimStroke * 1.15f),
                        topLeft = Offset(cx - rOuter + rimStroke / 2f, cy - rOuter + rimStroke / 2f),
                        size = Size((rOuter - rimStroke / 2f) * 2f, (rOuter - rimStroke / 2f) * 2f)
                    )

                    // Racing teal center alignment strip at 12 o'clock
                    drawArc(
                        color = AccentTeal,
                        startAngle = 262f,
                        sweepAngle = 16f,
                        useCenter = false,
                        style = Stroke(width = rimStroke * 1.25f),
                        topLeft = Offset(cx - rOuter + rimStroke / 2f, cy - rOuter + rimStroke / 2f),
                        size = Size((rOuter - rimStroke / 2f) * 2f, (rOuter - rimStroke / 2f) * 2f)
                    )

                    // 3 Structural Spokes
                    val spokeWidth = 14f
                    val hubRadius = rOuter * 0.38f
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(cx - hubRadius, cy),
                        end = Offset(cx - rOuter + rimStroke, cy),
                        strokeWidth = spokeWidth
                    )
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(cx + hubRadius, cy),
                        end = Offset(cx + rOuter - rimStroke, cy),
                        strokeWidth = spokeWidth
                    )
                    drawLine(
                        color = Color(0xFF475569),
                        start = Offset(cx, cy + hubRadius),
                        end = Offset(cx, cy + rOuter - rimStroke),
                        strokeWidth = spokeWidth
                    )

                    // Center Hub
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = hubRadius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color(0xFF64748B),
                        radius = hubRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3f)
                    )
                    // Hub horn emblem
                    drawCircle(
                        color = AccentTeal,
                        radius = hubRadius * 0.40f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // Lower-Right Area: Gas & Brake Pedal Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Brake Pedal
            Box(
                modifier = Modifier
                    .width(66.dp)
                    .height(82.dp)
                    .background(
                        color = if (isBraking) StatusError else Color(0xFF7F1D1D),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isBraking) Color(0xFFFCA5A5) else StatusError,
                        shape = RoundedCornerShape(10.dp)
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
                                .width(42.dp)
                                .height(3.dp)
                                .background(Color(0xFFB91C1C).copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text("BRAKE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Gas Pedal (taller)
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(112.dp)
                    .background(
                        color = if (isAccelerating) StatusSuccess else Color(0xFF14532D),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isAccelerating) Color(0xFF86EFAC) else StatusSuccess,
                        shape = RoundedCornerShape(10.dp)
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
                                .width(36.dp)
                                .height(3.dp)
                                .background(Color(0xFF15803D).copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text("GAS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Overlay: Practice Start & Official Trial Launch Card
        if (isPractice && !isGameRunning && trialResults == null) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(420.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = SurfaceElevatedVariant,
                borderColor = BorderSubtle
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

        // Overlay: Pause Dialog
        if (isGamePaused) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(360.dp),
                shape = RoundedCornerShape(16.dp),
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

        // Overlay: Scored Trial Completed Results Dialog
        if (trialResults != null) {
            val res = trialResults
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(460.dp),
                shape = RoundedCornerShape(16.dp),
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
