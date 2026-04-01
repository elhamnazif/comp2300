package com.group8.comp2300.data.notifications

class JvmRoutineNotificationService : RoutineNotificationService {
    override suspend fun schedule(notification: ScheduledRoutineNotification) = Unit

    override suspend fun cancel(notificationId: String) = Unit

    override suspend fun notificationsEnabled(): Boolean = false
}
