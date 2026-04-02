package com.group8.comp2300.data.offline

import com.group8.comp2300.data.local.OutboxItem
import kotlinx.serialization.json.Json

interface OfflineMutationHandler {
    val type: String

    suspend fun apply(item: OutboxItem)
}

class MutationHandlerRegistry(handlers: List<OfflineMutationHandler>) {
    private val handlersByType = handlers.associateBy(OfflineMutationHandler::type)

    init {
        check(handlersByType.size == handlers.size) {
            "Duplicate offline mutation handler type registered"
        }
    }

    fun handlerFor(type: String): OfflineMutationHandler? = handlersByType[type]
}

abstract class DecodingOfflineMutationHandler<T>(
    private val mutation: OfflineMutationSpec<T>,
    private val json: Json = Json,
) : OfflineMutationHandler {
    final override val type: String = mutation.type

    final override suspend fun apply(item: OutboxItem) {
        handle(item, json.decodeFromString(mutation.serializer, item.payload))
    }

    protected abstract suspend fun handle(item: OutboxItem, payload: T)
}

abstract class ItemOnlyOfflineMutationHandler(final override val type: String) : OfflineMutationHandler
