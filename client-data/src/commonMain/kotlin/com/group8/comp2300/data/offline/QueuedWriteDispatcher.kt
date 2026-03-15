package com.group8.comp2300.data.offline

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.domain.repository.medical.SyncCoordinator

class QueuedWriteDispatcher(
    private val tokenManager: TokenManager,
    private val outbox: OutboxDataSource,
    private val syncCoordinator: SyncCoordinator,
) {
    suspend fun replacePending(entityType: String, localId: String, payload: String) {
        deletePending(entityType, localId)
        outbox.enqueue(
            entityType = entityType,
            payload = payload,
            localId = localId,
            state = OutboxState.PENDING,
        )
        flushIfAuthenticated()
    }

    suspend fun enqueue(entityType: String, localId: String, payload: String = "") {
        outbox.enqueue(
            entityType = entityType,
            payload = payload,
            localId = localId,
            state = OutboxState.PENDING,
        )
        flushIfAuthenticated()
    }

    fun deletePending(entityType: String, localId: String) {
        outbox.getAll()
            .filter { it.entityType == entityType && it.localId == localId }
            .forEach { outbox.delete(it.id) }
    }

    private suspend fun flushIfAuthenticated() {
        val hasValidSession = tokenManager.getUserId() != null && !tokenManager.isTokenExpired()
        if (hasValidSession) {
            syncCoordinator.flushOutbox()
            syncCoordinator.refreshAuthenticatedData()
        }
    }
}
