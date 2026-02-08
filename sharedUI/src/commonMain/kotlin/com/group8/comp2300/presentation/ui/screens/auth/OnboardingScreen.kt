@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.mock.sampleOnboardingQuestions
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    isGuest: Boolean = true,
    onRequireAuth: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) } // 0: Welcome, 1: Auth, 2: PIN, 3: Q1, 4: Q2, 5: Result
    var pin by remember { mutableStateOf("") }
    var riskScore by remember { mutableIntStateOf(0) }

    // Container
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
    ) {
        val questionStartIndex = 3
        val questionEndIndex = questionStartIndex + sampleOnboardingQuestions.size

        when (step) {
            0 -> WelcomeStep({ step++ })

            1 -> AuthChoiceStep(isGuest = isGuest, onRequireAuth = onRequireAuth, onNext = { step++ })

            2 ->
                PinStep(
                    onComplete = { enteredPin ->
                        pin = enteredPin
                        step++
                    },
                    pinLength = 4
                )

            in questionStartIndex until questionEndIndex -> {
                val questionIndex = step - questionStartIndex
                val question = sampleOnboardingQuestions[questionIndex]

                val questionText =
                    when (question.id) {
                        1 -> stringResource(Res.string.onboarding_q1_text)
                        2 -> stringResource(Res.string.onboarding_q2_text)
                        else -> question.text
                    }

                val localizedOptions =
                    when (question.id) {
                        1 ->
                            listOf(
                                stringResource(Res.string.onboarding_q1_op1),
                                stringResource(Res.string.onboarding_q1_op2),
                                stringResource(Res.string.onboarding_q1_op3),
                                stringResource(Res.string.onboarding_q1_op4)
                            )

                        2 ->
                            listOf(
                                stringResource(Res.string.onboarding_q2_op1),
                                stringResource(Res.string.onboarding_q2_op2),
                                stringResource(Res.string.onboarding_q2_op3)
                            )

                        else -> question.options
                    }

                QuestionStep(
                    question = questionText,
                    options = localizedOptions,
                    onAnswerSelect = { index ->
                        riskScore += index
                        step++
                    }
                )
            }

            questionEndIndex -> ResultStep(riskScore = riskScore, onFinish = onFinish)
        }

        // Progress Indicator (Optional visual)
        if (step in questionStartIndex until questionEndIndex) {
            val progress = (step - 2) / (sampleOnboardingQuestions.size + 1).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 20.dp)
            )
        }
    }
}

@Composable
fun AuthChoiceStep(isGuest: Boolean, onRequireAuth: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AccountBoxW400Outlinedfill1,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!isGuest) {
            Text(
                stringResource(Res.string.onboarding_welcome_back_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(Res.string.onboarding_signed_in_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(Res.string.onboarding_continue))
            }
        } else {
            Text(
                stringResource(Res.string.onboarding_create_account_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(Res.string.onboarding_sign_up_body),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onRequireAuth, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(Res.string.onboarding_sign_up_log_in))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(stringResource(Res.string.onboarding_continue_as_guest))
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AccountBoxW400Outlinedfill1,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            stringResource(Res.string.onboarding_welcome_vita),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(Res.string.onboarding_welcome_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text(stringResource(Res.string.onboarding_get_started))
        }
    }
}

@Composable
fun PinStep(onComplete: (String) -> Unit, modifier: Modifier = Modifier, pinLength: Int = 4) {
    var pin by rememberSaveable { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val onCompleteState by rememberUpdatedState(onComplete)

    /* auto-advance when full */
    LaunchedEffect(pin) {
        if (pin.length == pinLength) {
            onCompleteState(pin)
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
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.onboarding_create_pin_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(text = stringResource(Res.string.onboarding_create_pin_desc), color = MaterialTheme.colorScheme.secondary)

        Spacer(Modifier.height(32.dp))

        /* PIN dots */
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(pinLength) { index ->
                Box(
                    modifier =
                        Modifier.size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < pin.length) {
                                    MaterialTheme.colorScheme.primary
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
                                if (isBackspace && pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                } else if (!isBackspace && pin.length < pinLength) {
                                    pin += label
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

@Composable
fun QuestionStep(
    question: String,
    options: List<String>,
    onAnswerSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { onAnswerSelect(index) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(option, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ResultStep(riskScore: Int, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    // Mock Logic: Higher score = higher risk recommendation
    val recommendation =
        if (riskScore > 2) {
            stringResource(Res.string.onboarding_every_3_months)
        } else {
            stringResource(Res.string.onboarding_every_6_months)
        }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.CheckW400Outlinedfill1,
            contentDescription = null,
            tint = Color(0xFF4CAF50), // Success Green
            modifier = Modifier.size(100.dp).background(Color(0xFFE8F5E9), CircleShape).padding(16.dp)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(Res.string.onboarding_all_set),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.onboarding_recommended_plan),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.onboarding_screening_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    recommendation,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(Res.string.onboarding_go_to_dashboard))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.ArrowForwardW400Outlinedfill1, null)
        }
    }
}
