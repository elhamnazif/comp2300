package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.NotificationPrivacyMode
import com.group8.comp2300.data.local.PrivacySettings

private const val GenericReminderBody = "Open the app to check it."

data class NotificationContent(val title: String, val body: String)

class NotificationContentFormatter {
    fun routineReminder(settings: PrivacySettings): NotificationContent = NotificationContent(
        title = reminderTitle(settings),
        body = GenericReminderBody,
    )

    fun appointmentReminder(settings: PrivacySettings): NotificationContent = NotificationContent(
        title = reminderTitle(settings),
        body = GenericReminderBody,
    )

    private fun reminderTitle(settings: PrivacySettings): String {
        val alias = settings.notificationAlias.takeIf(String::isNotBlank)
        return if (settings.notificationPrivacyMode == NotificationPrivacyMode.ALIAS_BASED && alias != null) {
            "Reminder for $alias"
        } else {
            "Private reminder"
        }
    }
}
