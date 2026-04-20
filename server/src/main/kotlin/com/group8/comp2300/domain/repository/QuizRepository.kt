package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.Quiz

interface QuizRepository {
    fun getQuizById(id: String): Quiz?
    fun getQuizByArticleId(articleId: String): Quiz?
    fun getAllQuizzes(): List<Quiz>
    fun upsertQuiz(quiz: Quiz)
    fun deleteQuiz(id: String)
}
