package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AccessibilitySettings(
    val grayscaleEnabled: Boolean = false,
)

class AccessibilitySettingsDataSource(
    private val settings: Settings,
) {
    val state: StateFlow<AccessibilitySettings>
        field: MutableStateFlow<AccessibilitySettings> = MutableStateFlow(loadSettings())

    fun setGrayscaleEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_GRAYSCALE_ENABLED, enabled)
        state.value = state.value.copy(grayscaleEnabled = enabled)
    }

    private fun loadSettings(): AccessibilitySettings = AccessibilitySettings(
        grayscaleEnabled = settings.getBoolean(KEY_GRAYSCALE_ENABLED, false),
    )

    private companion object {
        const val KEY_GRAYSCALE_ENABLED = "accessibility.grayscale_enabled"
    }
}
