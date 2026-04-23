package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.NotificationPrivacyMode
import com.group8.comp2300.data.local.PrivacySettings

data class NotificationContent(val title: String, val body: String)

class NotificationContentFormatter {
    fun routineReminder(settings: PrivacySettings): NotificationContent {
        return NotificationContent(
            title = reminderTitle(settings),
            body = genericReminderBody(),
        )
    }

    fun appointmentReminder(settings: PrivacySettings): NotificationContent = NotificationContent(
        title = reminderTitle(settings),
        body = genericReminderBody(),
    )

    private fun reminderTitle(settings: PrivacySettings): String {
        val alias = settings.notificationAlias.takeIf(String::isNotBlank)
        return if (settings.notificationPrivacyMode == NotificationPrivacyMode.ALIAS_BASED && alias != null) {
            "Reminder for $alias"
        } else {
            "Private reminder"
        }
    }

    private fun genericReminderBody(): String = "Open the app to check it."
}
