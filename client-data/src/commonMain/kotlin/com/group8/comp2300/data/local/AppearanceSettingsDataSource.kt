package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppearanceThemeMode {
    MINT,
    WALLPAPER,
}

data class AppearanceSettings(
    val themeMode: AppearanceThemeMode = AppearanceThemeMode.WALLPAPER,
)

class AppearanceSettingsDataSource(private val settings: Settings) {
    val state: StateFlow<AppearanceSettings>
        field: MutableStateFlow<AppearanceSettings> = MutableStateFlow(loadSettings())

    fun setThemeMode(mode: AppearanceThemeMode) {
        settings.putString(KeyThemeMode, mode.name)
        state.value = state.value.copy(themeMode = mode)
    }

    private fun loadSettings(): AppearanceSettings = AppearanceSettings(
        themeMode = settings.getStringOrNull(KeyThemeMode)
            ?.let(::appearanceThemeModeFromStorage)
            ?: AppearanceThemeMode.WALLPAPER,
    )

    private companion object {
        const val KeyThemeMode = "appearance.theme_mode"
    }
}

private fun appearanceThemeModeFromStorage(value: String): AppearanceThemeMode =
    AppearanceThemeMode.entries.firstOrNull { it.name == value } ?: AppearanceThemeMode.WALLPAPER
