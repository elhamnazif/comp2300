package com.group8.comp2300.data.offline

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.remote.ApiException
import com.group8.comp2300.domain.repository.medical.FailedSyncMutation
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import com.group8.comp2300.domain.repository.medical.SyncStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncCoordinatorImpl(
    private val tokenManager: TokenManager,
    private val outbox: OutboxDataSource,
    private val mutationHandlers: MutationHandlerRegistry,
    private val dataRefresher: OfflineDataRefresher,
) : SyncCoordinator {
    private val logger = Logger.withTag("SyncCoordinator")
    private val mutex = Mutex()

    override suspend fun flushOutbox(): SyncStatus {
        if (!hasAuthenticatedSession()) return currentStatus(hasSession = false)

        return mutex.withLock {
            outbox.getPending().forEach { item ->
                if (item.retryCount >= MaxRetries) {
                    outbox.updateState(item.id, OutboxState.FAILED, "Retry limit reached")
                    return@forEach
                }

                try {
                    val handler = mutationHandlers.handlerFor(item.entityType)
                        ?: error("No offline mutation handler registered for ${item.entityType}")
                    handler.apply(item)
                    outbox.delete(item.id)
                } catch (e: Exception) {
                    if (e.isAuthenticationFailure()) {
                        logger.w(e) { "Authentication failure while flushing outbox for ${item.entityType}" }
                        throw e
                    }
                    if (e.isRetryable()) {
                        outbox.incrementRetry(item.id)
                        logger.w(e) { "Retryable outbox failure for ${item.entityType}" }
                    } else {
                        outbox.updateState(item.id, OutboxState.FAILED, e.message)
                        logger.e(e) { "Non-retryable outbox failure for ${item.entityType}" }
                    }
                }
            }
            currentStatus(hasSession = true)
        }
    }

    override suspend fun refreshAuthenticatedData(): SyncStatus {
        if (!hasAuthenticatedSession()) return currentStatus(hasSession = false)
        if (outbox.getAll().isNotEmpty()) return currentStatus(hasSession = true)

        dataRefresher.refreshAuthenticatedData()
        return currentStatus(hasSession = true, refreshed = true)
    }

    override suspend fun getFailedMutations(): List<FailedSyncMutation> = outbox.getFailed().map { item ->
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
        flushOutbox()
        return refreshAuthenticatedData()
    }

    override suspend fun discardMutation(id: String) {
        outbox.delete(id)
    }

    private suspend fun hasAuthenticatedSession(): Boolean = tokenManager.getUserId() != null

    private fun currentStatus(hasSession: Boolean, refreshed: Boolean = false): SyncStatus = SyncStatus(
        hasAuthenticatedSession = hasSession,
        pendingCount = outbox.getPending().size,
        failedCount = outbox.getFailed().size,
        refreshed = refreshed,
    )

    private fun Throwable.isAuthenticationFailure(): Boolean = this is ApiException && statusCode == 401

    companion object {
        const val MaxRetries = 5
    }
}
