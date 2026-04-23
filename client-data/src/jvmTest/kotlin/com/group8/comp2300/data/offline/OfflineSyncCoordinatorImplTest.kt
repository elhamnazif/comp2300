package com.group8.comp2300.data.offline

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxItem
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.remote.ApiException
import com.group8.comp2300.data.repository.newDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineSyncCoordinatorImplTest {
    @Test
    fun syncNowLeavesRetryableFailurePendingAndSkipsCacheRefresh() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        val cacheRefresher = RecordingOfflineCacheRefresher()
        val coordinator = coordinator(
            outbox = outbox,
            cacheRefresher = cacheRefresher,
            handlers = listOf(
                object : OfflineMutationHandler {
                    override val type: String = TestMutationType

                    override suspend fun apply(item: OutboxItem): Unit = throw IllegalStateException("offline")
                },
            ),
        )

        val status = coordinator.syncNow()

        assertEquals(1, status.pendingCount)
        assertEquals(0, status.failedCount)
        assertFalse(status.cachesRefreshed)
        assertEquals(0, cacheRefresher.refreshCallCount)
    }

    @Test
    fun refreshCachesSkipsWhileFailedMutationExists() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        val failedItemId = outbox.getAll().single().id
        outbox.updateState(failedItemId, OutboxState.FAILED, "bad request")
        val cacheRefresher = RecordingOfflineCacheRefresher()
        val coordinator = coordinator(
            outbox = outbox,
            cacheRefresher = cacheRefresher,
            handlers = emptyList(),
        )

        val status = coordinator.refreshCaches()

        assertEquals(0, status.pendingCount)
        assertEquals(1, status.failedCount)
        assertFalse(status.cachesRefreshed)
        assertEquals(0, cacheRefresher.refreshCallCount)
    }

    @Test
    fun syncNowMovesNonRetryableFailureToFailed() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        val cacheRefresher = RecordingOfflineCacheRefresher()
        val coordinator = coordinator(
            outbox = outbox,
            cacheRefresher = cacheRefresher,
            handlers = listOf(
                object : OfflineMutationHandler {
                    override val type: String = TestMutationType

                    override suspend fun apply(item: OutboxItem): Unit = throw ApiException(400, "bad request")
                },
            ),
        )

        val status = coordinator.syncNow()

        assertEquals(0, status.pendingCount)
        assertEquals(1, status.failedCount)
        assertFalse(status.cachesRefreshed)
        assertEquals(0, cacheRefresher.refreshCallCount)
        assertEquals(OutboxState.FAILED, outbox.getAll().single().state)
    }

    @Test
    fun syncNowRethrowsAuthenticationFailure() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        val cacheRefresher = RecordingOfflineCacheRefresher()
        val coordinator = coordinator(
            outbox = outbox,
            cacheRefresher = cacheRefresher,
            handlers = listOf(
                object : OfflineMutationHandler {
                    override val type: String = TestMutationType

                    override suspend fun apply(item: OutboxItem): Unit =
                        throw ApiException(401, "Authentication failed")
                },
            ),
        )

        assertFailsWith<ApiException> { coordinator.syncNow() }
        assertEquals(1, outbox.getPending().size)
        assertEquals(0, cacheRefresher.refreshCallCount)
    }

    @Test
    fun retryFailedMutationResetsStateAndRefreshesAfterSuccess() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        val failedItemId = outbox.getAll().single().id
        outbox.updateState(failedItemId, OutboxState.FAILED, "bad request")
        val cacheRefresher = RecordingOfflineCacheRefresher()
        var applyCallCount = 0
        val coordinator = coordinator(
            outbox = outbox,
            cacheRefresher = cacheRefresher,
            handlers = listOf(
                object : OfflineMutationHandler {
                    override val type: String = TestMutationType

                    override suspend fun apply(item: OutboxItem) {
                        applyCallCount += 1
                    }
                },
            ),
        )

        val failedBeforeRetry = coordinator.listFailedMutations()
        val retryStatus = coordinator.retryFailedMutation(failedItemId)

        assertEquals(1, failedBeforeRetry.size)
        assertEquals("bad request", failedBeforeRetry.single().lastError)
        assertEquals(1, applyCallCount)
        assertTrue(outbox.getAll().isEmpty())
        assertTrue(retryStatus.cachesRefreshed)
        assertEquals(1, cacheRefresher.refreshCallCount)
    }

    @Test
    fun syncNowStillAttemptsWhenTokenManagerReportsExpired() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        var applyCallCount = 0
        val coordinator = OfflineSyncCoordinatorImpl(
            tokenManager = FakeTokenManager(userId = "user-1", tokenExpired = true),
            outbox = outbox,
            mutationHandlers = OfflineMutationHandlers(
                listOf(
                    object : OfflineMutationHandler {
                        override val type: String = TestMutationType

                        override suspend fun apply(item: OutboxItem) {
                            applyCallCount += 1
                        }
                    },
                ),
            ),
            cacheRefresher = RecordingOfflineCacheRefresher(),
        )

        val status = coordinator.syncNow()

        assertEquals(1, applyCallCount)
        assertEquals(0, status.pendingCount)
        assertEquals(0, status.failedCount)
        assertTrue(status.cachesRefreshed)
    }

    private fun coordinator(
        outbox: OutboxDataSource,
        cacheRefresher: RecordingOfflineCacheRefresher,
        handlers: List<OfflineMutationHandler>,
    ): OfflineSyncCoordinatorImpl = OfflineSyncCoordinatorImpl(
        tokenManager = FakeTokenManager(userId = "user-1"),
        outbox = outbox,
        mutationHandlers = OfflineMutationHandlers(handlers),
        cacheRefresher = cacheRefresher,
    )

    private class RecordingOfflineCacheRefresher : OfflineCacheRefresher {
        var refreshCallCount = 0

        override suspend fun refreshCaches() {
            refreshCallCount += 1
        }
    }

    private class FakeTokenManager(private val userId: String?, private val tokenExpired: Boolean = false) :
        TokenManager {
        override suspend fun saveTokens(userId: String, accessToken: String, refreshToken: String, expiresAt: Long) =
            Unit

        override suspend fun getAccessToken(): String? = "access"

        override suspend fun getRefreshToken(): String? = "refresh"

        override suspend fun getUserId(): String? = userId

        override suspend fun clearTokens() = Unit

        override suspend fun isTokenExpired(): Boolean = tokenExpired
    }

    private companion object {
        const val TestMutationType = "TEST_MUTATION"
    }
}
