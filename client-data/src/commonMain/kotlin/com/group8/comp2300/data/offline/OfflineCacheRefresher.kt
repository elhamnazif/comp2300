package com.group8.comp2300.data.offline

interface OfflineCacheRefresher {
    suspend fun refreshCaches()
}
