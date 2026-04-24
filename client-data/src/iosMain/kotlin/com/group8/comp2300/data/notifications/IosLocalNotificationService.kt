package com.group8.comp2300.data.notifications

import com.tweener.alarmee.configuration.AlarmeeIosPlatformConfiguration
import com.tweener.alarmee.createAlarmeeService
import com.tweener.alarmee.model.Alarmee
import com.tweener.alarmee.model.AndroidNotificationConfiguration
import com.tweener.alarmee.model.IosNotificationConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.time.Instant

class IosLocalNotificationService : LocalNotificationService {
    private val alarmeeService = createAlarmeeService().apply {
        initialize(platformConfiguration = AlarmeeIosPlatformConfiguration)
    }

    override suspend fun schedule(notification: ScheduledLocalNotification) {
        alarmeeService.local.schedule(
            alarmee = Alarmee(
                uuid = notification.id,
                notificationTitle = notification.title,
                notificationBody = notification.body,
                scheduledDateTime = Instant.fromEpochMilliseconds(notification.fireAtMs)
                    .toLocalDateTime(TimeZone.currentSystemDefault()),
                androidNotificationConfiguration = AndroidNotificationConfiguration(),
                iosNotificationConfiguration = IosNotificationConfiguration(),
            ),
        )
    }

    override suspend fun cancel(notificationId: String) {
        alarmeeService.local.cancel(uuid = notificationId)
    }

    override suspend fun notificationsEnabled(): Boolean = suspendCancellableCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val enabled = when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusEphemeral,
                UNAuthorizationStatusProvisional,
                -> true

                else -> false
            }
            continuation.resume(enabled)
        }
    }
}
