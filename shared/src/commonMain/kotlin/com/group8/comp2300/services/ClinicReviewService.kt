package com.group8.comp2300.services

import com.group8.comp2300.domain.model.medical.CreateReviewRequest
import com.group8.comp2300.domain.model.medical.Review
import com.group8.comp2300.domain.model.medical.ReviewSortBy
import com.group8.comp2300.domain.model.medical.UpdateReviewRequest
import com.group8.comp2300.domain.repository.ClinicReviewRepository
import kotlin.uuid.ExperimentalUuidApi

class ClinicReviewService(private val repository: ClinicReviewRepository) {

    // Submit a new review
    @OptIn(ExperimentalUuidApi::class)
    suspend fun submitReview(request: CreateReviewRequest): Result<Review> {
        // Validation
        if (request.rating !in 1..5) {
            return Result.failure(IllegalArgumentException("Rating must be between 1 and 5"))
        }

        if (request.comment.isBlank()) {
            return Result.failure(IllegalArgumentException("Comment cannot be empty"))
        }

        if (request.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Title cannot be empty"))
        }

        return repository.createReview(request)
    }

    // Get reviews for a clinic
    suspend fun getClinicReviews(
        clinicId: String,
        sortBy: ReviewSortBy = ReviewSortBy.MOST_RECENT,
    ): Result<List<Review>> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be empty"))
        }

        return repository.getReviewsByClinicId(clinicId, sortBy)
    }

    // Get average rating for a clinic
    suspend fun getClinicRating(clinicId: String): Result<Double> = repository.getAverageRating(clinicId)

    // Update a review (author only)
    suspend fun updateReview(
        reviewId: String,
        userId: String,
        rating: Int?,
        title: String?,
        comment: String?,
    ): Result<Review> {
        // Validation
        rating?.let {
            if (it !in 1..5) {
                return Result.failure(IllegalArgumentException("Rating must be between 1 and 5"))
            }
        }

        title?.let {
            if (it.isBlank()) {
                return Result.failure(IllegalArgumentException("Title cannot be empty"))
            }
        }

        comment?.let {
            if (it.isBlank()) {
                return Result.failure(IllegalArgumentException("Comment cannot be empty"))
            }
        }

        val request = UpdateReviewRequest(reviewId, rating, title, comment)
        return repository.updateReview(request, userId)
    }

    // Delete a review
    suspend fun deleteReview(reviewId: String, userId: String): Result<Boolean> =
        repository.deleteReview(reviewId, userId)

    // Mark review as helpful
    suspend fun markReviewHelpful(reviewId: String, userId: String): Result<Boolean> =
        repository.markHelpful(reviewId, userId)

    // Search reviews by keyword with sorting
    suspend fun searchClinicReviews(
        clinicId: String,
        keyword: String,
        sortBy: ReviewSortBy = ReviewSortBy.MOST_RECENT,
    ): Result<List<Review>> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be empty"))
        }

        if (keyword.isBlank()) {
            return repository.getReviewsByClinicId(clinicId, sortBy)
        }

        return repository.searchReviewsByClinicId(clinicId, keyword, sortBy)
    }

    // Get reviews with multiple filter options (rating + keyword)
    suspend fun getFilteredReviews(
        clinicId: String,
        sortBy: ReviewSortBy = ReviewSortBy.MOST_RECENT,
        minRating: Int? = null,
        keyword: String? = null,
    ): Result<List<Review>> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be empty"))
        }

        val reviewsResult = if (!keyword.isNullOrBlank()) {
            repository.searchReviewsByClinicId(clinicId, keyword, sortBy)
        } else {
            repository.getReviewsByClinicId(clinicId, sortBy)
        }

        if (reviewsResult.isFailure) {
            return reviewsResult
        }

        var reviews = reviewsResult.getOrNull() ?: emptyList()

        if (minRating != null && minRating in 1..5) {
            reviews = reviews.filter { it.rating >= minRating }
        }

        return Result.success(reviews)
    }
}
