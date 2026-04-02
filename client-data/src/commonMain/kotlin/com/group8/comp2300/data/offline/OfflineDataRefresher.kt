package com.group8.comp2300.data.offline

interface OfflineDataRefresher {
    suspend fun refreshAuthenticatedData()
}

class CompositeOfflineDataRefresher(private val refreshers: List<OfflineDataRefresher>) : OfflineDataRefresher {
    override suspend fun refreshAuthenticatedData() {
        refreshers.forEach { it.refreshAuthenticatedData() }
    }
}
