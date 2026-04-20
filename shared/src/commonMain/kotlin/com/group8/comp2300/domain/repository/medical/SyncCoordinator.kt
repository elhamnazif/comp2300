package com.group8.comp2300.domain.repository.medical

data class FailedSyncMutation(
    val id: String,
    val entityType: String,
    val localId: String,
    val retryCount: Long,
    val lastError: String?,
)

data class SyncStatus(
    val hasAuthenticatedSession: Boolean,
    val pendingCount: Int,
    val failedCount: Int,
    val refreshed: Boolean,
)

interface SyncCoordinator {
    suspend fun flushOutbox(): SyncStatus

    suspend fun refreshAuthenticatedData(): SyncStatus

    suspend fun getFailedMutations(): List<FailedSyncMutation>

    suspend fun retryFailedMutation(id: String): SyncStatus

    suspend fun discardMutation(id: String)
}
