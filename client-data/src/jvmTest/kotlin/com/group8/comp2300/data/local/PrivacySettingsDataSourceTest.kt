package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivacySettingsDataSourceTest {
    @Test
    fun blurAppWhenBackgroundedDefaultsToEnabledAndPersistsChanges() {
        val settings = Settings()
        settings.remove("privacy.blur_app_when_backgrounded")
        val dataSource = PrivacySettingsDataSource(settings)

        assertTrue(dataSource.state.value.blurAppWhenBackgrounded)

        dataSource.setBlurAppWhenBackgrounded(false)

        assertFalse(dataSource.state.value.blurAppWhenBackgrounded)
        assertFalse(PrivacySettingsDataSource(settings).state.value.blurAppWhenBackgrounded)
    }

    @Test
    fun notificationPrivacyDefaultsToNeutralAndPersistsAliasSettings() {
        val settings = Settings()
        settings.remove("privacy.notification_privacy_mode")
        settings.remove("privacy.notification_alias")
        val dataSource = PrivacySettingsDataSource(settings)

        assertEquals(NotificationPrivacyMode.NEUTRAL, dataSource.state.value.notificationPrivacyMode)
        assertEquals("", dataSource.state.value.notificationAlias)

        dataSource.setNotificationPrivacyMode(NotificationPrivacyMode.ALIAS_BASED)
        dataSource.setNotificationAlias("  Care   Buddy  ")

        val restored = PrivacySettingsDataSource(settings).state.value
        assertEquals(NotificationPrivacyMode.ALIAS_BASED, restored.notificationPrivacyMode)
        assertEquals("Care Buddy", restored.notificationAlias)
    }
}
