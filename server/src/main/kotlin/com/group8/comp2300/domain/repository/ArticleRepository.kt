package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.Article

interface ArticleRepository {
    fun getArticleById(id: String): Article?
    fun getAllArticles(): List<Article>
    fun getArticlesPaginated(page: Int, pageSize: Int): List<Article>
    fun searchArticles(query: String): List<Article>
    fun getArticlesByCategory(categoryId: String): List<Article>
    fun upsertArticle(article: Article)
    fun deleteArticle(id: String)
    fun addCategoryToArticle(articleId: String, categoryId: String)
    fun removeCategoryFromArticle(articleId: String, categoryId: String)
}
