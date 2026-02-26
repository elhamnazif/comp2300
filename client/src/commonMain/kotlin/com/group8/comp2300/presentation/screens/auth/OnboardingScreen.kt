@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group8.comp2300.mock.sampleOnboardingQuestions
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AccountBoxW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowForwardW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    isGuest: Boolean = true,
    onRequireAuth: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) } // 0: Welcome, 1: Auth, 2: PIN, 3+: Questions, Last: Result
    var pin by remember { mutableStateOf("") }
    var riskScore by remember { mutableIntStateOf(0) }

    val questionStartIndex = 3
    val questionEndIndex = questionStartIndex + sampleOnboardingQuestions.size

    // Animated progress
    // Step 2 is now the unified PIN step.
    val targetProgress = when {
        step < questionStartIndex -> 0f
        step >= questionEndIndex -> 1f
        else -> (step - questionStartIndex + 1).toFloat() / (sampleOnboardingQuestions.size + 1)
    }
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "OnboardingProgress")

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
    ) {
        // --- Header Area ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(Modifier.size(48.dp)) {
                if (step in 1..<questionEndIndex) {
                    IconButton(
                        onClick = {
                            if (step == 2) {
                                pin = ""
                            }
                            step--
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.ArrowBackW400Outlinedfill1,
                            contentDescription = stringResource(Res.string.onboarding_back)
                        )
                    }
                }
            }

            // Middle Area (Progress Bar & Step Label)
            if (step in questionStartIndex until questionEndIndex) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Text(
                    text = "${step - questionStartIndex + 1}/${sampleOnboardingQuestions.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Skip Button (Right side of middle area or separate?)
                // User said "put skip in top right", so it should be at the absolute end.
                TextButton(
                    onClick = { step++ }
                ) {
                    Text(stringResource(Res.string.onboarding_skip))
                }
            } else {
                // Empty space if not in questionnaire
                Spacer(Modifier.weight(1f))
            }
        }

        // --- Main Content Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "OnboardingStepTransition",
                modifier = Modifier.fillMaxSize()
            ) { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep({ step++ })

                1 -> AuthChoiceStep(
                    isGuest = isGuest,
                    onRequireAuth = onRequireAuth,
                    onNext = { step++ }
                )

                2 -> PinScreen(
                    onComplete = { finalPin ->
                        pin = finalPin
                        step++ // Moves to questionStartIndex (3)
                    }
                )

                in questionStartIndex until questionEndIndex -> {
                    val questionIndex = currentStep - questionStartIndex
                    val question = sampleOnboardingQuestions[questionIndex]

                    val questionText = when (question.id) {
                        1 -> stringResource(Res.string.onboarding_q1_text)
                        2 -> stringResource(Res.string.onboarding_q2_text)
                        else -> question.text
                    }

                    val localizedOptions = when (question.id) {
                        1 -> listOf(
                            stringResource(Res.string.onboarding_q1_op1),
                            stringResource(Res.string.onboarding_q1_op2),
                            stringResource(Res.string.onboarding_q1_op3),
                            stringResource(Res.string.onboarding_q1_op4)
                        )
                        2 -> listOf(
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

                questionEndIndex -> ResultStep(
                    riskScore = riskScore,
                    onFinish = onFinish
                )
                }
            }
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

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
