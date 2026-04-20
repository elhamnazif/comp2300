package com.group8.comp2300.service.content

import com.group8.comp2300.domain.repository.ArticleRepository
import com.group8.comp2300.domain.repository.ContentCategoryRepository
import com.group8.comp2300.dto.ArticleSummaryResponse
import com.group8.comp2300.dto.ContentCategoryResponse
import com.group8.comp2300.mapper.ArticleMapper
import com.group8.comp2300.mapper.CategoryMapper

class ContentCategoryService(
    private val categoryRepository: ContentCategoryRepository,
    private val articleRepository: ArticleRepository,
    private val categoryMapper: CategoryMapper,
    private val articleMapper: ArticleMapper,
) {

    /**
     * Returns all categories.
     * The CategoryMapper handles the article count calculation internally.
     */
    fun getAllCategories(): List<ContentCategoryResponse> = categoryRepository.getAllCategories().map { category ->
        categoryMapper.toResponse(category)
    }

    /**
     * Fetches all articles belonging to a specific category.
     */
    fun getArticlesByCategory(categoryId: String): List<ArticleSummaryResponse> {
        val articles = articleRepository.getArticlesByCategory(categoryId)

        return articles.map { article ->
            articleMapper.toSummary(article)
        }
    }
}
