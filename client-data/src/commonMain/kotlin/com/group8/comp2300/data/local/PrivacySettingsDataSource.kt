package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PrivacySettings(
    val blurAppWhenBackgrounded: Boolean = true,
)

class PrivacySettingsDataSource(
    private val settings: Settings,
) {
    val state: StateFlow<PrivacySettings>
        field: MutableStateFlow<PrivacySettings> = MutableStateFlow(loadSettings())

    fun setBlurAppWhenBackgrounded(enabled: Boolean) {
        settings.putBoolean(KEY_BLUR_APP_WHEN_BACKGROUNDED, enabled)
        state.value = state.value.copy(blurAppWhenBackgrounded = enabled)
    }

    private fun loadSettings(): PrivacySettings = PrivacySettings(
        blurAppWhenBackgrounded = settings.getBoolean(KEY_BLUR_APP_WHEN_BACKGROUNDED, true),
    )

    private companion object {
        const val KEY_BLUR_APP_WHEN_BACKGROUNDED = "privacy.blur_app_when_backgrounded"
    }
}
