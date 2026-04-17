package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val title: String
)

@Serializable
data class ArticleImage(
    val id: String,
    val pictureUrl: String,
    val caption: String? = null
)

@Serializable
data class Article(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val thumbnailUrl: String?,
    val publisher: String?,
    val publishedDate: Long?,

    val categories: List<Category> = emptyList(),
    val images: List<ArticleImage> = emptyList()
)


