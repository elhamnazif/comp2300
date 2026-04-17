package com.group8.comp2300.mapper

import com.group8.comp2300.domain.model.education.*
import com.group8.comp2300.dto.*

object QuizMapper {

    fun toResponse(quiz: Quiz): QuizResponse {
        return QuizResponse(
            id = quiz.id,
            title = quiz.title,
            questions = quiz.questions.map { it.toResponse() }
        )
    }

    private fun QuizQuestion.toResponse(): QuestionResponse {
        return QuestionResponse(
            id = id,
            title = title,
            explanation = explanation,
            options = options.map { it.toResponse() }
        )
    }

    private fun QuizOption.toResponse(): OptionResponse {
        return OptionResponse(
            id = id,
            text = text,
            isCorrect = isCorrect
        )
    }
}
