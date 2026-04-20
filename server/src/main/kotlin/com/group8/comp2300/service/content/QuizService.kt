package com.group8.comp2300.service.content

import com.group8.comp2300.domain.repository.QuizRepository
import com.group8.comp2300.dto.QuizResponse
import com.group8.comp2300.mapper.QuizMapper

class QuizService(private val quizRepository: QuizRepository) {
    /**
     * Fetches all quizzes and maps them to DTOs.
     */
    fun getAllQuizzes(): List<QuizResponse> = quizRepository.getAllQuizzes().map { quiz ->
        QuizMapper.toResponse(quiz)
    }

    fun getQuizById(id: String): QuizResponse? {
        val quiz = quizRepository.getQuizById(id) ?: return null
        return QuizMapper.toResponse(quiz)
    }

    fun getQuizByArticleId(articleId: String): QuizResponse? {
        val quiz = quizRepository.getQuizByArticleId(articleId) ?: return null
        return QuizMapper.toResponse(quiz)
    }
}
