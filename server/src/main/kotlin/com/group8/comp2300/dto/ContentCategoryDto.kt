package com.group8.comp2300.dto

import com.group8.comp2300.dto.ArticleSummaryResponse
import kotlinx.serialization.Serializable

/**
 * For the main "Browse" or "Explore" screen.
 */
@Serializable
data class ContentCategoryResponse (
    val id: String,
    val title: String,
    val articleCount: Long? = null // e.g. to show "Nutrition (12)"
    )

/**
 * for dedicated header for a category
 */
@Serializable
data class CategoryDetailResponse(
    val id: String,
    val title: String,
    val articles: List<ArticleSummaryResponse>
)

