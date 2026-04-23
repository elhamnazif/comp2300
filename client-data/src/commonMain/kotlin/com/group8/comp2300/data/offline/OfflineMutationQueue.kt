package com.group8.comp2300.data.offline

import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import kotlinx.serialization.json.Json

class OfflineMutationQueue(
    private val tokenManager: TokenManager,
    private val outbox: OutboxDataSource,
    private val offlineSyncCoordinator: OfflineSyncCoordinator,
    private val json: Json = Json,
) {
    suspend fun <T> replacePending(mutation: OfflineMutationDescriptor<T>, localId: String, payload: T) {
        deletePending(mutation, localId)
        enqueuePending(mutation, localId, payload)
    }

    suspend fun <T> enqueue(mutation: OfflineMutationDescriptor<T>, localId: String, payload: T) {
        enqueuePending(mutation, localId, payload)
    }

    fun deletePending(mutation: OfflineMutationDescriptor<*>, localId: String) {
        outbox.deleteByEntityTypeAndLocalId(
            entityType = mutation.type,
            localId = localId,
        )
    }

    private suspend fun <T> enqueuePending(mutation: OfflineMutationDescriptor<T>, localId: String, payload: T) {
        outbox.enqueue(
            entityType = mutation.type,
            payload = json.encodeToString(mutation.serializer, payload),
            localId = localId,
            state = OutboxState.PENDING,
        )
        syncIfAuthenticated()
    }

    private suspend fun syncIfAuthenticated() {
        if (tokenManager.getUserId() != null) {
            offlineSyncCoordinator.syncNow()
        }
    }
}
