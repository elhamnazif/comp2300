package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.config.Environment
import com.group8.comp2300.data.repository.*
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.mock.*

fun createServerDatabase(databasePath: String = Environment.dbPath): ServerDatabase {
    val driver = JdbcSqliteDriver(url = databasePath, schema = ServerDatabase.Schema)
    ensureOrderTables(driver)
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

internal fun ensureOrderTables(driver: SqlDriver) {
    driver.execute(
        null,
        """
        CREATE TABLE IF NOT EXISTS ShopOrderEntity (
            id TEXT NOT NULL PRIMARY KEY,
            user_id TEXT NOT NULL REFERENCES UserEntity(id) ON DELETE CASCADE,
            status TEXT NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
            subtotal REAL NOT NULL,
            tax REAL NOT NULL DEFAULT 0,
            shipping REAL NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            shipping_address TEXT
        )
        """.trimIndent(),
        0,
    )
    driver.execute(
        null,
        """
        CREATE TABLE IF NOT EXISTS ShopOrderItemEntity (
            order_id TEXT NOT NULL REFERENCES ShopOrderEntity(id) ON DELETE CASCADE,
            product_id TEXT NOT NULL REFERENCES ProductEntity(id) ON DELETE RESTRICT,
            quantity INTEGER NOT NULL,
            price_at_add REAL NOT NULL,
            PRIMARY KEY (order_id, product_id)
        )
        """.trimIndent(),
        0,
    )
}
