package com.group8.comp2300.presentation.ui.screens.education

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.education.Quiz
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val CorrectGreen = Color(0xFF4CAF50) // Material green 500
private val CorrectOnGreen = Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(viewModel: EducationViewModel = koinViewModel(), quizId: String, onBack: () -> Unit) {
    val quiz = viewModel.getQuizById(quizId)
    if (quiz == null) {
        // Handle case where quiz is not found
        onBack()
        return
    }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswerIndex by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var correctAnswersCount by remember { mutableStateOf(0) }
    var showResults by remember { mutableStateOf(false) }

    val currentQuestion = quiz.questions.getOrNull(currentQuestionIndex)
    val progress = (currentQuestionIndex + 1).toFloat() / quiz.questions.size

    fun handleNextQuestion() {
        if (selectedAnswerIndex == currentQuestion?.correctAnswerIndex) correctAnswersCount++
        if (currentQuestionIndex < quiz.questions.size - 1) {
            currentQuestionIndex++
            selectedAnswerIndex = null
            showFeedback = false
        } else {
            showResults = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(quiz.title)
                        if (!showResults) {
                            Text(
                                stringResource(
                                    Res.string.education_quiz_progress_format,
                                    currentQuestionIndex + 1,
                                    quiz.questions.size,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.education_quiz_back_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (!showResults) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (showResults) {
                ResultsScreen(
                    quiz,
                    correctAnswersCount,
                    onRetake = {
                        currentQuestionIndex = 0
                        selectedAnswerIndex = null
                        showFeedback = false
                        correctAnswersCount = 0
                        showResults = false
                    },
                    onClose = onBack,
                )
            } else if (currentQuestion != null) {
                Column(
                    modifier =
                    Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                        CardDefaults.cardColors(
                            containerColor =
                            MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Text(
                            text = currentQuestion.question,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(20.dp),
                        )
                    }

                    currentQuestion.options.forEachIndexed { index, option ->
                        OptionCard(
                            option = option,
                            index = index,
                            selectedIndex = selectedAnswerIndex,
                            correctIndex =
                            if (showFeedback) {
                                currentQuestion.correctAnswerIndex
                            } else {
                                null
                            },
                            onClick = {
                                if (!showFeedback) {
                                    selectedAnswerIndex = index
                                    showFeedback = true
                                }
                            },
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(visible = showFeedback) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                            CardDefaults.cardColors(
                                containerColor =
                                if (selectedAnswerIndex ==
                                    currentQuestion
                                        .correctAnswerIndex
                                ) {
                                    CorrectGreen.copy(
                                        alpha = 0.9f,
                                    ) // theme-adaptive green
                                } else {
                                    MaterialTheme.colorScheme
                                        .errorContainer
                                },
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text =
                                    if (selectedAnswerIndex ==
                                        currentQuestion.correctAnswerIndex
                                    ) {
                                        stringResource(Res.string.education_quiz_correct)
                                    } else {
                                        stringResource(Res.string.education_quiz_incorrect)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color =
                                    if (selectedAnswerIndex ==
                                        currentQuestion.correctAnswerIndex
                                    ) {
                                        CorrectOnGreen
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentQuestion.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                    if (selectedAnswerIndex ==
                                        currentQuestion.correctAnswerIndex
                                    ) {
                                        CorrectOnGreen
                                    } else {
                                        Color.Unspecified
                                    },
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { handleNextQuestion() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = showFeedback,
                ) {
                    Text(
                        if (currentQuestionIndex < quiz.questions.size - 1) {
                            stringResource(Res.string.education_quiz_next_question)
                        } else {
                            stringResource(Res.string.education_quiz_see_results)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionCard(option: String, index: Int, selectedIndex: Int?, correctIndex: Int?, onClick: () -> Unit) {
    val isSelected = index == selectedIndex
    val isCorrect = index == correctIndex
    val showResult = correctIndex != null

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors =
        CardDefaults.cardColors(
            containerColor =
            when {
                showResult && isCorrect -> CorrectGreen.copy(alpha = 0.9f)

                showResult && isSelected && !isCorrect ->
                    MaterialTheme.colorScheme.errorContainer

                isSelected -> MaterialTheme.colorScheme.secondaryContainer

                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        border =
        BorderStroke(
            width = if (isSelected && !showResult) 2.dp else 0.dp,
            color =
            if (isSelected && !showResult) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (showResult && isCorrect) CorrectOnGreen else Color.Unspecified,
            )
            if (showResult && isCorrect) {
                Text(
                    text = "✓",
                    color = CorrectOnGreen,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ResultsScreen(quiz: Quiz, correctAnswersCount: Int, onRetake: () -> Unit, onClose: () -> Unit) {
    val percentage = (correctAnswersCount.toFloat() / quiz.questions.size * 100).toInt()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(100.dp),
            colors =
            CardDefaults.cardColors(
                containerColor =
                when {
                    percentage >= 80 -> CorrectGreen.copy(alpha = 0.9f)

                    percentage >= 60 ->
                        MaterialTheme.colorScheme.secondaryContainer

                    else -> MaterialTheme.colorScheme.errorContainer
                },
            ),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.education_quiz_score_format, percentage),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(
                            Res.string.education_quiz_count_format,
                            correctAnswersCount,
                            quiz.questions.size,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }

        Text(
            text =
            when {
                percentage >= 80 -> stringResource(Res.string.education_quiz_result_excellent)
                percentage >= 60 -> stringResource(Res.string.education_quiz_result_good)
                else -> stringResource(Res.string.education_quiz_result_keep_learning)
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.education_quiz_feedback_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text =
                    when {
                        percentage >= 80 ->
                            stringResource(Res.string.education_quiz_feedback_excellent, quiz.title.lowercase())

                        percentage >= 60 ->
                            stringResource(Res.string.education_quiz_feedback_good)

                        else ->
                            stringResource(Res.string.education_quiz_feedback_low)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Button(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.education_quiz_retake_button))
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.education_quiz_close_button))
        }
    }
}
