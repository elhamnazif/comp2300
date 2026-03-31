package com.group8.comp2300.data.notifications

import io.github.tweener.alarmee.Alarmee
import io.github.tweener.alarmee.AlarmeeIosPlatformConfiguration
import io.github.tweener.alarmee.AndroidNotificationConfiguration
import io.github.tweener.alarmee.IosNotificationConfiguration
import io.github.tweener.alarmee.createAlarmeeService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.time.Instant

class IosRoutineNotificationPlatform : RoutineNotificationPlatform {
    private val alarmeeService = createAlarmeeService().apply {
        initialize(platformConfiguration = AlarmeeIosPlatformConfiguration)
    }

    override suspend fun schedule(notification: ScheduledRoutineNotification) {
        alarmeeService.local.schedule(
            alarmee = Alarmee(
                uuid = notification.id,
                notificationTitle = notification.title,
                notificationBody = notification.body,
                scheduledDateTime = Instant.fromEpochMilliseconds(notification.fireAtMs)
                    .toLocalDateTime(TimeZone.currentSystemDefault()),
                androidNotificationConfiguration = AndroidNotificationConfiguration(
                    channelId = "routine_reminders",
                ),
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
