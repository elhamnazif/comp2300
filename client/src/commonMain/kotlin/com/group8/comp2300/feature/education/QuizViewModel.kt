package com.group8.comp2300.feature.education

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.education.*
import com.group8.comp2300.domain.repository.EducationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class QuizViewModel(
    private val repository: EducationRepository,
    private val refreshNotifier: EducationRefreshNotifier,
    private val quizId: String,
) : ViewModel() {
    private val mutableState = MutableStateFlow(State(isLoading = true))
    private var startedAtMs = Clock.System.now().toEpochMilliseconds()

    val state: StateFlow<State> = mutableState.asStateFlow()

    init {
        loadQuiz()
    }

    fun retryLoad() {
        loadQuiz()
    }

    fun selectOption(optionId: String) {
        val state = mutableState.value
        val question = state.currentQuestion ?: return
        if (state.showFeedback) return
        mutableState.value = state.copy(
            selectedOptionId = optionId,
            showFeedback = true,
            selectedAnswers = state.selectedAnswers + (question.id to optionId),
        )
    }

    fun nextQuestion() {
        val state = mutableState.value
        if (!state.showFeedback) return
        val quiz = state.quiz ?: return
        if (state.currentQuestionIndex < quiz.questions.lastIndex) {
            mutableState.value = state.copy(
                currentQuestionIndex = state.currentQuestionIndex + 1,
                selectedOptionId = state.selectedAnswers[quiz.questions[state.currentQuestionIndex + 1].id],
                showFeedback = false,
            )
        } else {
            submitQuiz()
        }
    }

    fun retakeQuiz() {
        startedAtMs = Clock.System.now().toEpochMilliseconds()
        val currentQuiz = mutableState.value.quiz
        mutableState.value = State(
            quiz = currentQuiz,
            isLoading = currentQuiz == null,
        )
    }

    fun retrySubmission() {
        if (mutableState.value.quiz != null) {
            submitQuiz()
        }
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            mutableState.value = State(isLoading = true)
            val quiz = repository.getQuizById(quizId)
            mutableState.value = if (quiz != null) {
                State(quiz = quiz)
            } else {
                State(isError = true)
            }
        }
    }

    private fun submitQuiz() {
        val currentState = mutableState.value
        val quiz = currentState.quiz ?: return
        val answers = quiz.questions.mapNotNull { question ->
            currentState.selectedAnswers[question.id]?.let { optionId ->
                UserQuizAnswerInput(questionId = question.id, selectedOptionId = optionId)
            }
        }
        val submittedAt = Clock.System.now().toEpochMilliseconds()

        viewModelScope.launch {
            mutableState.value = currentState.copy(
                isSubmitting = true,
                submissionError = null,
                showResults = true,
            )

            runCatching {
                val submission = repository.submitQuizAttempt(
                    quizId = quiz.id,
                    startedAt = startedAtMs,
                    submittedAt = submittedAt,
                    answers = answers,
                )
                submission to repository.getProgress()
            }.onSuccess { (submission, progress) ->
                refreshNotifier.requestRefresh()
                mutableState.value = currentState.copy(
                    showResults = true,
                    isSubmitting = false,
                    submissionError = null,
                    submissionResult = submission,
                    selectedAnswers = currentState.selectedAnswers,
                    progressStats = progress.stats,
                    earnedBadges = progress.earnedBadges,
                )
            }.onFailure {
                mutableState.value = currentState.copy(
                    showResults = true,
                    isSubmitting = false,
                    submissionError = it.message,
                    selectedAnswers = currentState.selectedAnswers,
                )
            }
        }
    }

    @Immutable
    data class State(
        val isLoading: Boolean = false,
        val isError: Boolean = false,
        val quiz: Quiz? = null,
        val currentQuestionIndex: Int = 0,
        val selectedOptionId: String? = null,
        val selectedAnswers: Map<String, String> = emptyMap(),
        val showFeedback: Boolean = false,
        val showResults: Boolean = false,
        val isSubmitting: Boolean = false,
        val submissionError: String? = null,
        val submissionResult: QuizSubmissionResult? = null,
        val progressStats: UserQuizStats? = null,
        val earnedBadges: List<EarnedBadge> = emptyList(),
    ) {
        val currentQuestion = quiz?.questions?.getOrNull(currentQuestionIndex)
    }
}
