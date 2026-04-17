package com.group8.comp2300.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.group8.comp2300.platform.theme.getWallpaperSeedColor
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val seedColor = getWallpaperSeedColor() ?: Color(0xFF66ffc7)
    DynamicMaterialExpressiveTheme(
        seedColor = seedColor,
        isDark = isSystemInDarkTheme(),
        isAmoled = false,
        style = PaletteStyle.Content,
        animate = true,
        content = content,
    )
}
