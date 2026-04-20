package com.group8.comp2300.mapper

import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.education.QuizOption
import com.group8.comp2300.domain.model.education.QuizQuestion
import com.group8.comp2300.dto.OptionResponse
import com.group8.comp2300.dto.QuestionResponse
import com.group8.comp2300.dto.QuizResponse

object QuizMapper {

    fun toResponse(quiz: Quiz): QuizResponse = QuizResponse(
        id = quiz.id,
        title = quiz.title,
        questions = quiz.questions.map { it.toResponse() },
    )

    private fun QuizQuestion.toResponse(): QuestionResponse = QuestionResponse(
        id = id,
        title = title,
        explanation = explanation,
        options = options.map { it.toResponse() },
    )

    private fun QuizOption.toResponse(): OptionResponse = OptionResponse(
        id = id,
        text = text,
    )
}
