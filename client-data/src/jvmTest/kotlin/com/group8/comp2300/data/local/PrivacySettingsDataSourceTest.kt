package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlin.test.Test
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
}
