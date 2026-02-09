package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val title: String,
    val content: String,
    val author: String? = null,
    val category: ContentCategory,
    val publishedAt: Long,
    val updatedAt: Long = publishedAt,
    val readTimeMinutes: Int = 5,
    val tags: List<String> = emptyList()
)
