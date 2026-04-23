package com.group8.comp2300.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.group8.comp2300.data.local.AppearanceThemeMode
import com.group8.comp2300.platform.theme.getWallpaperSeedColor
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle

private val MintThemeSeedColor = Color(0xFF66FFC7)

val LocalAppearanceThemeMode = staticCompositionLocalOf { AppearanceThemeMode.WALLPAPER }

@Composable
fun AppTheme(
    appearanceThemeMode: AppearanceThemeMode,
    content: @Composable () -> Unit,
) {
    val wallpaperSeedColor = getWallpaperSeedColor()
    val effectiveThemeMode = effectiveAppearanceThemeMode(appearanceThemeMode, wallpaperSeedColor)
    val seedColor = resolveThemeSeedColor(appearanceThemeMode, wallpaperSeedColor)

    CompositionLocalProvider(LocalAppearanceThemeMode provides effectiveThemeMode) {
        DynamicMaterialExpressiveTheme(
            seedColor = seedColor,
            isDark = isSystemInDarkTheme(),
            isAmoled = false,
            style = PaletteStyle.Content,
            animate = true,
            content = content,
        )
    }
}

internal fun effectiveAppearanceThemeMode(
    appearanceThemeMode: AppearanceThemeMode,
    wallpaperSeedColor: Color?,
): AppearanceThemeMode = when {
    appearanceThemeMode == AppearanceThemeMode.WALLPAPER && wallpaperSeedColor == null -> AppearanceThemeMode.MINT
    else -> appearanceThemeMode
}

internal fun resolveThemeSeedColor(
    appearanceThemeMode: AppearanceThemeMode,
    wallpaperSeedColor: Color?,
): Color = when (effectiveAppearanceThemeMode(appearanceThemeMode, wallpaperSeedColor)) {
    AppearanceThemeMode.MINT -> MintThemeSeedColor
    AppearanceThemeMode.WALLPAPER -> wallpaperSeedColor ?: MintThemeSeedColor
}
