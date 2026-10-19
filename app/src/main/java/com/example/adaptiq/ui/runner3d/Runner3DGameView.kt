package com.example.adaptiq.ui.runner3d

import android.app.Activity
import android.content.pm.ActivityInfo
import android.opengl.GLSurfaceView
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.screens.GamingTrialResults
import com.example.adaptiq.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * 3D Temple Run–Style Endless Runner View (Activity 12).
 * Integrates the high-performance OpenGL ES 2.0 Renderer directly into Jetpack Compose.
 * Preserves 100% of Adapt-IQ's research data collection contracts, monotonic 30s timing, and telemetry.
 */
@Composable
fun Runner3DGameView(
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

    // Strict Portrait Lock during Temple Runner
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

    var statusText by remember { mutableStateOf("Swipe track: ◀ Left • Right ▶ • ▲ Jump • ▼ Slide") }
    var actionsCount by remember { mutableIntStateOf(0) }
    var totalLatencySum by remember { mutableLongStateOf(0L) }
    var latencyEventsCount by remember { mutableIntStateOf(0) }
    var showCollisionFlash by remember { mutableStateOf(false) }
    var hudTick by remember { mutableLongStateOf(0L) }

    var lastGestureTime by remember { mutableLongStateOf(0L) }
    var lastLaneLogTime by remember { mutableLongStateOf(0L) }

    // Initialize the OpenGL ES 3D Renderer
    val renderer = remember {
        Runner3DGLRenderer(
            onEventCollision = { obstacle ->
                val laneName = when (obstacle.lane) {
                    0 -> "Left"
                    1 -> "Center"
                    else -> "Right"
                }
                statusText = "💥 Impact with ${obstacle.type.name} in $laneName Lane!"
                showCollisionFlash = true
                if (!isPractice) {
                    onLogEvent(
                        when (obstacle.type.name) {
                            "JUMP_HURDLE" -> "jump"
                            "SLIDE_ARCH" -> "slide"
                            else -> "dodge"
                        },
                        "collision",
                        "collision",
                        trialAttemptId,
                        -1L,
                        "obstacleId=${obstacle.obstacleId},type=${obstacle.type.name},lane=${obstacle.lane}"
                    )
                }
            },
            onEventAvoided = { obstacle, latencyMs ->
                val actionName = when (obstacle.type.name) {
                    "JUMP_HURDLE" -> "Leaped over fallen log"
                    "SLIDE_ARCH" -> "Slid under stone arch"
                    else -> "Dodged idol monolith"
                }
                statusText = "✓ Successfully $actionName!"

                if (latencyMs in 1..4000) {
                    totalLatencySum += latencyMs
                    latencyEventsCount++
                }

                if (!isPractice) {
                    onLogEvent(
                        when (obstacle.type.name) {
                            "JUMP_HURDLE" -> "jump"
                            "SLIDE_ARCH" -> "slide"
                            else -> "dodge"
                        },
                        when (obstacle.type.name) {
                            "JUMP_HURDLE" -> "jump"
                            "SLIDE_ARCH" -> "slide"
                            else -> "lane_change"
                        },
                        "successful",
                        trialAttemptId,
                        latencyMs,
                        "obstacleId=${obstacle.obstacleId},type=${obstacle.type.name},lane=${obstacle.lane}"
                    )
                }
            },
            onLaneChanged = { newLane ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastLaneLogTime > 250L) {
                    lastLaneLogTime = now
                    actionsCount++
                    val actionStr = if (newLane == 0) "swipe_left" else if (newLane == 2) "swipe_right" else "center"
                    if (!isPractice && isGameRunning) {
                        onLogEvent(actionStr, actionStr, "successful", trialAttemptId, -1L, "newLane=$newLane")
                    }
                }
            }
        )
    }

    // Synchronize Renderer Lifecycle & Parameters
    LaunchedEffect(isGameRunning, isGamePaused, isPractice) {
        renderer.isRunning = isGameRunning
        renderer.isPaused = isGamePaused
        renderer.isPractice = isPractice
    }

    // Dynamic HUD update tick loop while game is actively running
    LaunchedEffect(isGameRunning, isGamePaused) {
        if (isGameRunning && !isGamePaused) {
            while (isGameRunning && !isGamePaused) {
                delay(80L)
                hudTick++
            }
        }
    }

    // Reset Trial when starting a run or new trial
    LaunchedEffect(isGameRunning, trialAttemptId) {
        if (isGameRunning) {
            renderer.resetTrial()
            actionsCount = 0
            totalLatencySum = 0L
            latencyEventsCount = 0
            showCollisionFlash = false
            statusText = if (isPractice) "3D Practice Run: Swipe to Dodge & Leap!" else "Trial Started: Navigate 3D Ancient Ruins!"
        }
    }

    // Handle Trial Completion when 30 seconds elapse
    LaunchedEffect(isGameRunning, timeRemainingSeconds) {
        if (!isGameRunning && timeRemainingSeconds == 0 && renderer.metersRun > 0f && trialResults == null) {
            val avgLatency = if (latencyEventsCount > 0) totalLatencySum / latencyEventsCount else -1L
            onTrialFinished(
                GamingTrialResults(
                    gameMode = "Runner (Temple Explorer 3D)",
                    durationSeconds = 30.0f,
                    score = renderer.score,
                    encountersTotal = renderer.obstaclesEncountered,
                    avoidedTotal = renderer.obstaclesAvoided,
                    collisionsTotal = renderer.obstaclesCollided,
                    actionsCount = actionsCount,
                    averageLatencyMs = avgLatency,
                    extraMetricLabel = "Distance Run",
                    extraMetricValue = "${renderer.metersRun.toInt()} meters",
                    completionStatus = "COMPLETED"
                )
            )
        }
    }

    // Auto-dismiss red collision flash
    LaunchedEffect(showCollisionFlash) {
        if (showCollisionFlash) {
            delay(280L)
            showCollisionFlash = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .pointerInput(isGameRunning, isGamePaused) {
                if (isGameRunning && !isGamePaused) {
                    var dragX = 0f
                    var dragY = 0f
                    var gestureHandled = false
                    val swipeThreshold = 38f

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
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastGestureTime > 200L) {
                                        lastGestureTime = now
                                        gestureHandled = true

                                        // Dominant Axis Decision
                                        if (absX > absY * 1.25f) {
                                            if (dragX > 0f) {
                                                renderer.swipeRight()
                                                statusText = "Switched to Right Lane ▶"
                                            } else {
                                                renderer.swipeLeft()
                                                statusText = "◀ Switched to Left Lane"
                                            }
                                        } else if (absY > absX * 1.25f) {
                                            if (dragY < 0f) {
                                                renderer.jump()
                                                actionsCount++
                                                statusText = "▲ Leaping over low hurdle!"
                                                if (!isPractice && isGameRunning) {
                                                    onLogEvent("jump", "jump", "successful", trialAttemptId, -1L, "lane=${renderer.targetLane}")
                                                }
                                            } else {
                                                renderer.slide()
                                                actionsCount++
                                                statusText = "▼ Sliding under ruined arch!"
                                                if (!isPractice && isGameRunning) {
                                                    onLogEvent("slide", "slide", "successful", trialAttemptId, -1L, "lane=${renderer.targetLane}")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
    ) {
        // 1. OpenGL ES 2.0 Real-Time 3D Viewport
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Fullscreen Red Collision Vignette (Only triggers upon confirmed genuine collision!)
        if (showCollisionFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFDC2626).copy(alpha = 0.45f))
            )
        }

        // 3. TOP FLOATING ARCADE RESEARCH HUD
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
                // Exit button
                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.65f)),
                    border = BorderStroke(1.dp, BorderSubtle),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("✕ Exit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Practice / Official Trial Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPractice) Color(0xFFCA8A04) else Color(0xFF0D9488)
                ) {
                    Text(
                        text = if (isPractice) "3D PRACTICE" else "OFFICIAL 30s TRIAL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Monotonic Countdown Timer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, AccentTeal)
                ) {
                    Text(
                        text = if (isPractice && !isGameRunning) "PRACTICE" else "⏱️ 00:%02d".format(timeRemainingSeconds),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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

            // Stats Pill Row (Score, Avoided, Collisions, Meters)
            val currentScore = if (hudTick >= 0) renderer.score else 0
            val currentMeters = renderer.metersRun.toInt()
            val currentAvoided = renderer.obstaclesAvoided
            val currentCollided = renderer.obstaclesCollided

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "Score: $currentScore • ${currentMeters}m",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = "✓ Avoided: $currentAvoided  💥 Collisions: $currentCollided",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (currentCollided > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Real-Time Gesture & Navigation Feedback Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    color = AccentTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. PRACTICE LAUNCH OVERLAY
        if (isPractice && !isGameRunning && trialResults == null) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
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
                        text = "🏛️ Temple Explorer 3D",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "True 3D endless runner in ancient ruins.\n• Swipe Left / Right to change lanes\n• Swipe Up to leap over fallen logs\n• Swipe Down to slide under ruined stone arches",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        // 5. PAUSE OVERLAY
        if (isGamePaused) {
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
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
                        text = "⏸️ 3D RUNNER PAUSED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Active trial countdown is paused.\nTap 'Resume' to continue the run.",
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

        // 6. TRIAL COMPLETED RESULTS DIALOG
        if (trialResults != null) {
            val res = trialResults
            AdaptIQSurfaceCard(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp),
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
                        Text("Obstacles Avoided / Total:", color = TextSecondary)
                        Text("${res.avoidedTotal} / ${res.encountersTotal}", color = AccentTeal)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Collisions Recorded:", color = TextSecondary)
                        Text("${res.collisionsTotal}", color = if (res.collisionsTotal > 0) StatusError else StatusSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Swipe Maneuvers:", color = TextSecondary)
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
