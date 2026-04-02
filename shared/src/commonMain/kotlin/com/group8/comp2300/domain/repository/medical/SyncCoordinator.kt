package com.group8.comp2300.domain.repository.medical

interface SyncCoordinator {
    suspend fun flushOutbox()

    suspend fun refreshAuthenticatedData()
}
