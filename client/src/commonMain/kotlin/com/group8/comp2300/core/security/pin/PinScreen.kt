package com.group8.comp2300.core.security.pin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.group8.comp2300.platform.biometrics.isBiometricAvailable
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.FingerprintW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LockW400Outlinedfill1
import com.group8.comp2300.util.constantTimeEquals
import com.tecknobit.biometrik.BiometrikAuthenticator
import com.tecknobit.biometrik.rememberBiometrikState
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
    onDismiss: (() -> Unit)? = null,
    onBiometricSuccess: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    var savedPin by remember { mutableStateOf<CharArray?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var internalError: String? by remember { mutableStateOf(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }
    val mismatchErrorText = stringResource(Res.string.onboarding_pin_mismatch)

    val showBiometricOption = !isSetup && onBiometricSuccess != null
    val biometricAvailable = showBiometricOption && isBiometricAvailable()
    var biometricTriggered by remember { mutableStateOf(false) }
    val biometrikState = rememberBiometrikState(requestOneTimeOnly = false)

    val displayError = internalError ?: errorMessage

    val displayTitle = title ?: if (isSetup) {
        if (isConfirming) {
            stringResource(Res.string.onboarding_confirm_pin_title)
        } else {
            stringResource(Res.string.onboarding_create_pin_title)
        }
    } else {
        stringResource(Res.string.pin_unlock_title)
    }

    val displayDescription = description ?: if (isSetup) {
        if (isConfirming) {
            stringResource(Res.string.onboarding_confirm_pin_desc)
        } else {
            stringResource(Res.string.onboarding_create_pin_desc)
        }
    } else {
        stringResource(Res.string.pin_unlock_desc)
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

    fun zeroSavedPin() {
        savedPin?.fill('\u0000')
        savedPin = null
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
                zeroSavedPin()
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
                    savedPin = pin.toCharArray()
                    pin = ""
                    isConfirming = true
                }

                isSetup && isConfirming -> {
                    val saved = savedPin
                    if (saved != null && constantTimeEquals(pin, saved.concatToString())) {
                        zeroSavedPin()
                        onComplete(pin)
                    } else {
                        zeroSavedPin()
                        internalError = mismatchErrorText
                        shakeTrigger++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                else -> onComplete(pin)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize().systemBarsPadding(),
    ) {
        if (onDismiss != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(48.dp)) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            Icons.ArrowBackW400Outlinedfill1,
                            contentDescription = stringResource(Res.string.auth_back_desc),
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            val buttonSize = if (maxHeight < 560.dp) {
                val scale = (maxHeight / 560.dp).coerceIn(0.5f, 1f)
                72.dp * scale
            } else {
                72.dp
            }
            val buttonSpacing = if (maxHeight < 560.dp) {
                val scale = (maxHeight / 560.dp).coerceIn(0.5f, 1f)
                24.dp * scale
            } else {
                24.dp
            }
            val rowSpacing = if (maxHeight < 560.dp) {
                val scale = (maxHeight / 560.dp).coerceIn(0.5f, 1f)
                12.dp * scale
            } else {
                12.dp
            }
            val iconSize = if (maxHeight < 560.dp) {
                val scale = (maxHeight / 560.dp).coerceIn(0.5f, 1f)
                28.dp * scale
            } else {
                28.dp
            }
            val infoKeypadSpacing = if (maxHeight < 560.dp) 24.dp else 32.dp

            val isLandscape = maxWidth > maxHeight

            @Composable
            fun PinInfoColumn() {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.LockW400Outlinedfill1,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (displayError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )

                    Spacer(Modifier.height(20.dp))

                    AnimatedContent(
                        targetState = isConfirming,
                        label = "PinTextTransition",
                    ) { _ ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (displayError != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = displayError ?: displayDescription,
                                color = if (displayError != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

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
                                            if (displayError != null) {
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
                }
            }

            @Composable
            fun KeypadColumn() {
                Column(
                    verticalArrangement = Arrangement.spacedBy(rowSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val rows =
                        listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf(null, "0", "⌫"),
                        )

                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                            row.forEach inner@{ label ->
                                if (label == null) {
                                    if (showBiometricOption) {
                                        val biometricDescription = stringResource(Res.string.pin_use_biometric_desc)
                                        KeyPadButton(
                                            text = "",
                                            onClick = { biometricTriggered = true },
                                            enabled = biometricAvailable,
                                            accent = true,
                                            contentDescription = biometricDescription,
                                            icon = {
                                                Icon(
                                                    Icons.FingerprintW400Outlinedfill1,
                                                    contentDescription = biometricDescription,
                                                    modifier = Modifier.size(iconSize),
                                                )
                                            },
                                            size = buttonSize,
                                        )
                                    } else {
                                        Spacer(Modifier.size(buttonSize))
                                    }
                                    return@inner
                                }

                                KeyPadButton(
                                    text = label,
                                    onClick = { handleKey(label) },
                                    size = buttonSize,
                                )
                            }
                        }
                    }
                }
            }

            if (isLandscape) {
                // Two-column layout: info on left, keypad on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PinInfoColumn()
                    KeypadColumn()
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PinInfoColumn()
                        Spacer(Modifier.height(infoKeypadSpacing))
                        KeypadColumn()
                    }
                }
            }

            // Biometric authentication prompt
            if (showBiometricOption && biometricAvailable && biometricTriggered) {
                val biometricTitle = stringResource(Res.string.pin_biometric_prompt_title)
                val biometricReason = stringResource(Res.string.pin_biometric_prompt_reason)
                val biometricFailedText = stringResource(Res.string.pin_biometric_failed)
                val biometricSuccess = requireNotNull(onBiometricSuccess)

                BiometrikAuthenticator(
                    state = biometrikState,
                    appName = "Vita",
                    title = biometricTitle,
                    reason = biometricReason,
                    onSuccess = {
                        LaunchedEffect(Unit) { biometricSuccess() }
                    },
                    onFailure = {
                        LaunchedEffect(Unit) {
                            biometricTriggered = false
                            internalError = biometricFailedText
                            shakeTrigger++
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onHardwareUnavailable = {
                        LaunchedEffect(Unit) { biometricTriggered = false }
                    },
                    onFeatureUnavailable = {
                        LaunchedEffect(Unit) { biometricTriggered = false }
                    },
                    onAuthenticationNotSet = {
                        LaunchedEffect(Unit) { biometricTriggered = false }
                    },
                )
            }
        }
    }
}

@Composable
fun KeyPadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    accent: Boolean = false,
    contentDescription: String? = text.takeIf { it.isNotEmpty() },
    icon: (@Composable () -> Unit)? = null,
    size: Dp = 72.dp,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val resolvedContainerColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val resolvedContentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        accent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier.size(size)
            .clip(CircleShape)
            .background(resolvedContainerColor)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) { detectTapGestures(onTap = { currentOnClick() }) }
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription?.let { this.contentDescription = it }
                if (!enabled) disabled()
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            if (icon != null) {
                icon()
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    color = LocalContentColor.current,
                )
            }
        }
    }
}
