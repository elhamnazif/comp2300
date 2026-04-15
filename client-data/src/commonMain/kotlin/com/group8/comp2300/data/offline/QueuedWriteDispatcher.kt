package com.group8.comp2300.data.offline

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.serialization.json.Json

class QueuedWriteDispatcher(
    private val tokenManager: TokenManager,
    private val outbox: OutboxDataSource,
    private val syncCoordinator: SyncCoordinator,
    private val json: Json = Json,
) {
    suspend fun <T> replacePending(mutation: OfflineMutationSpec<T>, localId: String, payload: T) {
        deletePending(mutation, localId)
        outbox.enqueue(
            entityType = mutation.type,
            payload = json.encodeToString(mutation.serializer, payload),
            localId = localId,
            state = OutboxState.PENDING,
        )
        flushIfAuthenticated()
    }

    suspend fun <T> enqueue(mutation: OfflineMutationSpec<T>, localId: String, payload: T) {
        outbox.enqueue(
            entityType = mutation.type,
            payload = json.encodeToString(mutation.serializer, payload),
            localId = localId,
            state = OutboxState.PENDING,
        )
        flushIfAuthenticated()
    }

    fun deletePending(mutation: OfflineMutationSpec<*>, localId: String) {
        outbox.deleteByEntityTypeAndLocalId(
            entityType = mutation.type,
            localId = localId,
        )
    }

    private suspend fun flushIfAuthenticated() {
        val hasValidSession = tokenManager.getUserId() != null && !tokenManager.isTokenExpired()
        if (hasValidSession) {
            syncCoordinator.flushOutbox()
            syncCoordinator.refreshAuthenticatedData()
        }
    }
}
