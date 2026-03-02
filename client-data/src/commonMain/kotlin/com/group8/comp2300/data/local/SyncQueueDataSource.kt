package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import kotlin.uuid.Uuid

data class SyncQueueItem(
    val id: String,
    val entityType: String,
    val payload: String,
    val createdAt: Long,
    val retryCount: Long,
)

class SyncQueueDataSource(private val database: AppDatabase) {

    fun enqueue(entityType: String, payload: String) {
        database.appDatabaseQueries.insertSyncQueue(
            id = Uuid.random().toString(),
            entityType = entityType,
            payload = payload,
            createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun getAllPending(): List<SyncQueueItem> =
        database.appDatabaseQueries.selectAllSyncQueue().executeAsList().map { entity ->
            SyncQueueItem(
                id = entity.id,
                entityType = entity.entityType,
                payload = entity.payload,
                createdAt = entity.createdAt,
                retryCount = entity.retryCount,
            )
        }

    fun delete(id: String) {
        database.appDatabaseQueries.deleteSyncQueue(id)
    }

    fun incrementRetry(id: String) {
        database.appDatabaseQueries.incrementSyncRetry(id)
    }
}
