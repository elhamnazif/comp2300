package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.Category

interface ContentCategoryRepository {
    fun getAllCategories(): List<Category>
    fun getCategoryById(id: String): Category?
    fun getCategoriesForArticle(articleId: String): List<Category>
    fun upsertCategory(category: Category)
    fun deleteCategory(id: String)
    fun getArticleCountForCategory(categoryId: String): Long
}
