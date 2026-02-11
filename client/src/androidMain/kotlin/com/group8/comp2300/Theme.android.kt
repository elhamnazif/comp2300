package com.group8.comp2300

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getWallpaperSeedColor(): androidx.compose.ui.graphics.Color? {
    val context = LocalContext.current
    val seed = WallpaperManager.getInstance(context)
        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        ?.primaryColor
        ?.toArgb()
    return seed?.let { androidx.compose.ui.graphics.Color(it) }
}
