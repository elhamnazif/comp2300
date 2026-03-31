package com.group8.comp2300.data.offline

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
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

    override suspend fun flushOutbox() {
        if (tokenManager.getUserId() == null || tokenManager.isTokenExpired()) return

        mutex.withLock {
            outbox.getPending().forEach { item ->
                if (item.retryCount >= MAX_RETRIES) {
                    outbox.updateState(item.id, OutboxState.FAILED, "Retry limit reached")
                    return@forEach
                }

                try {
                    val handler = mutationHandlers.handlerFor(item.entityType)
                        ?: error("No offline mutation handler registered for ${item.entityType}")
                    handler.apply(item)
                    outbox.delete(item.id)
                } catch (e: Exception) {
                    if (e.isRetryable()) {
                        outbox.incrementRetry(item.id)
                        logger.w(e) { "Retryable outbox failure for ${item.entityType}" }
                    } else {
                        outbox.updateState(item.id, OutboxState.FAILED, e.message)
                        logger.e(e) { "Non-retryable outbox failure for ${item.entityType}" }
                    }
                }
            }
        }
    }

    override suspend fun refreshAuthenticatedData() {
        if (tokenManager.getUserId() == null || tokenManager.isTokenExpired()) return

        dataRefresher.refreshAuthenticatedData()
    }

    private companion object {
        const val MAX_RETRIES = 5
    }
}
