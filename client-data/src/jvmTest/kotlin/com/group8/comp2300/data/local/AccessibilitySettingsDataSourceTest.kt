package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccessibilitySettingsDataSourceTest {
    @Test
    fun grayscalePreferencePersistsAcrossRepositoryInstances() {
        val settings = Settings()
        settings.remove("accessibility.grayscale_enabled")
        val dataSource = AccessibilitySettingsDataSource(settings)

        assertFalse(dataSource.state.value.grayscaleEnabled)

        dataSource.setGrayscaleEnabled(true)

        assertTrue(dataSource.state.value.grayscaleEnabled)
        assertTrue(AccessibilitySettingsDataSource(settings).state.value.grayscaleEnabled)
    }
}
