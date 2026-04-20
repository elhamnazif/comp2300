package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class NotificationPrivacyMode {
    NEUTRAL,
    ALIAS_BASED,
}

data class PrivacySettings(
    val blurAppWhenBackgrounded: Boolean = true,
    val notificationPrivacyMode: NotificationPrivacyMode = NotificationPrivacyMode.NEUTRAL,
    val notificationAlias: String = "",
)

class PrivacySettingsDataSource(private val settings: Settings) {
    val state: StateFlow<PrivacySettings>
        field: MutableStateFlow<PrivacySettings> = MutableStateFlow(loadSettings())

    fun setBlurAppWhenBackgrounded(enabled: Boolean) {
        settings.putBoolean(KeyBlurAppWhenBackgrounded, enabled)
        state.value = state.value.copy(blurAppWhenBackgrounded = enabled)
    }

    fun setNotificationPrivacyMode(mode: NotificationPrivacyMode) {
        settings.putString(KeyNotificationPrivacyMode, mode.name)
        state.value = state.value.copy(notificationPrivacyMode = mode)
    }

    fun setNotificationAlias(alias: String) {
        val normalizedAlias = alias.normalizeNotificationAlias()
        if (normalizedAlias.isEmpty()) {
            settings.remove(KeyNotificationAlias)
        } else {
            settings.putString(KeyNotificationAlias, normalizedAlias)
        }
        state.value = state.value.copy(notificationAlias = normalizedAlias)
    }

    private fun loadSettings(): PrivacySettings = PrivacySettings(
        blurAppWhenBackgrounded = settings.getBoolean(KeyBlurAppWhenBackgrounded, true),
        notificationPrivacyMode = settings.getStringOrNull(KeyNotificationPrivacyMode)
            ?.let(::notificationPrivacyModeFromStorage)
            ?: NotificationPrivacyMode.NEUTRAL,
        notificationAlias = settings.getStringOrNull(KeyNotificationAlias)
            ?.normalizeNotificationAlias()
            .orEmpty(),
    )

    private companion object {
        const val KeyBlurAppWhenBackgrounded = "privacy.blur_app_when_backgrounded"
        const val KeyNotificationPrivacyMode = "privacy.notification_privacy_mode"
        const val KeyNotificationAlias = "privacy.notification_alias"
    }
}

private fun notificationPrivacyModeFromStorage(value: String): NotificationPrivacyMode =
    NotificationPrivacyMode.entries.firstOrNull { it.name == value } ?: NotificationPrivacyMode.NEUTRAL

private fun String.normalizeNotificationAlias(): String = trim().replace("\\s+".toRegex(), " ").take(40)
