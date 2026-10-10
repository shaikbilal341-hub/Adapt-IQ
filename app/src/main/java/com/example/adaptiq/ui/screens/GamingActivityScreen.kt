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
import kotlin.math.abs
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
    var timeRemainingSeconds by remember { mutableIntStateOf(45) }
    var isGameRunning by remember { mutableStateOf(false) }
    var isGameFinished by remember { mutableStateOf(false) }

    // Countdown Timer (45s research trial)
    LaunchedEffect(isGameRunning) {
        if (isGameRunning) {
            while (timeRemainingSeconds > 0 && isGameRunning) {
                delay(1000)
                timeRemainingSeconds--
            }
            if (timeRemainingSeconds <= 0) {
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
            text = "Engage in controlled 3D research mini-games to measure natural interaction behavior.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (gameMode == GameMode.SELECTION) {
            Text(
                text = "Choose 3D Research Game",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = {
                    gameMode = GameMode.ADAPT_IQ_RUNNER
                    timeRemainingSeconds = 45
                    isGameRunning = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏃 Adapt-IQ Runner (3D Perspective)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3-lane 3D perspective running track with approaching obstacles. Swipe left/right, jump, and slide.")
                }
            }

            Card(
                onClick = {
                    gameMode = GameMode.ADAPT_IQ_DRIVING_LAB
                    timeRemainingSeconds = 45
                    isGameRunning = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏎️ Adapt-IQ Driving Lab (3D Simulation)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("3D perspective road driving simulation with steering, lane deviation tracking, and obstacle avoidance.")
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Dashboard")
            }
        } else {
            // Active Game Header
            PracticeHeader(
                isPractice = isPractice,
                onStartOfficialCollection = {
                    isPractice = false
                    timeRemainingSeconds = 45
                    isGameRunning = true
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
                        GameMode.ADAPT_IQ_RUNNER -> "Adapt-IQ Runner (3D)"
                        GameMode.ADAPT_IQ_DRIVING_LAB -> "Adapt-IQ Driving Lab (3D)"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "⏱️ 00:%02d".format(timeRemainingSeconds),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (!isGameFinished) {
                when (gameMode) {
                    GameMode.ADAPT_IQ_RUNNER -> {
                        AdaptIqRunner3DView(
                            isGameRunning = isGameRunning,
                            isPractice = isPractice,
                            onLogEvent = onLogEvent
                        )
                    }

                    GameMode.ADAPT_IQ_DRIVING_LAB -> {
                        AdaptIqDriving3DView(
                            isGameRunning = isGameRunning,
                            isPractice = isPractice,
                            onLogEvent = onLogEvent
                        )
                    }

                    else -> {}
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
                            text = "✓ 3D Gaming Session Completed!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "45-second research gaming session recorded successfully.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Button(
                    onClick = onActivityCompleted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to Dashboard", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { gameMode = GameMode.SELECTION },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Another Game")
            }
        }
    }
}

/**
 * Obstacle model for 3D Runner
 */
private data class RunnerObstacle(
    val id: Int,
    val lane: Int,
    var depth: Float // 0.0 (horizon) to 1.0 (player)
)

/**
 * Adapt-IQ Runner — Active 3-Lane 3D Perspective Endless Runner with Collision & Monotonic Latency
 */
@Composable
private fun AdaptIqRunner3DView(
    isGameRunning: Boolean,
    isPractice: Boolean,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var isJumping by remember { mutableStateOf(false) }
    var isSliding by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Swipe Left/Right, Up to Jump, Down to Slide") }

    val obstacles = remember { mutableStateListOf<RunnerObstacle>() }
    var obstacleIdCounter by remember { mutableIntStateOf(0) }
    var trialAttempt = remember { mutableIntStateOf(1) }
    var gestureStartTime by remember { mutableLongStateOf(0L) }

    // Game Loop for obstacle movement & collision
    LaunchedEffect(isGameRunning) {
        var lastSpawnTime = SystemClock.elapsedRealtime()
        while (true) {
            if (isGameRunning) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastSpawnTime > 2500L) {
                    lastSpawnTime = now
                    val randomLane = Random.nextInt(0, 3)
                    obstacleIdCounter++
                    obstacles.add(RunnerObstacle(obstacleIdCounter, randomLane, 0.0f))
                }

                val iterator = obstacles.listIterator()
                while (iterator.hasNext()) {
                    val obs = iterator.next()
                    obs.depth += 0.018f

                    if (obs.depth >= 0.82f && obs.depth <= 0.92f) {
                        if (obs.lane == playerLane) {
                            val avoided = isJumping || isSliding
                            val resultStr = if (avoided) "successful" else "incorrect"
                            statusText = if (avoided) "✓ Obstacle Avoided!" else "💥 Collision!"
                            if (avoided) score += 10

                            if (!isPractice) {
                                val reactionMs = SystemClock.elapsedRealtime() - now
                                onLogEvent(
                                    if (obs.lane == 0) "swipe_right" else "jump_or_slide",
                                    if (avoided) "jump_or_slide" else "collision",
                                    resultStr,
                                    trialAttempt.intValue++,
                                    reactionMs.coerceAtLeast(50L),
                                    "lane=${obs.lane},depth=${obs.depth}"
                                )
                            }
                            iterator.remove()
                        }
                    } else if (obs.depth > 1.0f) {
                        iterator.remove()
                        score += 5
                    }
                }
            }
            delay(30L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(Color(0xFF102027), shape = RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { gestureStartTime = SystemClock.elapsedRealtime() },
                    onDragEnd = { },
                    onDrag = { _, dragAmount ->
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        val threshold = 30f

                        var action = ""
                        if (abs(dx) > abs(dy) && dx > threshold && playerLane < 2) {
                            playerLane++
                            action = "swipe_right"
                            statusText = "Lane Shift Right -> Lane $playerLane"
                        } else if (abs(dx) > abs(dy) && dx < -threshold && playerLane > 0) {
                            playerLane--
                            action = "swipe_left"
                            statusText = "Lane Shift Left -> Lane $playerLane"
                        } else if (abs(dy) > abs(dx) && dy < -threshold) {
                            isJumping = true
                            action = "jump"
                            statusText = "Jump over obstacle!"
                        } else if (abs(dy) > abs(dx) && dy > threshold) {
                            isSliding = true
                            action = "roll"
                            statusText = "Slide under obstacle!"
                        }

                        if (action.isNotBlank() && !isPractice) {
                            val duration = SystemClock.elapsedRealtime() - gestureStartTime
                            onLogEvent(
                                action,
                                action,
                                "successful",
                                trialAttempt.intValue++,
                                duration.coerceAtLeast(30L),
                                "lane=$playerLane,action=$action"
                            )
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Horizon & Sky
            drawRect(color = Color(0xFF1A237E), topLeft = Offset(0f, 0f), size = Size(width, height * 0.4f))
            // Ground
            drawRect(color = Color(0xFF263238), topLeft = Offset(0f, height * 0.4f), size = Size(width, height * 0.6f))

            val horizonX = width / 2f
            val horizonY = height * 0.4f
            val bottomY = height

            val laneWidthAtBottom = width * 0.8f
            val leftLaneBottomX = width / 2f - laneWidthAtBottom * 0.5f
            val rightLaneBottomX = width / 2f + laneWidthAtBottom * 0.5f

            // Draw Obstacles in 3D perspective
            for (obs in obstacles) {
                val obsY = horizonY + (bottomY - horizonY) * obs.depth
                val laneW = (rightLaneBottomX - leftLaneBottomX) / 3f
                val obsX = when (obs.lane) {
                    0 -> leftLaneBottomX + laneW * 0.5f + (obs.depth * (horizonX - leftLaneBottomX - laneW * 0.5f))
                    1 -> horizonX
                    else -> rightLaneBottomX - laneW * 0.5f - (obs.depth * (rightLaneBottomX - horizonX - laneW * 0.5f))
                }
                val obsSize = 30f * (0.2f + obs.depth * 0.8f)

                drawRect(
                    color = Color(0xFFD32F2F),
                    topLeft = Offset(obsX - obsSize / 2f, obsY - obsSize / 2f),
                    size = Size(obsSize, obsSize)
                )
            }

            val leftLaneCenter = width / 2f - laneWidthAtBottom * 0.3f
            val centerLaneCenter = width / 2f
            val rightLaneCenter = width / 2f + laneWidthAtBottom * 0.3f

            val playerX = when (playerLane) {
                0 -> leftLaneCenter
                1 -> centerLaneCenter
                else -> rightLaneCenter
            }

            val playerY = if (isJumping) height - 140f else height - 90f
            val playerSize = if (isSliding) 35f else 60f

            drawCircle(
                color = Color(0xFFFF4081),
                radius = playerSize / 2f,
                center = Offset(playerX, playerY)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = statusText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text(text = "Score: $score", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }

    LaunchedEffect(isJumping, isSliding) {
        if (isJumping) {
            delay(450)
            isJumping = false
        }
        if (isSliding) {
            delay(450)
            isSliding = false
        }
    }
}

/**
 * Adapt-IQ Driving Lab — 3D Perspective Road Simulation with Lane Deviation & Obstacles
 */
@Composable
private fun AdaptIqDriving3DView(
    isGameRunning: Boolean,
    isPractice: Boolean,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var carXOffset by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableIntStateOf(60) }
    var steeringStatus by remember { mutableStateOf("Steer to stay on the 3D road") }
    var laneDeviationCount by remember { mutableIntStateOf(0) }
    var trialAttempt = remember { mutableIntStateOf(1) }

    // Driving simulation loop
    LaunchedEffect(isGameRunning) {
        while (true) {
            if (isGameRunning) {
                if (abs(carXOffset) > 100f) {
                    laneDeviationCount++
                    steeringStatus = "⚠️ Lane Deviation Warning!"
                }
            }
            delay(100L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF004D40), shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏎️ Adapt-IQ Driving Lab (3D Simulation)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // 3D Perspective Road Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF212121), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Sky
                drawRect(color = Color(0xFF0D47A1), topLeft = Offset(0f, 0f), size = Size(width, height * 0.35f))
                // Road Perspective Trapezoid
                val roadPath = Path().apply {
                    moveTo(width / 2f - 40f, height * 0.35f)
                    lineTo(width / 2f + 40f, height * 0.35f)
                    lineTo(width + carXOffset * 1.5f, height)
                    lineTo(carXOffset * 1.5f, height)
                    close()
                }
                drawPath(roadPath, Color(0xFF616161))

                // Vehicle Representation
                val carX = width / 2f + carXOffset
                val carY = height - 40f
                drawRoundRect(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(carX - 25f, carY - 15f),
                    size = Size(50f, 30f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            Text(
                text = "Speed: $speed km/h • Deviations: $laneDeviationCount • $steeringStatus",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            )
        }

        // Steering & Pedal Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val startTime = SystemClock.elapsedRealtime()
                        carXOffset -= 25f
                        steeringStatus = "Steering Left"
                        val duration = SystemClock.elapsedRealtime() - startTime
                        if (!isPractice) {
                            onLogEvent("steer_left", "steer_left", "successful", trialAttempt.intValue++, duration.coerceAtLeast(30L), "offset=$carXOffset")
                        }
                    }
                ) {
                    Text("◀ Steer Left")
                }

                Button(
                    onClick = {
                        val startTime = SystemClock.elapsedRealtime()
                        carXOffset += 25f
                        steeringStatus = "Steering Right"
                        val duration = SystemClock.elapsedRealtime() - startTime
                        if (!isPractice) {
                            onLogEvent("steer_right", "steer_right", "successful", trialAttempt.intValue++, duration.coerceAtLeast(30L), "offset=$carXOffset")
                        }
                    }
                ) {
                    Text("Steer Right ▶")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val startTime = SystemClock.elapsedRealtime()
                        speed = (speed + 10).coerceAtMost(140)
                        steeringStatus = "Accelerating"
                        val duration = SystemClock.elapsedRealtime() - startTime
                        if (!isPractice) {
                            onLogEvent("accelerate", "accelerate", "successful", trialAttempt.intValue++, duration.coerceAtLeast(30L), "speed=$speed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Gas")
                }

                Button(
                    onClick = {
                        val startTime = SystemClock.elapsedRealtime()
                        speed = (speed - 15).coerceAtLeast(0)
                        steeringStatus = "Braking"
                        val duration = SystemClock.elapsedRealtime() - startTime
                        if (!isPractice) {
                            onLogEvent("brake", "brake", "successful", trialAttempt.intValue++, duration.coerceAtLeast(30L), "speed=$speed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Brake")
                }
            }
        }
    }
}
