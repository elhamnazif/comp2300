package com.group8.comp2300.domain.model.quiz

import com.group8.comp2300.domain.model.quiz.EvaluatedAnswer
import com.group8.comp2300.domain.model.quiz.Question
import com.group8.comp2300.domain.model.quiz.UserAnswer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuizResultViewModel{

    private val _uiState = MutableStateFlow(QuizResultState())
    val uiState: StateFlow<QuizResultState> = _uiState.asStateFlow()

    fun initializeResults(questions: List<Question>, userAnswers: List<UserAnswer>) {
        val evaluatedList = questions.map { question ->
            val userAnswer = userAnswers.find { it.questionId == question.id }
            val isCorrect = userAnswer?.selectedOptionId == question.correctAnswerId

            EvaluatedAnswer(
                question = question,
                userSelectedOptionId = userAnswer?.selectedOptionId,
                isCorrect = isCorrect
            )
        }

        val totalPoints = questions.sumOf { it.points }
        val earnedPoints = evaluatedList.filter { it.isCorrect }.sumOf { it.question.points }
        val correctCount = evaluatedList.count { it.isCorrect }

        _uiState.update { currentState ->
            currentState.copy(
                totalPossibleScore = totalPoints,
                pointsEarned = earnedPoints,
                totalQuestions = questions.size,
                correctAnswersCount = correctCount,
                evaluatedAnswers = evaluatedList,
                isReviewModeActive = false,
                currentReviewIndex = 0
            )
        }
    }

    fun startReviewMode() {
        if (_uiState.value.evaluatedAnswers.isNotEmpty()) {
            _uiState.update { it.copy(isReviewModeActive = true, currentReviewIndex = 0) }
        }
    }

    fun exitReviewMode() {
        _uiState.update { it.copy(isReviewModeActive = false) }
    }

    fun nextReviewQuestion() {
        _uiState.update { currentState ->
            if (currentState.hasNextReview) {
                currentState.copy(currentReviewIndex = currentState.currentReviewIndex + 1)
            } else {
                currentState
            }
        }
    }
    fun previousReviewQuestion() {
        _uiState.update { currentState ->
            if (currentState.hasPreviousReview) {
                currentState.copy(currentReviewIndex = currentState.currentReviewIndex - 1)
            } else {
                currentState
            }
        }
    }
}



