package com.example.adaptiq.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.theme.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// =========================================================================
// Adapt-IQ Reusable Compose Design System Components
// =========================================================================

/**
 * Standard root container with deep navy background and subtle ambient top glow (BoxScope)
 */
@Composable
fun AdaptIQScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .background(BackgroundGlowBrush)
    ) {
        content()
    }
}

/**
 * Structured activity screen container providing ColumnScope with spacing, padding, and optional scrolling
 */
@Composable
fun AdaptIQScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .background(BackgroundGlowBrush)
    ) {
        val colModifier = if (scrollable) {
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        }
        Column(
            modifier = colModifier,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * Primary Action Button — Teal Accent, dark high-contrast text, smooth press response
 */
@Composable
fun AdaptIQPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ButtonShape,
    height: Dp = 52.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "btn_scale")

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(height)
            .scale(scale),
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentTeal,
            contentColor = TextDark,
            disabledContainerColor = SurfaceElevatedVariant,
            disabledContentColor = TextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            trailingIcon?.let {
                Spacer(modifier = Modifier.width(8.dp))
                it()
            }
        }
    }
}

/**
 * Secondary Action Button — Elevated surface, subtle border, primary text
 */
@Composable
fun AdaptIQSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ButtonShape,
    height: Dp = 52.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "btn_sec_scale")

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(height)
            .scale(scale)
            .border(BorderStroke(1.dp, BorderSubtle), shape),
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceElevatedVariant,
            contentColor = TextPrimary,
            disabledContainerColor = BackgroundSecondary,
            disabledContentColor = TextMuted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Outlined Action Button — Transparent background with subtle border, teal or primary text
 */
@Composable
fun AdaptIQOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    borderColor: Color = BorderSubtle,
    textColor: Color = TextPrimary,
    shape: Shape = ButtonShape,
    height: Dp = 50.dp
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = shape,
        border = BorderStroke(1.dp, if (enabled) borderColor else BorderSubtle.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor,
            disabledContentColor = TextMuted
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }
}

/**
 * Standard Surface Card with subtle border and elevated navy surface
 */
@Composable
fun AdaptIQSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    containerColor: Color = SurfaceElevated,
    borderColor: Color = BorderSubtle,
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .clickable(onClick = onClick)
            .background(containerColor)
    } else {
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(borderWidth, borderColor), shape)
            .background(containerColor)
    }

    Column(
        modifier = cardModifier.padding(contentPadding),
        content = content
    )
}

/**
 * Section Header Component
 */
@Composable
fun AdaptIQSectionHeader(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            badgeText?.let {
                AdaptIQStatusBadge(text = it, status = "INFO")
            }
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

/**
 * Reusable Status Badge Pill
 */
@Composable
fun AdaptIQStatusBadge(
    text: String,
    status: String = "INFO", // SUCCESS, WARNING, ERROR, INFO, NOT_STARTED
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "SUCCESS", "COMPLETED", "DONE", "SYNCED" -> Pair(StatusSuccessContainer, StatusSuccess)
        "WARNING", "IN_PROGRESS", "PENDING", "RESUME" -> Pair(StatusWarningContainer, StatusWarning)
        "ERROR", "FAILED" -> Pair(StatusErrorContainer, StatusError)
        "NOT_STARTED", "START", "READY" -> Pair(SurfaceElevatedVariant, AccentTeal)
        else -> Pair(StatusInfoContainer, StatusInfo)
    }

    Surface(
        modifier = modifier,
        shape = BadgeShape,
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Linear Progress Indicator with Teal Gradient
 */
@Composable
fun AdaptIQProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    height: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(SurfaceElevatedVariant)
    ) {
        val fillBrush = if (color != null) SolidColor(color) else TealGradientBrush
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(height / 2))
                .background(fillBrush)
        )
    }
}

/**
 * Styled Text Field with elevated navy background and teal focus border
 */
@Composable
fun AdaptIQTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isError) StatusError else TextSecondary
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, color = TextMuted) } },
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = InputFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = BorderSubtle,
                errorBorderColor = StatusError,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentTeal
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = StatusError,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Clean Empty State Component
 */
@Composable
fun AdaptIQEmptyState(
    title: String,
    description: String,
    iconText: String = "📋",
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceElevatedVariant,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = iconText, fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        actionButton?.let {
            Spacer(modifier = Modifier.height(20.dp))
            it()
        }
    }
}
