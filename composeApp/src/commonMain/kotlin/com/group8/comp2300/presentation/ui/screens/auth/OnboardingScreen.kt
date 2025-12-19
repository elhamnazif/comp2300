package com.group8.comp2300.presentation.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.group8.comp2300.mock.sampleOnboardingQuestions

@Composable
fun OnboardingScreen(isGuest: Boolean = true, onRequireAuth: () -> Unit = {}, onFinished: () -> Unit) {
    var step by remember {
        mutableIntStateOf(0)
    } // 0: Welcome, 1: Auth, 2: PIN, 3: Q1, 4: Q2, 5: Result
    var pin by remember { mutableStateOf("") }
    var riskScore by remember { mutableIntStateOf(0) }

    // Container
    Box(
        modifier =
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
    ) {
        val questionStartIndex = 3
        val questionEndIndex = questionStartIndex + sampleOnboardingQuestions.size

        when (step) {
            0 -> WelcomeStep(onNext = { step++ })

            1 ->
                AuthChoiceStep(
                    isGuest = isGuest,
                    onRequireAuth = onRequireAuth,
                    onNext = { step++ },
                )

            2 ->
                PinStep(
                    pinLength = 4,
                    onCompleted = { enteredPin ->
                        pin = enteredPin
                        step++
                    },
                )

            in questionStartIndex until questionEndIndex -> {
                val questionIndex = step - questionStartIndex
                val question = sampleOnboardingQuestions[questionIndex]
                QuestionStep(
                    question = question.text,
                    options = question.options,
                    onAnswer = { index ->
                        riskScore += index
                        step++
                    },
                )
            }

            questionEndIndex -> ResultStep(riskScore = riskScore, onFinish = onFinished)
        }

        // Progress Indicator (Optional visual)
        if (step in questionStartIndex until questionEndIndex) {
            val progress = (step - 2) / (sampleOnboardingQuestions.size + 1).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier =
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
        }
    }
}

@Composable
fun AuthChoiceStep(isGuest: Boolean, onRequireAuth: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.AccountBox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!isGuest) {
            Text(
                "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "You are signed in.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Continue")
            }
        } else {
            Text(
                "Create an Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Sign up to save your progress and access all features.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequireAuth,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Sign Up / Log In") }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text("Continue as Guest") }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.AccountBox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Welcome to Vita",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Your private, judgment-free space for sexual health.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Get Started")
        }
    }
}

@Composable
fun PinStep(pinLength: Int = 4, onCompleted: (String) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    /* auto-advance when full */
    LaunchedEffect(pin) {
        if (pin.length == pinLength) {
            onCompleted(pin)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Create a Privacy PIN",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "We'll ask for this whenever you open the app.",
            color = MaterialTheme.colorScheme.secondary,
        )

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
                                MaterialTheme.colorScheme
                                    .primary
                            } else {
                                MaterialTheme.colorScheme
                                    .surfaceVariant
                            },
                        ),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        /* Keypad */
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val rows =
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(null, "0", "⌫"), // null = spacer
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
                                haptic.performHapticFeedback(
                                    HapticFeedbackType
                                        .TextHandleMove,
                                )
                                if (isBackspace && pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                } else if (!isBackspace &&
                                    pin.length <
                                    pinLength
                                ) {
                                    pin += label
                                }
                            },
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
                },
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { tryAwaitRelease() },
                    onTap = { onClick() },
                )
            }
            .semantics { contentDescription = text },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color =
            if (text == "⌫") {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
fun QuestionStep(question: String, options: List<String>, onAnswer: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { onAnswer(index) },
                modifier =
                Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text(option, fontSize = 18.sp) }
        }
    }
}

@Composable
fun ResultStep(riskScore: Int, onFinish: () -> Unit) {
    // Mock Logic: Higher score = higher risk recommendation
    val recommendation = if (riskScore > 2) "Every 3 Months" else "Every 6 Months"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50), // Success Green
            modifier =
            Modifier.size(100.dp)
                .background(Color(0xFFE8F5E9), CircleShape)
                .padding(16.dp),
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
            colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Recommended Plan",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "HIV/STI Screening",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    recommendation,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Go to Dashboard")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }
    }
}
