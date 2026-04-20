package com.group8.comp2300.data.offline

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxItem
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.repository.newDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncCoordinatorImplTest {
    @Test
    fun retryableFlushFailureLeavesItemPendingAndSkipsRefresh() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        val refresher = RecordingOfflineDataRefresher()
        val coordinator = coordinator(
            outbox = outbox,
            refresher = refresher,
            handlers = listOf(
                object : OfflineMutationHandler {
                    override val type: String = TestMutationType

                    override suspend fun apply(item: OutboxItem) {
                        throw Exception("offline")
                    }
                },
            ),
        )

        val flushStatus = coordinator.flushOutbox()
        val refreshStatus = coordinator.refreshAuthenticatedData()

        assertEquals(1, flushStatus.pendingCount)
        assertEquals(0, flushStatus.failedCount)
        assertFalse(flushStatus.refreshed)
        assertEquals(1, refreshStatus.pendingCount)
        assertFalse(refreshStatus.refreshed)
        assertEquals(0, refresher.refreshCalls)
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
        val refresher = RecordingOfflineDataRefresher()
        var applyCalls = 0
        val coordinator = coordinator(
            outbox = outbox,
            refresher = refresher,
            handlers = listOf(
                object : OfflineMutationHandler {
                    override val type: String = TestMutationType

                    override suspend fun apply(item: OutboxItem) {
                        applyCalls += 1
                    }
                },
            ),
        )

        val failedBeforeRetry = coordinator.getFailedMutations()
        val retryStatus = coordinator.retryFailedMutation(failedItemId)

        assertEquals(1, failedBeforeRetry.size)
        assertEquals("bad request", failedBeforeRetry.single().lastError)
        assertEquals(1, applyCalls)
        assertTrue(outbox.getAll().isEmpty())
        assertTrue(retryStatus.refreshed)
        assertEquals(1, refresher.refreshCalls)
    }

    @Test
    fun flushOutboxStillAttemptsWhenTokenManagerReportsExpired() = runTest {
        val outbox = OutboxDataSource(newDatabase())
        outbox.enqueue(
            entityType = TestMutationType,
            payload = "{}",
            localId = "local-1",
            state = OutboxState.PENDING,
        )
        var applyCalls = 0
        val coordinator = SyncCoordinatorImpl(
            tokenManager = FakeTokenManager(userId = "user-1", tokenExpired = true),
            outbox = outbox,
            mutationHandlers = MutationHandlerRegistry(
                listOf(
                    object : OfflineMutationHandler {
                        override val type: String = TestMutationType

                        override suspend fun apply(item: OutboxItem) {
                            applyCalls += 1
                        }
                    },
                ),
            ),
            dataRefresher = RecordingOfflineDataRefresher(),
        )

        val flushStatus = coordinator.flushOutbox()

        assertEquals(1, applyCalls)
        assertEquals(0, flushStatus.pendingCount)
        assertEquals(0, flushStatus.failedCount)
    }

    private fun coordinator(
        outbox: OutboxDataSource,
        refresher: RecordingOfflineDataRefresher,
        handlers: List<OfflineMutationHandler>,
    ): SyncCoordinatorImpl = SyncCoordinatorImpl(
        tokenManager = FakeTokenManager(userId = "user-1"),
        outbox = outbox,
        mutationHandlers = MutationHandlerRegistry(handlers),
        dataRefresher = refresher,
    )

    private class RecordingOfflineDataRefresher : OfflineDataRefresher {
        var refreshCalls = 0

        override suspend fun refreshAuthenticatedData() {
            refreshCalls += 1
        }
    }

    private class FakeTokenManager(
        private val userId: String?,
        private val tokenExpired: Boolean = false,
    ) : TokenManager {
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
