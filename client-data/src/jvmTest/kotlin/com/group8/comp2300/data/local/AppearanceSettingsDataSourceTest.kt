package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceSettingsDataSourceTest {
    @Test
    fun themeModeDefaultsToWallpaperAndPersistsChanges() {
        val settings = Settings()
        settings.remove("appearance.theme_mode")
        val dataSource = AppearanceSettingsDataSource(settings)

        assertEquals(AppearanceThemeMode.WALLPAPER, dataSource.state.value.themeMode)

        dataSource.setThemeMode(AppearanceThemeMode.MINT)

        assertEquals(AppearanceThemeMode.MINT, dataSource.state.value.themeMode)
        assertEquals(
            AppearanceThemeMode.MINT,
            AppearanceSettingsDataSource(settings).state.value.themeMode,
        )
    }
}
