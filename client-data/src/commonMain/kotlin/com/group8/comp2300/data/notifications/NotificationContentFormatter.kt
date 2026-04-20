package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.NotificationPrivacyMode
import com.group8.comp2300.data.local.PrivacySettings

data class NotificationContent(val title: String, val body: String)

class NotificationContentFormatter {
    fun routineReminder(settings: PrivacySettings): NotificationContent {
        val alias = settings.notificationAlias.takeIf(String::isNotBlank)
        return if (settings.notificationPrivacyMode == NotificationPrivacyMode.ALIAS_BASED && alias != null) {
            NotificationContent(
                title = "Reminder for $alias",
                body = "Open the app to check it.",
            )
        } else {
            NotificationContent(
                title = "Private reminder",
                body = "Open the app to check it.",
            )
        }
    }
}
