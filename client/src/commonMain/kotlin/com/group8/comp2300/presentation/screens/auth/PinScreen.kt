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
    onErrorMessageCleared: () -> Unit = {},
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var savedPin by rememberSaveable { mutableStateOf("") }
    var isConfirming by rememberSaveable { mutableStateOf(false) }
    var internalError: String? by remember { mutableStateOf(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }
    val mismatchErrorText = stringResource(Res.string.onboarding_pin_mismatch)

    val displayError = internalError ?: errorMessage

    val displayTitle = title ?: if (isSetup) {
        if (isConfirming) {
            stringResource(Res.string.onboarding_confirm_pin_title)
        } else {
            stringResource(Res.string.onboarding_create_pin_title)
        }
    } else {
        stringResource(Res.string.onboarding_create_pin_title)
    }

    val displayDescription = description ?: if (isSetup) {
        if (isConfirming) {
            stringResource(Res.string.onboarding_confirm_pin_desc)
        } else {
            stringResource(Res.string.onboarding_create_pin_desc)
        }
    } else {
        ""
    }

    // Shake on external error
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shakeTrigger++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Shake animation
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            repeat(4) {
                shakeOffset.animateTo(10f, tween(40, easing = LinearEasing))
                shakeOffset.animateTo(-10f, tween(40, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, tween(40, easing = LinearEasing))
        }
    }

    // All state transitions happen synchronously in handleKey — no LaunchedEffect(pin)
    fun handleKey(key: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        // Any keypress while error is showing: clear error, reset, then process the key
        if (displayError != null) {
            internalError = null
            onErrorMessageCleared()
            if (isSetup) {
                isConfirming = false
                savedPin = ""
            }
            pin = ""
        }

        if (key == "⌫") {
            if (pin.isNotEmpty()) pin = pin.dropLast(1)
            return
        }

        if (pin.length >= pinLength) return
        pin += key

        if (pin.length == pinLength) {
            when {
                isSetup && !isConfirming -> {
                    savedPin = pin
                    pin = ""
                    isConfirming = true
                }

                isSetup && isConfirming -> {
                    if (pin == savedPin) {
                        onComplete(pin)
                    } else {
                        internalError = mismatchErrorText
                        shakeTrigger++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                else -> onComplete(pin)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.LockW400Outlinedfill1,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = if (displayError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = isConfirming,
            label = "PinTextTransition",
        ) { _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (displayError !=
                        null
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = displayError ?: displayDescription,
                    color = if (displayError !=
                        null
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        /* PIN dots */
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.offset(x = shakeOffset.value.dp),
        ) {
            repeat(pinLength) { index ->
                val isFilled = index < pin.length
                val scale by animateFloatAsState(
                    targetValue = if (isFilled) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "PinDotScaleAnimation",
                )

                Box(
                    modifier =
                    Modifier.size(24.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) {
                                if (displayError !=
                                    null
                                ) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
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
                    listOf(null, "0", "⌫"),
                )

            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach inner@{ label ->
                        if (label == null) {
                            Spacer(Modifier.size(72.dp))
                            return@inner
                        }

                        KeyPadButton(
                            text = label,
                            onClick = { handleKey(label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyPadButton(text: String, onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier.size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnClick() }) }
            .semantics { contentDescription = text },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
