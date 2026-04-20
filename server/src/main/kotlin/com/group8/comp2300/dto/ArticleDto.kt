package com.group8.comp2300.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleSummaryResponse(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val publisher: String?,
    val publishedDate: Long?,
    val categories: List<ContentCategoryResponse>,
)

@Serializable
data class ArticleDetailResponse(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val thumbnailUrl: String?,
    val publisher: String?,
    val publishedDate: Long?,
    val categories: List<ContentCategoryResponse>,
    val quiz: QuizResponse? = null,
)
