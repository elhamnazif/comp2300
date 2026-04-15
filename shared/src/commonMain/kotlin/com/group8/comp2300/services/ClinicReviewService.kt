package com.group8.comp2300.services

import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.ClinicReviewRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ClinicReviewService(  // Renamed class
    private val repository: ClinicReviewRepository
) {

    /**
     * Submit a new review
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun submitReview(request: CreateReviewRequest): Result<Review> {
        // Validation
        if (request.rating < 1 || request.rating > 5) {
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

    /**
     * Get reviews for a clinic
     */
    suspend fun getClinicReviews(
        clinicId: String,
        sortBy: ReviewSortBy = ReviewSortBy.MOST_RECENT
    ): Result<List<Review>> {
        if (clinicId.isBlank()) {
            return Result.failure(IllegalArgumentException("Clinic ID cannot be empty"))
        }

        return repository.getReviewsByClinicId(clinicId, sortBy)
    }

    /**
     * Get average rating for a clinic
     */
    suspend fun getClinicRating(clinicId: String): Result<Double> {
        return repository.getAverageRating(clinicId)
    }

    /**
     * Update a review (author only)
     */
    suspend fun updateReview(
        reviewId: String,
        userId: String,
        rating: Int?,
        title: String?,
        comment: String?
    ): Result<Review> {
        // Validation
        rating?.let {
            if (it < 1 || it > 5) {
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

    /**
     * Delete a review
     */
    suspend fun deleteReview(reviewId: String, userId: String): Result<Boolean> {
        return repository.deleteReview(reviewId, userId)
    }

    /**
     * Mark review as helpful
     */
    suspend fun markReviewHelpful(reviewId: String, userId: String): Result<Boolean> {
        return repository.markHelpful(reviewId, userId)
    }
}