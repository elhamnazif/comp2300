package com.group8.comp2300.app

import androidx.compose.ui.graphics.Color
import com.group8.comp2300.data.local.AppearanceThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeTest {
    @Test
    fun wallpaperModeFallsBackToMintWhenWallpaperSeedIsUnavailable() {
        assertEquals(
            AppearanceThemeMode.MINT,
            effectiveAppearanceThemeMode(
                appearanceThemeMode = AppearanceThemeMode.WALLPAPER,
                wallpaperSeedColor = null,
            ),
        )
    }

    @Test
    fun wallpaperModeUsesWallpaperSeedWhenAvailable() {
        val wallpaperSeed = Color(0xFF123456)

        assertEquals(
            wallpaperSeed,
            resolveThemeSeedColor(
                appearanceThemeMode = AppearanceThemeMode.WALLPAPER,
                wallpaperSeedColor = wallpaperSeed,
            ),
        )
    }

    @Test
    fun mintModeAlwaysUsesMintSeed() {
        assertEquals(
            Color(0xFF66FFC7),
            resolveThemeSeedColor(
                appearanceThemeMode = AppearanceThemeMode.MINT,
                wallpaperSeedColor = Color(0xFF123456),
            ),
        )
    }
}
