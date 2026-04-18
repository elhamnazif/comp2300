package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    val clinicId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val title: String,
    val comment: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isModerated: Boolean = false,
    val isApproved: Boolean = false,
    val helpfulCount: Int = 0,
    val images: List<String> = emptyList(),
)

@Serializable
data class CreateReviewRequest(
    val clinicId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val title: String,
    val comment: String,
    val images: List<String> = emptyList(),
)

@Serializable
data class UpdateReviewRequest(
    val reviewId: String,
    val rating: Int? = null,
    val title: String? = null,
    val comment: String? = null,
)

enum class ReviewSortBy {
    MOST_RECENT,
    HIGHEST_RATED,
    LOWEST_RATED,
    MOST_HELPFUL,
}
