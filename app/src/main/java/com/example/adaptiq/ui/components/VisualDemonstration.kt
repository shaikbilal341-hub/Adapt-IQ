package com.example.adaptiq.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Visual demonstration showing animated finger movement for guided actions
 */
@Composable
fun VisualDemonstration(
    action: String,
    title: String = "",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gesture_anim")

    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val primaryColor = Color(0xFF1976D2)
                val secondaryColor = Color(0xFFBBDEFB)

                when (action.lowercase()) {
                    "swipe_up", "scroll_up" -> {
                        val startY = cy + 30f
                        val endY = cy - 30f
                        val currentY = startY - (startY - endY) * animProgress

                        drawLine(
                            color = secondaryColor,
                            start = Offset(cx, startY),
                            end = Offset(cx, endY),
                            strokeWidth = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(
                            color = primaryColor,
                            radius = 16f,
                            center = Offset(cx, currentY)
                        )
                    }

                    "swipe_down", "scroll_down" -> {
                        val startY = cy - 30f
                        val endY = cy + 30f
                        val currentY = startY + (endY - startY) * animProgress

                        drawLine(
                            color = secondaryColor,
                            start = Offset(cx, startY),
                            end = Offset(cx, endY),
                            strokeWidth = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(
                            color = primaryColor,
                            radius = 16f,
                            center = Offset(cx, currentY)
                        )
                    }

                    "swipe_left", "scroll_left" -> {
                        val startX = cx + 50f
                        val endX = cx - 50f
                        val currentX = startX - (startX - endX) * animProgress

                        drawLine(
                            color = secondaryColor,
                            start = Offset(startX, cy),
                            end = Offset(endX, cy),
                            strokeWidth = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(
                            color = primaryColor,
                            radius = 16f,
                            center = Offset(currentX, cy)
                        )
                    }

                    "swipe_right", "scroll_right" -> {
                        val startX = cx - 50f
                        val endX = cx + 50f
                        val currentX = startX + (endX - startX) * animProgress

                        drawLine(
                            color = secondaryColor,
                            start = Offset(startX, cy),
                            end = Offset(endX, cy),
                            strokeWidth = 6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        drawCircle(
                            color = primaryColor,
                            radius = 16f,
                            center = Offset(currentX, cy)
                        )
                    }

                    "double_tap" -> {
                        val pulseRadius = 14f + (animProgress * 20f)
                        val pulseAlpha = (1f - animProgress).coerceIn(0f, 1f)

                        drawCircle(
                            color = primaryColor.copy(alpha = pulseAlpha),
                            radius = pulseRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 4f)
                        )

                        drawCircle(
                            color = primaryColor,
                            radius = 16f,
                            center = Offset(cx, cy)
                        )
                    }

                    else -> {
                        drawCircle(
                            color = primaryColor,
                            radius = 20f + (animProgress * 10f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }
        }
    }
}
