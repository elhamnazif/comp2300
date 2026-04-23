package com.group8.comp2300.feature.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EmojiEventsW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.InfoW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun QuizScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: QuizViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val quiz = state.quiz

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(quiz?.title.orEmpty())
                        if (quiz != null && !state.showResults && !state.isError) {
                            Text(
                                text = stringResource(
                                    Res.string.education_quiz_progress_format,
                                    state.currentQuestionIndex + 1,
                                    quiz.questions.size,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.education_quiz_back_desc),
            )
        },
        bottomBar = {
            if (!state.isLoading && !state.isError && !state.showResults && quiz != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    tonalElevation = 6.dp,
                ) {
                    Button(
                        onClick = viewModel::nextQuestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = state.showFeedback,
                    ) {
                        Text(
                            text = if (state.currentQuestionIndex < quiz.questions.lastIndex) {
                                stringResource(Res.string.education_quiz_next_question)
                            } else {
                                stringResource(Res.string.education_quiz_see_results)
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 320.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading quiz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            state.isError || quiz == null -> {
                ErrorState(
                    title = stringResource(Res.string.education_quiz_loading_error),
                    onRetry = viewModel::retryLoad,
                    onClose = onBack,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            state.showResults -> {
                ResultsScreen(
                    quiz = quiz,
                    score = state.submissionResult?.attempt?.score ?: 0,
                    submissionError = state.submissionError,
                    isSubmitting = state.isSubmitting,
                    newlyAwardedBadges = state.submissionResult?.newlyAwardedBadges.orEmpty(),
                    perfectScores = state.progressStats?.totalPerfectScores ?: 0,
                    badgeCount = state.earnedBadges.size,
                    averageTimeSpentSeconds = state.progressStats?.averageTimeSpentSeconds ?: 0.0,
                    onRetrySubmit = viewModel::retrySubmission,
                    onRetake = viewModel::retakeQuiz,
                    onClose = onBack,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                QuizQuestionContent(
                    quiz = quiz,
                    state = state,
                    onOptionClick = viewModel::selectOption,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun QuizQuestionContent(
    quiz: Quiz,
    state: QuizViewModel.State,
    onOptionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentQuestion = state.currentQuestion ?: return
    val progress = (state.currentQuestionIndex + 1).toFloat() / quiz.questions.size.toFloat()

    Column(modifier = modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = currentQuestion.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp),
                )
            }

            currentQuestion.options.forEach { option ->
                OptionCard(
                    optionText = option.text,
                    optionId = option.id,
                    selectedOptionId = state.selectedOptionId,
                    answersLocked = state.showFeedback,
                    onClick = { onOptionClick(option.id) },
                )
            }

            if (state.showFeedback) {
                FeedbackPanel(
                    explanation = currentQuestion.explanation,
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    optionText: String,
    optionId: String,
    selectedOptionId: String?,
    answersLocked: Boolean,
    onClick: () -> Unit,
) {
    val isSelected = optionId == selectedOptionId

    Card(
        onClick = onClick,
        enabled = !answersLocked,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = BorderStroke(
            width = if (isSelected) 1.dp else 0.dp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FeedbackPanel(explanation: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.InfoW400Outlinedfill1,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.education_quiz_feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultsScreen(
    quiz: Quiz,
    score: Int,
    submissionError: String?,
    isSubmitting: Boolean,
    newlyAwardedBadges: List<String>,
    perfectScores: Long,
    badgeCount: Int,
    averageTimeSpentSeconds: Double,
    onRetrySubmit: () -> Unit,
    onRetake: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percentage = if (quiz.questions.isNotEmpty()) {
        (score.toFloat() / quiz.questions.size * 100).toInt()
    } else {
        0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ResultHeroCard(
            percentage = percentage,
            score = score,
            totalQuestions = quiz.questions.size,
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.education_quiz_feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = when {
                        percentage >= 80 -> stringResource(
                            Res.string.education_quiz_feedback_excellent,
                            quiz.title.lowercase(),
                        )

                        percentage >= 60 -> stringResource(Res.string.education_quiz_feedback_good)

                        else -> stringResource(Res.string.education_quiz_feedback_low)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (newlyAwardedBadges.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(Res.string.education_quiz_new_badges),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            newlyAwardedBadges.forEach { badgeName ->
                                AssistChip(
                                    onClick = {},
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.EmojiEventsW400Outlinedfill1,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    label = { Text(badgeName.replace('_', ' ')) },
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.education_badges_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ProgressSnapshotRow(
                    label = stringResource(Res.string.education_progress_perfect_scores),
                    value = perfectScores.toString(),
                )
                ProgressSnapshotRow(
                    label = stringResource(Res.string.education_progress_badges),
                    value = badgeCount.toString(),
                )
                ProgressSnapshotRow(
                    label = stringResource(Res.string.education_progress_average_time),
                    value = "${averageTimeSpentSeconds.toInt()}s",
                )
            }
        }

        if (isSubmitting || submissionError != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (isSubmitting) {
                            stringResource(Res.string.education_quiz_submitting)
                        } else {
                            submissionError.orEmpty()
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isSubmitting) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = onRetrySubmit,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.education_quiz_retry_submit))
                        }
                    }
                }
            }
        }

        Button(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.education_quiz_retake_button))
        }
        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.education_quiz_close_button))
        }
    }
}

@Composable
private fun ResultHeroCard(percentage: Int, score: Int, totalQuestions: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.EmojiEventsW400Outlinedfill1,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = when {
                    percentage >= 80 -> stringResource(Res.string.education_quiz_result_excellent)
                    percentage >= 60 -> stringResource(Res.string.education_quiz_result_good)
                    else -> stringResource(Res.string.education_quiz_result_keep_learning)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.education_quiz_score_format, percentage),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.education_quiz_count_format, score, totalQuestions),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ProgressSnapshotRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ErrorState(title: String, onRetry: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(stringResource(Res.string.education_quiz_retry_load))
            }
            TextButton(onClick = onClose) {
                Text(stringResource(Res.string.education_quiz_close_button))
            }
        }
    }
}
