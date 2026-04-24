package com.group8.comp2300.domain.model.quiz

import com.group8.comp2300.domain.model.quiz.EvaluatedAnswer

data class QuizResultState (
    val totalPossibleScore: Int = 0,
    val pointsEarned: Int = 0,
    val totalQuestions: Int = 0,
    val correctAnswersCount: Int = 0,
    val evaluatedAnswers: List<EvaluatedAnswer> = emptyList(),

    // review system
    val isReviewModeActive: Boolean = false,
    val currentReviewIndex: Int = 0
) {
    val currentReviewItem: EvaluatedAnswer?
        get() = evaluatedAnswers.getOrNull(currentReviewIndex)

    val hasNextReview: Boolean
        get() = currentReviewIndex < evaluatedAnswers.size - 1

    val hasPreviousReview: Boolean
        get() = currentReviewIndex > 0
}


