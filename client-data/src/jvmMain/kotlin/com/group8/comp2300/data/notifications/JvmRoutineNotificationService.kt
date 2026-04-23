package com.group8.comp2300.data.notifications

class JvmLocalNotificationService : LocalNotificationService {
    override suspend fun schedule(notification: ScheduledLocalNotification) = Unit

    override suspend fun cancel(notificationId: String) = Unit

    override suspend fun notificationsEnabled(): Boolean = false
}
