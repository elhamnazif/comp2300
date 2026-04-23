package com.group8.comp2300.data.notifications

data class ScheduledLocalNotification(
    val id: String,
    val fireAtMs: Long,
    val title: String,
    val body: String,
)

interface LocalNotificationService {
    suspend fun schedule(notification: ScheduledLocalNotification)

    suspend fun cancel(notificationId: String)

    suspend fun notificationsEnabled(): Boolean
}
