package com.group8.comp2300.mapper

import com.group8.comp2300.domain.model.education.Article
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.dto.ArticleDetailResponse
import com.group8.comp2300.dto.ArticleSummaryResponse

class ArticleMapper(private val categoryMapper: CategoryMapper) {

    fun toSummary(article: Article): ArticleSummaryResponse = ArticleSummaryResponse(
        id = article.id,
        title = article.title,
        description = article.description,
        thumbnailUrl = article.thumbnailUrl,
        publisher = article.publisher,
        publishedDate = article.publishedDate,
        categories = article.categories.map { categoryMapper.toResponse(it) },
    )

    fun toDetail(article: Article, quiz: Quiz?): ArticleDetailResponse = ArticleDetailResponse(
        id = article.id,
        title = article.title,
        description = article.description,
        content = article.content,
        thumbnailUrl = article.thumbnailUrl,
        publisher = article.publisher,
        publishedDate = article.publishedDate,
        categories = article.categories.map { categoryMapper.toResponse(it) },
        quiz = quiz?.let { QuizMapper.toResponse(it) },
    )
}
