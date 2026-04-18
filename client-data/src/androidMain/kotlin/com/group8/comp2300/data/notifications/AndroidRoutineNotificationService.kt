package com.group8.comp2300.data.notifications

import android.R.drawable.ic_dialog_info
import android.app.NotificationManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationManagerCompat
import com.tweener.alarmee.channel.AlarmeeNotificationChannel
import com.tweener.alarmee.configuration.AlarmeeAndroidPlatformConfiguration
import com.tweener.alarmee.createAlarmeeService
import com.tweener.alarmee.model.Alarmee
import com.tweener.alarmee.model.AndroidNotificationConfiguration
import com.tweener.alarmee.model.AndroidNotificationPriority
import com.tweener.alarmee.model.IosNotificationConfiguration
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val RoutineNotificationChannelId = "routine_reminders"

class AndroidRoutineNotificationService(private val context: Context) : RoutineNotificationService {
    private val alarmeeService = createAlarmeeService().apply {
        initialize(
            platformConfiguration = AlarmeeAndroidPlatformConfiguration(
                notificationIconResId = ic_dialog_info,
                notificationIconColor = Color.Transparent,
                notificationChannels = listOf(
                    AlarmeeNotificationChannel(
                        id = RoutineNotificationChannelId,
                        name = "Schedule reminders",
                        importance = NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                ),
                useExactScheduling = false,
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

    override suspend fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}
