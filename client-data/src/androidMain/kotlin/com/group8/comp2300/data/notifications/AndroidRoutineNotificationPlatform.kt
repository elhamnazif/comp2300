package com.group8.comp2300.data.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import io.github.tweener.alarmee.Alarmee
import io.github.tweener.alarmee.AlarmeeAndroidPlatformConfiguration
import io.github.tweener.alarmee.AlarmeeNotificationChannel
import io.github.tweener.alarmee.AndroidNotificationConfiguration
import io.github.tweener.alarmee.AndroidNotificationPriority
import io.github.tweener.alarmee.IosNotificationConfiguration
import io.github.tweener.alarmee.createAlarmeeService
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val RoutineNotificationChannelId = "routine_reminders"

class AndroidRoutineNotificationPlatform(
    private val context: Context,
) : RoutineNotificationPlatform {
    private val alarmeeService = createAlarmeeService().apply {
        initialize(
            platformConfiguration = AlarmeeAndroidPlatformConfiguration(
                notificationIconResId = android.R.drawable.ic_dialog_info,
                useExactScheduling = false,
                notificationChannels = listOf(
                    AlarmeeNotificationChannel(
                        id = RoutineNotificationChannelId,
                        name = "Schedule reminders",
                        importance = NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                ),
            ),
        )
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
                    priority = AndroidNotificationPriority.DEFAULT,
                    channelId = RoutineNotificationChannelId,
                ),
                iosNotificationConfiguration = IosNotificationConfiguration(),
            ),
        )
    }

    override suspend fun cancel(notificationId: String) {
        alarmeeService.local.cancel(uuid = notificationId)
    }

    override suspend fun notificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()
}
