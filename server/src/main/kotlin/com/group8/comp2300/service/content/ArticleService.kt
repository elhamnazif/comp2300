package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.education.*
import com.group8.comp2300.domain.repository.ArticleRepository
import com.group8.comp2300.domain.repository.ContentCategoryRepository
import com.group8.comp2300.domain.repository.QuizRepository
import com.group8.comp2300.dto.ArticleDetailResponse
import com.group8.comp2300.dto.ArticleSummaryResponse
import com.group8.comp2300.mapper.ArticleMapper

class ArticleService(
    private val articleRepository: ArticleRepository,
    private val quizRepository: QuizRepository,
    private val categoryRepository: ContentCategoryRepository,
    private val articleMapper: ArticleMapper // Injected via Koin
) {

    fun getAllArticles(): List<ArticleSummaryResponse> {
        return articleRepository
            .getAllArticles()
            .map { articleMapper.toSummary(it) } // Use the instance variable
    }

    fun getArticleById(id: String): ArticleDetailResponse? {
        val article = articleRepository.getArticleById(id) ?: return null
        val quiz = quizRepository.getQuizByArticleId(id)

        return articleMapper.toDetail(article, quiz)
    }
}
