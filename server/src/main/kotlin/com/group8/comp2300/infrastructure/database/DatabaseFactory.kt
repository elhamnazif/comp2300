package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.config.Environment
import com.group8.comp2300.data.repository.*
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.mock.*

fun createServerDatabase(databasePath: String = Environment.dbPath): ServerDatabase {
    val driver = JdbcSqliteDriver(url = databasePath, schema = ServerDatabase.Schema)
    val database = ServerDatabase(driver)
    seedProducts(database)
    seedCategories(database)
    seedArticles(database)
    seedQuizzes(database)
    seedBadges(database)
    return database
}

private fun seedProducts(database: ServerDatabase) {
    val repository = ProductRepositoryImpl(database)
    if (database.productQueries.selectAllProducts().executeAsList().isEmpty()) {
        sampleProducts.forEach { repository.insert(it) }
    } else {
        sampleProducts.forEach { sample ->
            val existing = repository.getById(sample.id) ?: return@forEach
            if (existing.imageUrl.isNullOrBlank() && !sample.imageUrl.isNullOrBlank()) {
                repository.update(existing.copy(imageUrl = sample.imageUrl))
            }
        }
    }
}

private fun seedBadges(database: ServerDatabase) {
    if (database.badgeQueries.getAllBadges().executeAsList().isEmpty()) {
        val repository = BadgeRepositoryImpl(database)
        database.transaction {
            sampleBadges.forEach { repository.saveBadge(it) }
        }
    }
}

private fun seedCategories(database: ServerDatabase) {
    if (database.contentCategoryQueries.getAllCategories().executeAsList().isEmpty()) {
        val repository = ContentCategoryRepositoryImpl(database)
        database.transaction {
            ContentCategory.all.forEach { category ->
                repository.upsertCategory(category)
            }
        }
    }
}

private fun seedArticles(database: ServerDatabase) {
    if (database.articleQueries.getAllArticles().executeAsList().isEmpty()) {
        val repository = ArticleRepositoryImpl(database)
        database.transaction {
            allArticles.forEach { repository.upsertArticle(it) }
        }
    }
}

private fun seedQuizzes(database: ServerDatabase) {
    if (database.quizQueries.getAllQuizzes().executeAsList().isEmpty()) {
        val repository = QuizRepositoryImpl(database)
        database.transaction {
            allQuizzes.forEach { repository.upsertQuiz(it) }
        }
    }
}
