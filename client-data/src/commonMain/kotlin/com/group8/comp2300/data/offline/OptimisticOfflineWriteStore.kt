package com.group8.comp2300.data.offline

import kotlin.uuid.Uuid

class OptimisticOfflineWriteStore<Request, LocalModel>(
    private val mutation: OfflineMutationDescriptor<Request>,
    private val offlineMutationQueue: OfflineMutationQueue,
    private val buildLocal: suspend (localId: String, request: Request) -> LocalModel,
    private val saveLocal: suspend (LocalModel) -> Unit,
    private val readLocal: suspend (localId: String) -> LocalModel?,
    private val idFactory: () -> String = { Uuid.random().toString() },
) {
    suspend fun write(request: Request, id: String? = null): LocalModel {
        val localId = id ?: idFactory()
        val optimisticLocalModel = buildLocal(localId, request)
        saveLocal(optimisticLocalModel)
        offlineMutationQueue.replacePending(mutation, localId, request)
        return readLocal(localId) ?: optimisticLocalModel
    }
}
