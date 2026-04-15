package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import kotlin.uuid.Uuid

enum class OutboxState {
    PENDING,
    FAILED,
}

data class OutboxItem(
    val id: String,
    val entityType: String,
    val payload: String,
    val localId: String,
    val state: OutboxState,
    val createdAt: Long,
    val retryCount: Long,
    val lastError: String?,
)

class OutboxDataSource(private val database: AppDatabase) {
    fun enqueue(entityType: String, payload: String, localId: String, state: OutboxState) {
        database.appDatabaseQueries.insertOutbox(
            id = Uuid.random().toString(),
            entityType = entityType,
            payload = payload,
            localId = localId,
            state = state.name,
            createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun getAll(): List<OutboxItem> = database.appDatabaseQueries.selectAllOutbox().executeAsList().map(::toOutboxItem)

    fun getPending(): List<OutboxItem> = database.appDatabaseQueries.selectPendingOutbox().executeAsList().map(::toOutboxItem)

    fun delete(id: String) {
        database.appDatabaseQueries.deleteOutbox(id)
    }

    fun deleteByEntityTypeAndLocalId(entityType: String, localId: String) {
        database.appDatabaseQueries.deleteOutboxByEntityTypeAndLocalId(
            entityType = entityType,
            localId = localId,
        )
    }

    fun incrementRetry(id: String) {
        database.appDatabaseQueries.incrementOutboxRetry(id)
    }

    fun updateState(id: String, state: OutboxState, lastError: String? = null) {
        database.appDatabaseQueries.updateOutboxState(
            state = state.name,
            lastError = lastError,
            id = id,
        )
    }

    fun clearAll() {
        database.appDatabaseQueries.deleteAllOutbox()
    }

    private fun toOutboxItem(entity: com.group8.comp2300.data.database.OutboxEntity): OutboxItem = OutboxItem(
        id = entity.id,
        entityType = entity.entityType,
        payload = entity.payload,
        localId = entity.localId,
        state = OutboxState.valueOf(entity.state),
        createdAt = entity.createdAt,
        retryCount = entity.retryCount,
        lastError = entity.lastError,
    )
}
