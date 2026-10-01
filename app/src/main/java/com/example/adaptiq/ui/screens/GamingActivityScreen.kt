package com.example.adaptiq.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.PracticeHeader
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class GameMode {
    SELECTION,
    PORTRAIT_RUNNER,
    PORTRAIT_MATCH,
    LANDSCAPE_DRIVING
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

    // Countdown Timer
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
            text = "Gaming Activity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Simple research mini-games designed to collect natural gameplay interactions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (gameMode == GameMode.SELECTION) {
            Text(
                text = "Choose Gaming Mode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                onClick = {
                    gameMode = GameMode.PORTRAIT_RUNNER
                    timeRemainingSeconds = 45
                    isGameRunning = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏃 Portrait Runner Mini-Game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Portrait orientation. Swipe Left/Right to change lanes, Swipe Up to jump, Swipe Down to duck.")
                }
            }

            Card(
                onClick = {
                    gameMode = GameMode.PORTRAIT_MATCH
                    timeRemainingSeconds = 45
                    isGameRunning = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🧩 Portrait Match Mini-Game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Portrait orientation. Tap, drag, and swipe shapes to complete shape matches.")
                }
            }

            Card(
                onClick = {
                    gameMode = GameMode.LANDSCAPE_DRIVING
                    timeRemainingSeconds = 45
                    isGameRunning = false
                    isGameFinished = false
                    isPractice = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏎️ Landscape Driving Mini-Game", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Landscape orientation. Tap & hold buttons to steer left/right, accelerate, and brake.")
                }
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        } else {
            // Active Game Screen Header
            PracticeHeader(
                isPractice = isPractice,
                onStartOfficialCollection = {
                    isPractice = false
                    timeRemainingSeconds = 45
                    isGameRunning = true
                    isGameFinished = false
                }
            )

            // Timer & Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (gameMode) {
                        GameMode.PORTRAIT_RUNNER -> "Portrait Runner"
                        GameMode.PORTRAIT_MATCH -> "Portrait Match"
                        GameMode.LANDSCAPE_DRIVING -> "Landscape Driving"
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
                    GameMode.PORTRAIT_RUNNER -> {
                        PortraitRunnerGame(
                            isPractice = isPractice,
                            onLogEvent = onLogEvent
                        )
                    }

                    GameMode.PORTRAIT_MATCH -> {
                        PortraitMatchGame(
                            isPractice = isPractice,
                            onLogEvent = onLogEvent
                        )
                    }

                    GameMode.LANDSCAPE_DRIVING -> {
                        LandscapeDrivingGame(
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
                            text = "✓ Mini-Game Completed!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "45-second research gameplay session recorded successfully.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Button(
                    onClick = onActivityCompleted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to Session Dashboard", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { gameMode = GameMode.SELECTION },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Game Mode")
            }
        }
    }
}

/**
 * Original Portrait Lane Runner Game
 */
@Composable
private fun PortraitRunnerGame(
    isPractice: Boolean,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) } // 0=Left, 1=Center, 2=Right
    var isJumping by remember { mutableStateOf(false) }
    var isDucking by remember { mutableStateOf(false) }
    var lastActionText by remember { mutableStateOf("Swipe Left/Right to change lanes, Up to Jump, Down to Roll") }

    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF263238), shape = RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragStartOffset = it },
                    onDragEnd = {
                        // Handled in onDrag if threshold reached
                    },
                    onDrag = { change, dragAmount ->
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        val threshold = 30f

                        var action = ""
                        if (abs(dx) > abs(dy) && dx > threshold && playerLane < 2) {
                            playerLane++
                            action = "swipe_right"
                            lastActionText = "Swipe Right -> Lane $playerLane"
                        } else if (abs(dx) > abs(dy) && dx < -threshold && playerLane > 0) {
                            playerLane--
                            action = "swipe_left"
                            lastActionText = "Swipe Left -> Lane $playerLane"
                        } else if (abs(dy) > abs(dx) && dy < -threshold) {
                            isJumping = true
                            action = "jump"
                            lastActionText = "Swipe Up -> Jump!"
                        } else if (abs(dy) > abs(dx) && dy > threshold) {
                            isDucking = true
                            action = "roll"
                            lastActionText = "Swipe Down -> Roll!"
                        }

                        if (action.isNotBlank() && !isPractice) {
                            onLogEvent(
                                action,
                                action,
                                "successful",
                                1,
                                100L,
                                "lane=$playerLane"
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
            val laneWidth = width / 3f

            // Draw Lanes
            for (i in 0..2) {
                val x = i * laneWidth
                drawRect(
                    color = if (i % 2 == 0) Color(0xFF37474F) else Color(0xFF455A64),
                    topLeft = Offset(x, 0f),
                    size = Size(laneWidth, height)
                )
            }

            // Player Position
            val playerX = playerLane * laneWidth + (laneWidth / 2f) - 25f
            val playerY = if (isJumping) height - 120f else height - 70f
            val playerHeight = if (isDucking) 25f else 50f

            drawRoundRect(
                color = Color(0xFFFF4081),
                topLeft = Offset(playerX, playerY),
                size = Size(50f, playerHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        Text(
            text = lastActionText,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        )
    }

    LaunchedEffect(isJumping, isDucking) {
        if (isJumping) {
            delay(400)
            isJumping = false
        }
        if (isDucking) {
            delay(400)
            isDucking = false
        }
    }
}

/**
 * Original Portrait Shape Matching Game
 */
@Composable
private fun PortraitMatchGame(
    isPractice: Boolean,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var selectedTile by remember { mutableIntStateOf(-1) }
    var matchedCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tap matching shape pairs below",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0..3).forEach { index ->
                Button(
                    onClick = {
                        if (selectedTile == -1) {
                            selectedTile = index
                        } else {
                            matchedCount++
                            selectedTile = -1
                        }

                        if (!isPractice) {
                            onLogEvent(
                                "tap",
                                "tap",
                                "successful",
                                1,
                                50L,
                                "tile_idx=$index"
                            )
                        }
                    },
                    modifier = Modifier.size(70.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTile == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        text = when (index % 2) {
                            0 -> "⭐"
                            else -> "🔵"
                        },
                        fontSize = 22.sp
                    )
                }
            }
        }

        Text(
            text = "Matches made: $matchedCount",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Original Landscape Driving Game
 */
@Composable
private fun LandscapeDrivingGame(
    isPractice: Boolean,
    onLogEvent: (expectedAction: String, detectedAction: String, result: String, attemptId: Int, durationMs: Long, touchInfo: String) -> Unit
) {
    var carXOffset by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableIntStateOf(40) }
    var statusText by remember { mutableStateOf("Use steering & pedal controls") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B5E20), shape = RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🏎️ LANDSCAPE DRIVING SIMULATOR",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Road Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color(0xFF424242), shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f + carXOffset
                val cy = size.height / 2f

                // Draw Car
                drawRoundRect(
                    color = Color(0xFFFFC107),
                    topLeft = Offset(cx - 30f, cy - 20f),
                    size = Size(60f, 40f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            Text(
                text = "Speed: $speed km/h • $statusText",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            )
        }

        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Steering Left / Right
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        carXOffset -= 20f
                        statusText = "Steer Left"
                        if (!isPractice) {
                            onLogEvent("steer_left", "steer_left", "successful", 1, 80L, "offset=$carXOffset")
                        }
                    }
                ) {
                    Text("◀ Steer Left")
                }

                Button(
                    onClick = {
                        carXOffset += 20f
                        statusText = "Steer Right"
                        if (!isPractice) {
                            onLogEvent("steer_right", "steer_right", "successful", 1, 80L, "offset=$carXOffset")
                        }
                    }
                ) {
                    Text("Steer Right ▶")
                }
            }

            // Accelerate / Brake
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        speed = (speed + 10).coerceAtMost(120)
                        statusText = "Accelerate"
                        if (!isPractice) {
                            onLogEvent("accelerate", "accelerate", "successful", 1, 100L, "speed=$speed")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Gas")
                }

                Button(
                    onClick = {
                        speed = (speed - 15).coerceAtLeast(0)
                        statusText = "Brake"
                        if (!isPractice) {
                            onLogEvent("brake", "brake", "successful", 1, 100L, "speed=$speed")
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
