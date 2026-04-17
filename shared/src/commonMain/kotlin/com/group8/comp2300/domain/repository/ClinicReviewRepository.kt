package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Review
import com.group8.comp2300.domain.model.medical.CreateReviewRequest
import com.group8.comp2300.domain.model.medical.UpdateReviewRequest
import com.group8.comp2300.domain.model.medical.ReviewSortBy

interface ClinicReviewRepository {  // Renamed from ReviewRepository

    //Submit a new review
    //@return The created review with generated ID and timestamps
    suspend fun createReview(request: CreateReviewRequest): Result<Review>

    //Get all reviews for a specific clinic
    suspend fun getReviewsByClinicId(
        clinicId: String,
        sortBy: ReviewSortBy = ReviewSortBy.MOST_RECENT
    ): Result<List<Review>>

    //Search reviews by keyword for a specific clinic
    suspend fun searchReviewsByClinicId(
        clinicId: String,
        keyword: String,
        sortBy: ReviewSortBy = ReviewSortBy.MOST_RECENT
    ): Result<List<Review>>

    //Get a single review by ID
    suspend fun getReviewById(reviewId: String): Result<Review>

    //Update an existing review (only by the author)
    suspend fun updateReview(request: UpdateReviewRequest, userId: String): Result<Review>

    //Delete a review (only by author or admin)
    suspend fun deleteReview(reviewId: String, userId: String): Result<Boolean>

    //Mark a review as helpful (vote)
    suspend fun markHelpful(reviewId: String, userId: String): Result<Boolean>

    //Get average rating for a clinic
    suspend fun getAverageRating(clinicId: String): Result<Double>
}