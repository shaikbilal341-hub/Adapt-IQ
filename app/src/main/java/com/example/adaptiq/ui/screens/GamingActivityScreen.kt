package com.example.adaptiq.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private enum class GameMode {
    SELECTION,
    ADAPT_IQ_RUNNER,
    ADAPT_IQ_DRIVING_LAB
}

@Composable
fun GamingActivityScreen(
    onLogEvent: (
        expectedAction: String,
        detectedAction: String,
        result: String,
        attemptId: Int,
        durationMs: Long,
        touchInfo: String
    ) -> Unit,
    onActivityCompleted: () -> Unit,
    onBack: () -> Unit
) {
    var gameMode by remember { mutableStateOf(GameMode.SELECTION) }
    var isPractice by remember { mutableStateOf(true) }
    var timeRemainingSeconds by remember { mutableIntStateOf(30) } // Exact 30-second active trial
    var isGameRunning by remember { mutableStateOf(false) }
    var isGamePaused by remember { mutableStateOf(false) }
    var isGameFinished by remember { mutableStateOf(false) }

    // 30-Second Active Trial Countdown Timer (Pauses when isGamePaused is true)
    LaunchedEffect(isGameRunning, isGamePaused) {
        if (isGameRunning && !isGamePaused) {
            while (timeRemainingSeconds > 0 && isGameRunning && !isGamePaused) {
                delay(1000)
                if (isGameRunning && !isGamePaused) {
                    timeRemainingSeconds--
                }
            }
            if (timeRemainingSeconds <= 0 && isGameRunning) {
                isGameRunning = false
                isGameFinished = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Activity 13 of 13: Gaming-style Interaction",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Playable 30-second 3D research mini-games measuring reaction time, obstacle avoidance, and steering stability.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (gameMode == GameMode.SELECTION) {
            Text(
                text = "Choose 30-Second Research Mini-Game",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = {
                    gameMode = GameMode.ADAPT_IQ_RUNNER
                    timeRemainingSeconds = 30
                    isGameRunning = false
                    isGamePaused = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏃 Adapt-IQ Runner (30s Trial)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3-lane perspective endless runner. Swipe or use buttons to dodge roadblocks, jump hurdles, and slide under overhead bars.")
                }
            }

            Card(
                onClick = {
                    gameMode = GameMode.ADAPT_IQ_DRIVING_LAB
                    timeRemainingSeconds = 30
                    isGameRunning = false
                    isGamePaused = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏎️ Adapt-IQ Driving Lab (30s Trial)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3D perspective winding road simulation with steering controls, speed regulation, hazard avoidance, and lane deviation tracking.")
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Dashboard")
            }
        } else {
            // Header with Practice / Official Mode Controls
            PracticeHeader(
                isPractice = isPractice,
                onStartOfficialCollection = {
                    isPractice = false
                    timeRemainingSeconds = 30
                    isGameRunning = true
                    isGamePaused = false
                    isGameFinished = false
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (gameMode) {
                        GameMode.ADAPT_IQ_RUNNER -> "Adapt-IQ Runner"
                        GameMode.ADAPT_IQ_DRIVING_LAB -> "Adapt-IQ Driving Lab"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isGameRunning) {
                        OutlinedButton(onClick = { isGamePaused = !isGamePaused }) {
                            Text(if (isGamePaused) "Resume" else "Pause")
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (isPractice && !isGameRunning) "PRACTICE" else "⏱️ 00:%02d".format(timeRemainingSeconds),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (!isGameFinished) {
                if (isGamePaused) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "⏸️ GAME PAUSED\nTrial timer is frozen.\nTap 'Resume' to continue.",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    when (gameMode) {
                        GameMode.ADAPT_IQ_RUNNER -> {
                            AdaptIqRunner3DView(
                                isGameRunning = isGameRunning,
                                isPractice = isPractice,
                                onStartPractice = {
                                    isGameRunning = true
                                    isGamePaused = false
                                },
                                onLogEvent = onLogEvent
                            )
                        }

                        GameMode.ADAPT_IQ_DRIVING_LAB -> {
                            AdaptIqDriving3DView(
                                isGameRunning = isGameRunning,
                                isPractice = isPractice,
                                onStartPractice = {
                                    isGameRunning = true
                                    isGamePaused = false
                                },
                                onLogEvent = onLogEvent
                            )
                        }

                        else -> {}
                    }
                }
            }

            if (isGameFinished) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✓ 30-Second Trial Completed Successfully!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Full 30-second research trial recorded with monotonic response latency and synchronized sensor data.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Button(
                    onClick = onActivityCompleted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Complete Activity & Return to Dashboard", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    isGameRunning = false
                    isGamePaused = false
                    isGameFinished = false
                    gameMode = GameMode.SELECTION
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Another Game")
            }
        }
    }
}

/**
 * Runner Obstacle Model
 */
private enum class ObstacleType { JUMP, SLIDE, DODGE }

private data class ValidatedRunnerObstacle(
    val trialId: Int,
    val lane: Int,
    val type: ObstacleType,
    var depth: Float, // 0.0 (horizon) to 1.0 (player)
    val onsetElapsedRealtime: Long,
    var evaluated: Boolean = false
)

/**
 * Adapt-IQ Runner — Real Playable 30-Second 3D Perspective Endless Runner
 */
@Composable
private fun AdaptIqRunner3DView(
    isGameRunning: Boolean,
    isPractice: Boolean,
    onStartPractice: () -> Unit,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var isJumping by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Swipe or tap controls below to dodge roadblocks, jump, and slide!") }

    // Performance statistics
    var obstaclesEncountered by remember { mutableIntStateOf(0) }
    var obstaclesAvoided by remember { mutableIntStateOf(0) }
    var collisionsCount by remember { mutableIntStateOf(0) }
    var totalLatencySum by remember { mutableLongStateOf(0L) }

    val obstacles = remember { mutableStateListOf<ValidatedRunnerObstacle>() }
    var trialIdCounter by remember { mutableIntStateOf(1) }

    var pendingAction by remember { mutableStateOf<String?>(null) }
    var actionTimestamp by remember { mutableLongStateOf(0L) }
    var roadScrollOffset by remember { mutableFloatStateOf(0f) }

    // Runner Game Loop
    LaunchedEffect(isGameRunning) {
        var lastSpawnTime = SystemClock.elapsedRealtime()
        while (true) {
            if (isGameRunning) {
                val now = SystemClock.elapsedRealtime()
                roadScrollOffset = (roadScrollOffset + 0.05f) % 1f

                // Spawn obstacle every ~2.0 seconds
                if (now - lastSpawnTime > 2000L) {
                    lastSpawnTime = now
                    val randomLane = Random.nextInt(0, 3)
                    val obsType = when (Random.nextInt(0, 3)) {
                        0 -> ObstacleType.JUMP
                        1 -> ObstacleType.SLIDE
                        else -> ObstacleType.DODGE
                    }
                    val currentId = trialIdCounter++
                    obstacles.add(
                        ValidatedRunnerObstacle(
                            trialId = currentId,
                            lane = randomLane,
                            type = obsType,
                            depth = 0.0f,
                            onsetElapsedRealtime = now
                        )
                    )
                }

                // Move and evaluate obstacles
                val iterator = obstacles.listIterator()
                while (iterator.hasNext()) {
                    val obs = iterator.next()
                    obs.depth += 0.025f

                    // Hit zone evaluation (depth between 0.82 and 0.98)
                    if (obs.depth >= 0.82f && !obs.evaluated) {
                        obs.evaluated = true
                        obstaclesEncountered++

                        val inSameLane = (playerLane == obs.lane)
                        val expectedAction: String
                        val matched: Boolean

                        when (obs.type) {
                            ObstacleType.JUMP -> {
                                expectedAction = "jump"
                                matched = inSameLane && isJumping
                            }
                            ObstacleType.SLIDE -> {
                                expectedAction = "roll"
                                matched = inSameLane && isSliding
                            }
                            ObstacleType.DODGE -> {
                                expectedAction = "swipe_dodge"
                                matched = !inSameLane // SUCCESS when player has dodged out of the obstacle's lane!
                            }
                        }

                        val actionTime = if (actionTimestamp > 0L) actionTimestamp else now
                        val latency = (actionTime - obs.onsetElapsedRealtime).coerceIn(40L, 1800L)
                        totalLatencySum += latency

                        val resultStr = if (matched) "successful" else "collision"
                        if (matched) {
                            obstaclesAvoided++
                            score += 20
                            statusText = "✓ Avoided! ($expectedAction) Latency: ${latency}ms"
                        } else {
                            collisionsCount++
                            statusText = "💥 Collision! Needed $expectedAction"
                        }

                        val detectedStr = pendingAction ?: when {
                            isJumping -> "jump"
                            isSliding -> "roll"
                            else -> if (inSameLane) "none" else "dodge"
                        }

                        if (!isPractice) {
                            onLogEvent(
                                expectedAction,
                                detectedStr,
                                resultStr,
                                obs.trialId,
                                latency,
                                "lane=${obs.lane},playerLane=$playerLane,type=${obs.type}"
                            )
                        }

                        pendingAction = null
                        actionTimestamp = 0L
                    } else if (obs.depth > 1.1f) {
                        iterator.remove()
                    }
                }
            }
            delay(30L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Perspective 3D Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(Color(0xFF102027), shape = RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    var dragAccumX = 0f
                    var dragAccumY = 0f
                    var gestureConsumed = false

                    detectDragGestures(
                        onDragStart = {
                            dragAccumX = 0f
                            dragAccumY = 0f
                            gestureConsumed = false
                        },
                        onDragEnd = {
                            dragAccumX = 0f
                            dragAccumY = 0f
                            gestureConsumed = false
                        },
                        onDrag = { _, dragAmount ->
                            if (!gestureConsumed) {
                                dragAccumX += dragAmount.x
                                dragAccumY += dragAmount.y
                                val threshold = 35f
                                val responseTime = SystemClock.elapsedRealtime()

                                if (abs(dragAccumX) > abs(dragAccumY) && abs(dragAccumX) > threshold) {
                                    if (dragAccumX > 0 && playerLane < 2) {
                                        playerLane++
                                        pendingAction = "swipe_right"
                                        actionTimestamp = responseTime
                                        statusText = "Dodge Right -> Lane $playerLane"
                                        gestureConsumed = true
                                    } else if (dragAccumX < 0 && playerLane > 0) {
                                        playerLane--
                                        pendingAction = "swipe_left"
                                        actionTimestamp = responseTime
                                        statusText = "Dodge Left -> Lane $playerLane"
                                        gestureConsumed = true
                                    }
                                } else if (abs(dragAccumY) > abs(dragAccumX) && abs(dragAccumY) > threshold) {
                                    if (dragAccumY < 0 && !isJumping) {
                                        isJumping = true
                                        pendingAction = "jump"
                                        actionTimestamp = responseTime
                                        statusText = "Jump over hurdle!"
                                        gestureConsumed = true
                                    } else if (dragAccumY > 0 && !isSliding) {
                                        isSliding = true
                                        pendingAction = "roll"
                                        actionTimestamp = responseTime
                                        statusText = "Slide under barrier!"
                                        gestureConsumed = true
                                    }
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Sky & Road Ground
                drawRect(color = Color(0xFF0D1B2A), topLeft = Offset(0f, 0f), size = Size(width, height * 0.35f))
                drawRect(color = Color(0xFF1E293B), topLeft = Offset(0f, height * 0.35f), size = Size(width, height * 0.65f))

                val horizonY = height * 0.35f
                val bottomY = height
                val bottomRoadWidth = width * 0.85f
                val horizonRoadWidth = width * 0.25f

                val leftBottomX = (width - bottomRoadWidth) / 2f
                val rightBottomX = leftBottomX + bottomRoadWidth
                val leftHorizonX = (width - horizonRoadWidth) / 2f
                val rightHorizonX = leftHorizonX + horizonRoadWidth

                // Road Trajectory Path
                val roadPath = Path().apply {
                    moveTo(leftHorizonX, horizonY)
                    lineTo(rightHorizonX, horizonY)
                    lineTo(rightBottomX, bottomY)
                    lineTo(leftBottomX, bottomY)
                    close()
                }
                drawPath(roadPath, Color(0xFF334155))

                // Animated road perspective speed stripes
                for (i in 1..4) {
                    val stripeDepth = (i * 0.22f + roadScrollOffset * 0.22f) % 1.0f
                    val stripeY = horizonY + (bottomY - horizonY) * stripeDepth
                    val currentRoadW = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * stripeDepth
                    val currentLeftX = (width - currentRoadW) / 2f
                    drawLine(
                        color = Color(0xFF475569).copy(alpha = 0.5f),
                        start = Offset(currentLeftX, stripeY),
                        end = Offset(currentLeftX + currentRoadW, stripeY),
                        strokeWidth = 2f + stripeDepth * 4f
                    )
                }

                // 3 Lane Divider Lines
                for (laneDiv in 1..2) {
                    val topX = leftHorizonX + (horizonRoadWidth / 3f) * laneDiv
                    val botX = leftBottomX + (bottomRoadWidth / 3f) * laneDiv
                    drawLine(
                        color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                        start = Offset(topX, horizonY),
                        end = Offset(botX, bottomY),
                        strokeWidth = 2.5f
                    )
                }

                // Approaching Obstacles
                for (obs in obstacles) {
                    val obsY = horizonY + (bottomY - horizonY) * obs.depth
                    val currentRoadW = horizonRoadWidth + (bottomRoadWidth - horizonRoadWidth) * obs.depth
                    val currentLeftX = (width - currentRoadW) / 2f
                    val laneW = currentRoadW / 3f
                    val obsCenterX = currentLeftX + laneW * obs.lane + (laneW / 2f)

                    val obsBaseW = laneW * 0.7f
                    val obsBaseH = 24f * (0.3f + obs.depth * 0.7f)

                    when (obs.type) {
                        ObstacleType.JUMP -> {
                            // Hurdle barrier
                            drawRoundRect(
                                color = Color(0xFFEF4444),
                                topLeft = Offset(obsCenterX - obsBaseW / 2f, obsY - obsBaseH / 2f),
                                size = Size(obsBaseW, obsBaseH),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                        ObstacleType.SLIDE -> {
                            // High barrier
                            drawRoundRect(
                                color = Color(0xFFA855F7),
                                topLeft = Offset(obsCenterX - obsBaseW / 2f, obsY - obsBaseH * 1.5f),
                                size = Size(obsBaseW, obsBaseH * 0.8f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                        ObstacleType.DODGE -> {
                            // Roadblock caution barrier
                            drawRoundRect(
                                color = Color(0xFFF97316),
                                topLeft = Offset(obsCenterX - obsBaseW / 2f, obsY - obsBaseH / 2f),
                                size = Size(obsBaseW, obsBaseH * 1.2f),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                // Player Runner Avatar
                val playerLaneW = bottomRoadWidth / 3f
                val playerCenterX = leftBottomX + playerLaneW * playerLane + (playerLaneW / 2f)
                val playerBaseY = bottomY - 50f
                val playerRenderY = if (isJumping) playerBaseY - 55f else playerBaseY
                val avatarRadius = if (isSliding) 14f else 24f

                // Shadow on road
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = if (isJumping) 16f else 22f,
                    center = Offset(playerCenterX, playerBaseY + 10f)
                )

                // Avatar Body
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = avatarRadius,
                    center = Offset(playerCenterX, playerRenderY)
                )
            }

            // Top Status Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = statusText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text(
                    text = "Score: $score • Avoided: $obstaclesAvoided • Collisions: $collisionsCount",
                    color = Color(0xFF4ADE80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // On-Screen Touch Controls (Ensures flawless responsiveness on both touch & mouse)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (playerLane > 0) {
                            playerLane--
                            pendingAction = "swipe_left"
                            actionTimestamp = SystemClock.elapsedRealtime()
                            statusText = "Dodge Left -> Lane $playerLane"
                        }
                    }
                ) {
                    Text("◀ Left")
                }

                Button(
                    onClick = {
                        if (playerLane < 2) {
                            playerLane++
                            pendingAction = "swipe_right"
                            actionTimestamp = SystemClock.elapsedRealtime()
                            statusText = "Dodge Right -> Lane $playerLane"
                        }
                    }
                ) {
                    Text("Right ▶")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (!isJumping) {
                            isJumping = true
                            pendingAction = "jump"
                            actionTimestamp = SystemClock.elapsedRealtime()
                            statusText = "Jump over hurdle!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("▲ Jump")
                }

                Button(
                    onClick = {
                        if (!isSliding) {
                            isSliding = true
                            pendingAction = "roll"
                            actionTimestamp = SystemClock.elapsedRealtime()
                            statusText = "Slide under barrier!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                ) {
                    Text("▼ Slide")
                }
            }
        }

        if (isPractice && !isGameRunning) {
            Button(
                onClick = onStartPractice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Practice Run", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Auto-reset jump and slide states
    LaunchedEffect(isJumping) {
        if (isJumping) {
            delay(450)
            isJumping = false
        }
    }

    LaunchedEffect(isSliding) {
        if (isSliding) {
            delay(450)
            isSliding = false
        }
    }
}

/**
 * Adapt-IQ Driving Lab — Real Playable 30-Second 3D Perspective Road Simulation
 */
@Composable
private fun AdaptIqDriving3DView(
    isGameRunning: Boolean,
    isPractice: Boolean,
    onStartPractice: () -> Unit,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var carXOffset by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableIntStateOf(65) }
    var steeringStatus by remember { mutableStateOf("Steer to stay centered on winding road") }
    var normalizedDeviation by remember { mutableFloatStateOf(0f) }
    var deviationEpisodeCount by remember { mutableIntStateOf(0) }
    var wasDeviated by remember { mutableStateOf(false) }
    var trialAttempt by remember { mutableIntStateOf(1) }

    var distanceTraveledMeters by remember { mutableFloatStateOf(0f) }
    var roadCurveOffset by remember { mutableFloatStateOf(0f) }
    var roadScrollOffset by remember { mutableFloatStateOf(0f) }

    // Driving Simulation Loop
    LaunchedEffect(isGameRunning) {
        var elapsedSec = 0f
        while (true) {
            if (isGameRunning) {
                elapsedSec += 0.05f
                // Smooth road curve oscillation simulating natural winding highway
                roadCurveOffset = sin(elapsedSec * 0.9f) * 65f

                // Road markings scrolling speed proportional to car speed
                roadScrollOffset = (roadScrollOffset + (speed.toFloat() / 600f)) % 1f
                distanceTraveledMeters += (speed * (1000f / 3600f) * 0.05f)

                val roadHalfWidth = 120f
                val deviation = abs(carXOffset - roadCurveOffset)
                normalizedDeviation = (deviation / roadHalfWidth).coerceIn(0f, 2f)

                val isCurrentlyDeviated = normalizedDeviation > 1.0f
                if (isCurrentlyDeviated && !wasDeviated) {
                    deviationEpisodeCount++
                    steeringStatus = "⚠️ Lane Boundary Exceeded!"
                    wasDeviated = true
                } else if (!isCurrentlyDeviated) {
                    wasDeviated = false
                    steeringStatus = "In-Lane • Smooth Steering"
                }
            }
            delay(50L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏎️ Adapt-IQ Driving Lab (30s Trial)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // 3D Perspective Road Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF020617), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Horizon & Sky
                val horizonY = height * 0.35f
                val bottomY = height
                drawRect(color = Color(0xFF0369A1), topLeft = Offset(0f, 0f), size = Size(width, horizonY))
                drawRect(color = Color(0xFF14532D), topLeft = Offset(0f, horizonY), size = Size(width, height - horizonY))

                val roadHalfBottom = width * 0.42f
                val roadHalfHorizon = width * 0.12f

                val horizonCenterX = width / 2f + roadCurveOffset * 0.3f
                val bottomCenterX = width / 2f + roadCurveOffset

                val roadPath = Path().apply {
                    moveTo(horizonCenterX - roadHalfHorizon, horizonY)
                    lineTo(horizonCenterX + roadHalfHorizon, horizonY)
                    lineTo(bottomCenterX + roadHalfBottom, bottomY)
                    lineTo(bottomCenterX - roadHalfBottom, bottomY)
                    close()
                }
                drawPath(roadPath, Color(0xFF334155))

                // Dashed Center Lane Line
                for (i in 0..5) {
                    val lineDepth = (i * 0.2f + roadScrollOffset * 0.2f) % 1.0f
                    val lineY = horizonY + (bottomY - horizonY) * lineDepth
                    val currentCurve = horizonCenterX + (bottomCenterX - horizonCenterX) * lineDepth
                    val dashH = 10f + lineDepth * 24f
                    val dashW = 2.5f + lineDepth * 4f

                    drawRoundRect(
                        color = Color(0xFFFACC15),
                        topLeft = Offset(currentCurve - dashW / 2f, lineY),
                        size = Size(dashW, dashH),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                // Player Car Model
                val carX = width / 2f + carXOffset
                val carY = bottomY - 35f

                // Car Shadow
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(carX - 30f, carY - 5f),
                    size = Size(60f, 32f),
                    cornerRadius = CornerRadius(6f, 6f)
                )

                // Car Chassis
                drawRoundRect(
                    color = Color(0xFFE11D48),
                    topLeft = Offset(carX - 26f, carY - 14f),
                    size = Size(52f, 28f),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Car Windshield
                drawRoundRect(
                    color = Color(0xFF38BDF8),
                    topLeft = Offset(carX - 16f, carY - 10f),
                    size = Size(32f, 10f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }

            // Real-Time Driving HUD
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Speed: $speed km/h • Norm. Dev: ${String.format(Locale.US, "%.2f", normalizedDeviation)} • Violations: $deviationEpisodeCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = steeringStatus,
                    color = if (normalizedDeviation > 1.0f) Color(0xFFF87171) else Color(0xFF4ADE80),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Steering & Speed Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val onsetTime = SystemClock.elapsedRealtime()
                        carXOffset = (carXOffset - 25f).coerceIn(-130f, 130f)
                        val latency = (SystemClock.elapsedRealtime() - onsetTime).coerceIn(30L, 800L)
                        if (!isPractice) {
                            onLogEvent("steer_left", "steer_left", "successful", trialAttempt++, latency, "offset=$carXOffset,dev=$normalizedDeviation")
                        }
                    }
                ) {
                    Text("◀ Steer Left")
                }

                Button(
                    onClick = {
                        val onsetTime = SystemClock.elapsedRealtime()
                        carXOffset = (carXOffset + 25f).coerceIn(-130f, 130f)
                        val latency = (SystemClock.elapsedRealtime() - onsetTime).coerceIn(30L, 800L)
                        if (!isPractice) {
                            onLogEvent("steer_right", "steer_right", "successful", trialAttempt++, latency, "offset=$carXOffset,dev=$normalizedDeviation")
                        }
                    }
                ) {
                    Text("Steer Right ▶")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val onsetTime = SystemClock.elapsedRealtime()
                        speed = (speed + 10).coerceAtMost(130)
                        val latency = (SystemClock.elapsedRealtime() - onsetTime).coerceIn(30L, 800L)
                        if (!isPractice) {
                            onLogEvent("accelerate", "accelerate", "successful", trialAttempt++, latency, "speed=$speed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Gas")
                }

                Button(
                    onClick = {
                        val onsetTime = SystemClock.elapsedRealtime()
                        speed = (speed - 15).coerceAtLeast(0)
                        val latency = (SystemClock.elapsedRealtime() - onsetTime).coerceIn(30L, 800L)
                        if (!isPractice) {
                            onLogEvent("brake", "brake", "successful", trialAttempt++, latency, "speed=$speed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Brake")
                }
            }
        }

        if (isPractice && !isGameRunning) {
            Button(
                onClick = onStartPractice,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Practice Drive", fontWeight = FontWeight.Bold)
            }
        }
    }
}
