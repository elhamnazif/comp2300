package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.ArticleEnt
import com.group8.comp2300.domain.model.education.Article
import com.group8.comp2300.domain.model.education.ArticleImage
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.repository.ArticleRepository

class ArticleRepositoryImpl(private val database: ServerDatabase) : ArticleRepository {

    private val articleQueries = database.articleQueries
    private val articleCategoryQueries = database.articleCategoryQueries
    private val imageQueries = database.articleImageQueries

    override fun getArticleById(id: String): Article? = articleQueries.getArticleById(id).executeAsOneOrNull()?.let {
        mapToDomain(it)
    }

    override fun getAllArticles(): List<Article> = articleQueries.getAllArticles().executeAsList().map {
        mapToDomain(it)
    }

    override fun getArticlesPaginated(page: Int, pageSize: Int): List<Article> {
        val limit = pageSize.toLong()
        val offset = ((page - 1) * pageSize).toLong()
        return articleQueries.getArticlesPaginated(limit, offset).executeAsList().map { mapToDomain(it) }
    }

    override fun searchArticles(query: String): List<Article> =
        articleQueries.searchArticles(query).executeAsList().map {
            mapToDomain(it)
        }

    override fun getArticlesByCategory(categoryId: String): List<Article> =
        articleCategoryQueries.getArticlesByCategory(categoryId).executeAsList().map {
            mapToDomain(it)
        }

    override fun upsertArticle(article: Article) {
        database.transaction {
            articleQueries.upsertArticle(
                id = article.id,
                title = article.title,
                description = article.description,
                content = article.content,
                thumbnailUrl = article.thumbnailUrl,
                publisher = article.publisher,
                publishedDate = article.publishedDate,
            )

            // Refresh Images
            imageQueries.deleteImagesByArticleId(article.id)
            article.images.forEach { img ->
                imageQueries.insertArticleImage(img.id, article.id, img.pictureUrl, img.caption)
            }

            // Refresh Category Junctions
            articleCategoryQueries.deleteCategoriesByArticleId(article.id)
            article.categories.forEach { cat ->
                articleCategoryQueries.insert(cat.id, article.id)
            }
        }
    }

    override fun addCategoryToArticle(articleId: String, categoryId: String) {
        articleCategoryQueries.insert(
            content_category_id = categoryId,
            article_id = articleId,
        )
    }

    override fun removeCategoryFromArticle(articleId: String, categoryId: String) {
        articleQueries.removeCategoryFromArticle(
            articleId = articleId,
            categoryId = categoryId,
        )
    }

    override fun deleteArticle(id: String) {
        articleQueries.deleteArticle(id)
    }

    private fun mapToDomain(entity: ArticleEnt): Article {
        val categories = database.articleCategoryQueries.getCategoriesForArticle(entity.id).executeAsList()
        val images = imageQueries.getImagesByArticleId(entity.id).executeAsList()

        return Article(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            content = entity.content,
            thumbnailUrl = entity.thumbnail_url,
            publisher = entity.publisher,
            publishedDate = entity.published_date,
            categories = categories.map { Category(it.id, it.title) },
            images = images.map { ArticleImage(it.id, it.picture_url, it.caption) },
        )
    }
}
