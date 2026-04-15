package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String,
    val clinicId: String,           // Which clinic this review is for
    val userId: String,             // Who wrote the review
    val userName: String,           // Display name (or anonymous)
    val rating: Int,                // 1-5 stars
    val title: String,              // Review title
    val comment: String,            // Written review
    val createdAt: Long,            // Unix timestamp
    val updatedAt: Long,            // Unix timestamp (for edits)
    val isModerated: Boolean = false,  // For future moderation
    val isApproved: Boolean = false,   // For future moderation
    val helpfulCount: Int = 0,      // How many found this helpful
    val images: List<String> = emptyList()  // Optional image URLs
)

@Serializable
data class CreateReviewRequest(
    val clinicId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val title: String,
    val comment: String,
    val images: List<String> = emptyList()
)

@Serializable
data class UpdateReviewRequest(
    val reviewId: String,
    val rating: Int? = null,
    val title: String? = null,
    val comment: String? = null
)

enum class ReviewSortBy {
    MOST_RECENT,
    HIGHEST_RATED,
    LOWEST_RATED,
    MOST_HELPFUL
}