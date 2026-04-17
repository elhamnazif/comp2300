package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.repository.ContentCategoryRepository
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.ContentCategoryEnt

class ContentCategoryRepositoryImpl(
    private val database: ServerDatabase
) : ContentCategoryRepository {

    private val categoryQueries = database.contentCategoryQueries

    override fun getAllCategories(): List<Category> {
        return categoryQueries.getAllCategories().executeAsList().map { it.toDomain() }
    }

    override fun getCategoryById(id: String): Category? {
        return categoryQueries.getCategoryById(id).executeAsOneOrNull()?.toDomain()
    }

    override fun getCategoriesForArticle(articleId: String): List<Category> {
        return database.articleCategoryQueries.getCategoriesForArticle(articleId).executeAsList().map { it.toDomain() }
    }

    override fun upsertCategory(category: Category) {
        categoryQueries.upsertCategory(id = category.id, title = category.title)
    }

    override fun deleteCategory(id: String) {
        categoryQueries.deleteCategory(id)
    }

    override fun getArticleCountForCategory(categoryId: String): Long {
        return database.articleCategoryQueries.getArticleCountForCategory(categoryId).executeAsOne()
    }


    private fun ContentCategoryEnt.toDomain(): Category {
        return Category(
            id = this.id,
            title = this.title
        )
    }
}
