package com.group8.comp2300.data.offline

import kotlin.uuid.Uuid

class QueuedOfflineStore<Request, LocalModel>(
    private val mutation: OfflineMutationSpec<Request>,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
    private val buildLocal: suspend (localId: String, request: Request) -> LocalModel,
    private val saveLocal: suspend (LocalModel) -> Unit,
    private val readLocal: suspend (localId: String) -> LocalModel?,
    private val idFactory: () -> String = { Uuid.random().toString() },
) {
    suspend fun write(request: Request, id: String? = null): LocalModel {
        val localId = id ?: idFactory()
        val localModel = buildLocal(localId, request)
        saveLocal(localModel)
        queuedWriteDispatcher.replacePending(mutation, localId, request)
        return readLocal(localId) ?: localModel
    }
}
