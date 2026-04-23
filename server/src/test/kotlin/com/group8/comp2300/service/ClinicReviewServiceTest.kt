package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.CreateReviewRequest
import com.group8.comp2300.domain.model.medical.Review
import com.group8.comp2300.domain.model.medical.ReviewSortBy
import com.group8.comp2300.domain.model.medical.UpdateReviewRequest
import com.group8.comp2300.domain.repository.ClinicReviewRepository
import com.group8.comp2300.services.ClinicReviewService
import kotlin.test.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// In-memory test repository
@OptIn(ExperimentalUuidApi::class)
class TestClinicReviewRepository : ClinicReviewRepository {

    private val reviews = mutableMapOf<String, Review>()
    private val helpfulVotes = mutableSetOf<String>()

    override suspend fun createReview(request: CreateReviewRequest): Result<Review> {
        val now = System.currentTimeMillis()
        val review = Review(
            id = Uuid.random().toString(),
            clinicId = request.clinicId,
            userId = request.userId,
            userName = request.userName,
            rating = request.rating,
            title = request.title,
            comment = request.comment,
            createdAt = now,
            updatedAt = now,
            images = request.images,
        )
        reviews[review.id] = review
        return Result.success(review)
    }

    override suspend fun getReviewsByClinicId(clinicId: String, sortBy: ReviewSortBy): Result<List<Review>> {
        val clinicReviews = reviews.values.filter { it.clinicId == clinicId }
        val sorted = when (sortBy) {
            ReviewSortBy.MOST_RECENT -> clinicReviews.sortedByDescending { it.createdAt }
            ReviewSortBy.HIGHEST_RATED -> clinicReviews.sortedByDescending { it.rating }
            ReviewSortBy.LOWEST_RATED -> clinicReviews.sortedBy { it.rating }
            ReviewSortBy.MOST_HELPFUL -> clinicReviews.sortedByDescending { it.helpfulCount }
        }
        return Result.success(sorted)
    }

    override suspend fun searchReviewsByClinicId(
        clinicId: String,
        keyword: String,
        sortBy: ReviewSortBy,
    ): Result<List<Review>> {
        val clinicReviews = reviews.values.filter { review ->
            review.clinicId == clinicId &&
                (
                    review.title.contains(keyword, ignoreCase = true) ||
                        review.comment.contains(keyword, ignoreCase = true)
                    )
        }
        val sorted = when (sortBy) {
            ReviewSortBy.MOST_RECENT -> clinicReviews.sortedByDescending { it.createdAt }
            ReviewSortBy.HIGHEST_RATED -> clinicReviews.sortedByDescending { it.rating }
            ReviewSortBy.LOWEST_RATED -> clinicReviews.sortedBy { it.rating }
            ReviewSortBy.MOST_HELPFUL -> clinicReviews.sortedByDescending { it.helpfulCount }
        }
        return Result.success(sorted)
    }

    override suspend fun getReviewById(reviewId: String): Result<Review> {
        val review = reviews[reviewId]
        return if (review != null) {
            Result.success(review)
        } else {
            Result.failure(NoSuchElementException("Review not found"))
        }
    }

    override suspend fun updateReview(request: UpdateReviewRequest, userId: String): Result<Review> {
        val existing = reviews[request.reviewId]
        if (existing == null) {
            return Result.failure(NoSuchElementException("Review not found"))
        }
        if (existing.userId != userId) {
            return Result.failure(IllegalStateException("Cannot update another user's review"))
        }
        val updated = existing.copy(
            rating = request.rating ?: existing.rating,
            title = request.title ?: existing.title,
            comment = request.comment ?: existing.comment,
            updatedAt = System.currentTimeMillis(),
        )
        reviews[request.reviewId] = updated
        return Result.success(updated)
    }

    override suspend fun deleteReview(reviewId: String, userId: String): Result<Boolean> {
        val existing = reviews[reviewId]
        if (existing == null) {
            return Result.failure(NoSuchElementException("Review not found"))
        }
        if (existing.userId != userId) {
            return Result.failure(IllegalStateException("Cannot delete another user's review"))
        }
        reviews.remove(reviewId)
        return Result.success(true)
    }

    override suspend fun markHelpful(reviewId: String, userId: String): Result<Boolean> {
        val voteKey = "$reviewId:$userId"
        if (helpfulVotes.contains(voteKey)) {
            return Result.failure(IllegalStateException("Already marked helpful"))
        }
        val review = reviews[reviewId] ?: return Result.failure(NoSuchElementException("Review not found"))
        helpfulVotes.add(voteKey)
        val updated = review.copy(helpfulCount = review.helpfulCount + 1)
        reviews[reviewId] = updated
        return Result.success(true)
    }

    override suspend fun getAverageRating(clinicId: String): Result<Double> {
        val clinicReviews = reviews.values.filter { it.clinicId == clinicId }
        if (clinicReviews.isEmpty()) return Result.success(0.0)
        val average = clinicReviews.map { it.rating }.average()
        return Result.success(average)
    }
}

class ClinicReviewServiceTest {

    private lateinit var repository: TestClinicReviewRepository
    private lateinit var service: ClinicReviewService

    @BeforeTest
    fun setup() {
        repository = TestClinicReviewRepository()
        service = ClinicReviewService(repository)
    }

    // ========== User Story 30 Tests ==========

    @Test
    fun testSubmitValidReview() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 5,
            title = "Great Clinic!",
            comment = "Very professional staff.",
            images = emptyList(),
        )

        val result = service.submitReview(request)

        assertTrue(result.isSuccess)
        val review = assertNotNull(result.getOrNull())
        assertEquals(5, review.rating)
        assertEquals("Great Clinic!", review.title)
    }

    @Test
    fun testSubmitReviewWithInvalidRating() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 6,
            title = "Great",
            comment = "Awesome",
            images = emptyList(),
        )

        val result = service.submitReview(request)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testSubmitReviewWithEmptyComment() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 4,
            title = "Test",
            comment = "",
            images = emptyList(),
        )

        val result = service.submitReview(request)

        assertTrue(result.isFailure)
        assertEquals("Comment cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateOwnReview() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 3,
            title = "Okay",
            comment = "It was fine",
            images = emptyList(),
        )
        val submitResult = service.submitReview(request)
        val review = submitResult.getOrNull()!!

        val updateResult = service.updateReview(
            reviewId = review.id,
            userId = "user456",
            rating = 5,
            title = "Excellent!",
            comment = "Actually it was amazing!",
        )

        assertTrue(updateResult.isSuccess)
        val updated = updateResult.getOrNull()
        assertEquals(5, updated?.rating)
        assertEquals("Excellent!", updated?.title)
    }

    @Test
    fun testCannotUpdateOtherUsersReview() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 4,
            title = "Good",
            comment = "Pretty good",
            images = emptyList(),
        )
        val submitResult = service.submitReview(request)
        val review = submitResult.getOrNull()!!

        val updateResult = service.updateReview(
            reviewId = review.id,
            userId = "hacker123",
            rating = 1,
            title = "Bad",
            comment = "Terrible!",
        )

        assertTrue(updateResult.isFailure)
    }

    @Test
    fun testDeleteOwnReview() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 4,
            title = "Good",
            comment = "Pretty good",
            images = emptyList(),
        )
        val submitResult = service.submitReview(request)
        val review = submitResult.getOrNull()!!

        val deleteResult = service.deleteReview(review.id, "user456")

        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun testMarkReviewHelpful() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user456",
            userName = "John Doe",
            rating = 5,
            title = "Great!",
            comment = "Wonderful experience",
            images = emptyList(),
        )
        val submitResult = service.submitReview(request)
        val review = submitResult.getOrNull()!!

        val result = service.markReviewHelpful(review.id, "otherUser789")

        assertTrue(result.isSuccess)
    }

    @Test
    fun testGetAverageRating() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "Excellent",
            comment = "Great!",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 3,
            title = "Okay",
            comment = "Average",
            images = emptyList(),
        )

        service.submitReview(request1)
        service.submitReview(request2)

        val result = service.getClinicRating("clinic123")

        assertTrue(result.isSuccess)
        assertEquals(4.0, result.getOrNull())
    }

    // ========== User Story 29 Tests ==========

    @Test
    fun testSortByMostRecent() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "First",
            comment = "First review",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 4,
            title = "Second",
            comment = "Second review",
            images = emptyList(),
        )

        service.submitReview(request1)
        Thread.sleep(10)
        service.submitReview(request2)

        val result = service.getClinicReviews("clinic123", ReviewSortBy.MOST_RECENT)

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(2, reviews?.size)
        assertTrue(reviews!![0].createdAt >= reviews[1].createdAt)
    }

    @Test
    fun testSortByHighestRated() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 3,
            title = "Average",
            comment = "Okay",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 5,
            title = "Excellent",
            comment = "Amazing!",
            images = emptyList(),
        )

        service.submitReview(request1)
        service.submitReview(request2)

        val result = service.getClinicReviews("clinic123", ReviewSortBy.HIGHEST_RATED)

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(5, reviews!![0].rating)
        assertEquals(3, reviews[1].rating)
    }

    @Test
    fun testSortByLowestRated() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "Excellent",
            comment = "Amazing!",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 2,
            title = "Poor",
            comment = "Disappointed",
            images = emptyList(),
        )

        service.submitReview(request1)
        service.submitReview(request2)

        val result = service.getClinicReviews("clinic123", ReviewSortBy.LOWEST_RATED)

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(2, reviews!![0].rating)
        assertEquals(5, reviews[1].rating)
    }

    @Test
    fun testSearchReviewsByKeyword() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "Amazing Experience",
            comment = "The staff was incredible!",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 3,
            title = "Average",
            comment = "Nothing special",
            images = emptyList(),
        )

        service.submitReview(request1)
        service.submitReview(request2)

        val result = service.searchClinicReviews("clinic123", "amazing", ReviewSortBy.MOST_RECENT)

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(1, reviews?.size)
        assertEquals("Amazing Experience", reviews!![0].title)
    }

    @Test
    fun testSearchWithNoResults() = runTest {
        val request = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "Great",
            comment = "Good experience",
            images = emptyList(),
        )

        service.submitReview(request)

        val result = service.searchClinicReviews("clinic123", "nonexistent", ReviewSortBy.MOST_RECENT)

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(0, reviews?.size)
    }

    @Test
    fun testFilterReviewsByMinRating() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "Excellent",
            comment = "Great!",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 3,
            title = "Average",
            comment = "Okay",
            images = emptyList(),
        )

        service.submitReview(request1)
        service.submitReview(request2)

        val result = service.getFilteredReviews(
            clinicId = "clinic123",
            sortBy = ReviewSortBy.HIGHEST_RATED,
            minRating = 4,
        )

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(1, reviews?.size)
        assertTrue(reviews!!.all { it.rating >= 4 })
    }

    @Test
    fun testFilterReviewsByKeywordAndRating() = runTest {
        val request1 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user1",
            userName = "User One",
            rating = 5,
            title = "Amazing Service",
            comment = "The staff was amazing!",
            images = emptyList(),
        )
        val request2 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user2",
            userName = "User Two",
            rating = 4,
            title = "Amazing Place",
            comment = "Great location",
            images = emptyList(),
        )
        val request3 = CreateReviewRequest(
            clinicId = "clinic123",
            userId = "user3",
            userName = "User Three",
            rating = 3,
            title = "Average",
            comment = "Nothing special",
            images = emptyList(),
        )

        service.submitReview(request1)
        service.submitReview(request2)
        service.submitReview(request3)

        val result = service.getFilteredReviews(
            clinicId = "clinic123",
            sortBy = ReviewSortBy.HIGHEST_RATED,
            minRating = 4,
            keyword = "amazing",
        )

        assertTrue(result.isSuccess)
        val reviews = result.getOrNull()
        assertEquals(2, reviews?.size)
        assertTrue(reviews!!.all { it.rating >= 4 })
    }
}

// Helper for running suspend functions in tests
fun runTest(block: suspend () -> Unit) {
    kotlinx.coroutines.runBlocking {
        block()
    }
}
