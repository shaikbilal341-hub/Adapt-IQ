package com.example.adaptiq.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptiq.ui.components.*
import com.example.adaptiq.ui.theme.*

@Composable
fun TypingActivityScreen(
    testId: String,
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
    val targetSentence = "Technology is changing the way we interact with the world."

    var typedText by remember { mutableStateOf("") }
    var isPractice by remember { mutableStateOf(true) }
    var isCompleted by remember { mutableStateOf(false) }
    var lastCharCount by remember { mutableIntStateOf(0) }
    var typingStartTime by remember { mutableLongStateOf(0L) }
    var lastKeyPressTime by remember { mutableLongStateOf(0L) }
    var attemptCount by remember { mutableIntStateOf(1) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isPractice) {
        if (!isPractice) {
            focusRequester.requestFocus()
            keyboardController?.show()
            typingStartTime = System.currentTimeMillis()
            lastKeyPressTime = typingStartTime
            onLogEvent(
                "typing_started",
                "typing_started",
                "successful",
                attemptCount,
                0L,
                "target_length=${targetSentence.length}"
            )
        }
    }

    AdaptIQScreenContainer(scrollable = true) {
        AdaptIQSectionHeader(
            title = "Activity 4 of 13: Typing",
            subtitle = "Type the sentence shown below exactly as it appears."
        )

        PracticeHeader(
            isPractice = isPractice,
            onStartOfficialCollection = {
                isPractice = false
                typedText = ""
                lastCharCount = 0
                isCompleted = false
            }
        )

        // Target Sentence Box
        AdaptIQSurfaceCard(
            containerColor = SurfaceElevatedVariant,
            borderColor = AccentBlue.copy(alpha = 0.4f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TARGET SENTENCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue,
                    letterSpacing = 1.sp
                )
                Text(
                    text = targetSentence,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        // Input Field
        OutlinedTextField(
            value = typedText,
            onValueChange = { newValue ->
                if (isCompleted) return@OutlinedTextField

                val now = System.currentTimeMillis()
                val interActionMs = if (lastKeyPressTime > 0) now - lastKeyPressTime else 0L
                lastKeyPressTime = now

                val newCount = newValue.length
                val oldCount = lastCharCount

                val detectedAction = when {
                    newCount < oldCount -> "backspace"
                    newCount > oldCount && newValue.endsWith(" ") -> "space"
                    else -> "key_press"
                }

                if (!isPractice) {
                    onLogEvent(
                        "key_press",
                        detectedAction,
                        "successful",
                        attemptCount,
                        interActionMs,
                        "pos=$newCount,len=$newCount"
                    )
                }

                typedText = newValue
                lastCharCount = newCount

                if (newValue.trim().equals(targetSentence.trim(), ignoreCase = true) || newValue.length >= targetSentence.length) {
                    isCompleted = true
                    val totalDuration = now - typingStartTime
                    if (!isPractice) {
                        onLogEvent(
                            "typing_completed",
                            "typing_completed",
                            "successful",
                            attemptCount,
                            totalDuration,
                            "final_chars=${newValue.length}"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text("Type here...", color = TextSecondary) },
            placeholder = { Text("Start typing the sentence above...", color = TextSecondary.copy(alpha = 0.6f)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            minLines = 3,
            maxLines = 5,
            enabled = !isCompleted,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceElevatedVariant,
                unfocusedContainerColor = SurfaceElevated,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentTeal
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Progress Bar
        val progress = (typedText.length.toFloat() / targetSentence.length.toFloat()).coerceIn(0f, 1f)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Typing Progress",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal
                )
            }
            AdaptIQProgressIndicator(
                progress = progress
            )
        }

        if (isCompleted) {
            AdaptIQSurfaceCard(
                borderColor = StatusSuccess.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "✓ Typing Completed Successfully!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )
                }
            }

            AdaptIQPrimaryButton(
                text = "Continue to Session Dashboard",
                onClick = onActivityCompleted
            )
        }

        AdaptIQOutlinedButton(
            text = "Back",
            onClick = onBack
        )
    }
}
