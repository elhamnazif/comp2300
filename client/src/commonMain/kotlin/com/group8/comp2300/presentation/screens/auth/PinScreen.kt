package com.group8.comp2300.presentation.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun PinScreen(
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    pinLength: Int = 4,
    isSetup: Boolean = true,
    title: String? = null,
    description: String? = null,
    errorMessage: String? = null,
    onErrorMessageCleared: () -> Unit = {}
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmedPin by rememberSaveable { mutableStateOf("") }
    var isConfirming by rememberSaveable { mutableStateOf(false) }
    var internalError: String? by remember { mutableStateOf(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    var errorClearJob by remember { mutableStateOf<Job?>(null) }

    val haptic = LocalHapticFeedback.current
    val onCompleteState by rememberUpdatedState(onComplete)
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val displayTitle = title ?: if (isSetup) {
        if (isConfirming) stringResource(Res.string.onboarding_confirm_pin_title)
        else stringResource(Res.string.onboarding_create_pin_title)
    } else {
        stringResource(Res.string.onboarding_create_pin_title) // Fallback if no title provided
    }

    val displayDescription = description ?: if (isSetup) {
        if (isConfirming) stringResource(Res.string.onboarding_confirm_pin_desc)
        else stringResource(Res.string.onboarding_create_pin_desc)
    } else {
        ""
    }

    val displayError = internalError ?: errorMessage
    val mismatchErrorText = stringResource(Res.string.onboarding_pin_mismatch)

    // Handle external error triggers
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shakeTrigger++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            errorClearJob?.cancel()
            errorClearJob = coroutineScope.launch {
                delay(1200)
                pin = ""
                onErrorMessageCleared()
            }
        }
    }

    /* auto-advance when full */
    LaunchedEffect(pin) {
        if (pin.isNotEmpty()) {
            internalError = null
            if (errorMessage != null) {
                onErrorMessageCleared()
            }
        }
        if (pin.length == pinLength) {
            if (isSetup && !isConfirming) {
                // First entry complete, switch to confirmation mode
                confirmedPin = pin
                pin = ""
                isConfirming = true
            } else if (isSetup && isConfirming) {
                // Confirmation entry complete, verify
                if (pin == confirmedPin) {
                    onCompleteState(pin)
                } else {
                    internalError = mismatchErrorText
                    shakeTrigger++
                    // Trigger strong haptic on error
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    errorClearJob?.cancel()
                    errorClearJob = coroutineScope.launch {
                        delay(1200) // Increased delay so user can read the error before it resets
                        pin = ""
                        internalError = null
                        if (isSetup) {
                            isConfirming = false
                            confirmedPin = ""
                        }
                    }
                }
            } else {
                // Not setup mode, just input PIN
                onCompleteState(pin)
                // Optionally clear PIN here if we want to reset it after entry. 
                // Wait for external error to handle reset if it's wrong.
            }
        }
    }

    /* Shake effect on error */
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            repeat(4) {
                shakeOffset.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(durationMillis = 40, easing = LinearEasing)
                )
                shakeOffset.animateTo(
                    targetValue = -10f,
                    animationSpec = tween(durationMillis = 40, easing = LinearEasing)
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(durationMillis = 40, easing = LinearEasing))
        }
    }

    Column(
        modifier = modifier.fillMaxSize().systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.LockW400Outlinedfill1,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (displayError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = isConfirming, // Still useful to animate if switching confirmation states
            label = "PinTextTransition"
        ) { confirming ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Just use the derived titles directly, ignoring the AnimatedContent implicit argument 
                // in favor of our more robust logic above.
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (displayError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = displayError ?: displayDescription,
                    color = if (displayError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        /* PIN dots */
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.offset(x = shakeOffset.value.dp)
        ) {
            repeat(pinLength) { index ->
                val isFilled = index < pin.length
                val scale by animateFloatAsState(
                    targetValue = if (isFilled) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "PinDotScaleAnimation"
                )

                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) {
                                    if (displayError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        /* Keypad */
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val rows =
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(null, "0", "⌫") // null = spacer
                )

            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach inner@{ label ->
                        if (label == null) {
                            Spacer(Modifier.size(72.dp))
                            return@inner
                        }

                        val isBackspace = label == "⌫"
                        KeyPadButton(
                            text = label,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (displayError != null) {
                                    // Dynamic Error Reset on any keypress
                                    errorClearJob?.cancel()
                                    internalError = null
                                    onErrorMessageCleared()
                                    if (isSetup) {
                                        isConfirming = false
                                        confirmedPin = ""
                                    }
                                    pin = ""
                                }

                                if (isBackspace) {
                                    if (pin.isNotEmpty()) {
                                        pin = pin.dropLast(1)
                                    }
                                } else { // It's a digit
                                    if (pin.length < pinLength) {
                                        pin += label
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyPadButton(text: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.size(72.dp)
                .clip(CircleShape)
                .background(
                    if (pressed) {
                        MaterialTheme.colorScheme.primary.copy(alpha = .3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                )
                .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease() }, onTap = { onClick() }) }
                .semantics { contentDescription = text }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color =
                if (text == "⌫") {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
        )
    }
}
