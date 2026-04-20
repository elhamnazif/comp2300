package com.group8.comp2300.feature.education

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EducationRefreshNotifier {
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val refreshes = refreshRequests.asSharedFlow()

    fun requestRefresh() {
        refreshRequests.tryEmit(Unit)
    }
}
