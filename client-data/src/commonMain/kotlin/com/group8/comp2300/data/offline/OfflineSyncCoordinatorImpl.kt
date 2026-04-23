package com.group8.comp2300.data.offline

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxItem
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.remote.ApiException
import com.group8.comp2300.domain.repository.medical.FailedSyncMutation
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import com.group8.comp2300.domain.repository.medical.SyncStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OfflineSyncCoordinatorImpl(
    private val tokenManager: TokenManager,
    private val outbox: OutboxDataSource,
    private val mutationHandlers: OfflineMutationHandlers,
    private val cacheRefresher: OfflineCacheRefresher,
) : OfflineSyncCoordinator {
    private val logger = Logger.withTag("OfflineSyncCoordinator")
    private val mutex = Mutex()

    override suspend fun syncNow(): SyncStatus {
        if (!hasAuthenticatedSession()) return currentStatus(hasSession = false)

        return mutex.withLock {
            flushPendingMutationsLocked()
            refreshCachesLocked()
        }
    }

    override suspend fun refreshCaches(): SyncStatus {
        if (!hasAuthenticatedSession()) return currentStatus(hasSession = false)

        return mutex.withLock {
            refreshCachesLocked()
        }
    }

    override suspend fun listFailedMutations(): List<FailedSyncMutation> = outbox.getFailed().map { item ->
        FailedSyncMutation(
            id = item.id,
            entityType = item.entityType,
            localId = item.localId,
            retryCount = item.retryCount,
            lastError = item.lastError,
        )
    }

    override suspend fun retryFailedMutation(id: String): SyncStatus {
        outbox.resetForRetry(id)
        return syncNow()
    }

    override suspend fun discardFailedMutation(id: String) {
        outbox.delete(id)
    }

    private suspend fun flushPendingMutationsLocked() {
        outbox.getPending().forEach { item ->
            applyPendingMutation(item)
        }
    }

    private suspend fun applyPendingMutation(item: OutboxItem) {
        if (item.retryCount >= MaxRetries) {
            outbox.updateState(item.id, OutboxState.FAILED, "Retry limit reached")
            return
        }

        try {
            val handler = mutationHandlers.handlerFor(item.entityType)
                ?: error("No offline mutation handler registered for ${item.entityType}")
            handler.apply(item)
            outbox.delete(item.id)
        } catch (error: Exception) {
            handleMutationFailure(item, error)
        }
    }

    private fun handleMutationFailure(item: OutboxItem, error: Exception) {
        if (error.isAuthenticationFailure()) {
            logger.w(error) { "Authentication failure while flushing outbox for ${item.entityType}" }
            throw error
        }

        if (error.isRetryable()) {
            outbox.incrementRetry(item.id)
            logger.w(error) { "Retryable outbox failure for ${item.entityType}" }
            return
        }

        outbox.updateState(item.id, OutboxState.FAILED, error.message)
        logger.e(error) { "Non-retryable outbox failure for ${item.entityType}" }
    }

    private suspend fun refreshCachesLocked(): SyncStatus {
        val snapshotBeforeRefresh = outboxSnapshot()
        if (snapshotBeforeRefresh.hasQueuedMutations) {
            return currentStatus(hasSession = true, snapshot = snapshotBeforeRefresh)
        }

        cacheRefresher.refreshCaches()
        return currentStatus(
            hasSession = true,
            snapshot = outboxSnapshot(),
            cachesRefreshed = true,
        )
    }

    private suspend fun hasAuthenticatedSession(): Boolean = tokenManager.getUserId() != null

    private fun currentStatus(
        hasSession: Boolean,
        snapshot: OutboxSnapshot = outboxSnapshot(),
        cachesRefreshed: Boolean = false,
    ): SyncStatus = SyncStatus(
        hasAuthenticatedSession = hasSession,
        pendingCount = snapshot.pendingCount,
        failedCount = snapshot.failedCount,
        cachesRefreshed = cachesRefreshed,
    )

    private fun outboxSnapshot(): OutboxSnapshot {
        val items = outbox.getAll()
        return OutboxSnapshot(
            pendingCount = items.count { it.state == OutboxState.PENDING },
            failedCount = items.count { it.state == OutboxState.FAILED },
        )
    }

    private fun Throwable.isAuthenticationFailure(): Boolean = this is ApiException && statusCode == 401

    private data class OutboxSnapshot(
        val pendingCount: Int,
        val failedCount: Int,
    ) {
        val hasQueuedMutations: Boolean = pendingCount > 0 || failedCount > 0
    }

    companion object {
        const val MaxRetries = 5
    }
}
