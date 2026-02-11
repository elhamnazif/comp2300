package com.group8.comp2300

import android.app.WallpaperManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getWallpaperSeedColor(): androidx.compose.ui.graphics.Color? {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
        // getWallpaperColors() requires API 27; on older devices just fall back.
        return null
    }

    val seed = WallpaperManager.getInstance(context)
        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        ?.primaryColor
        ?.toArgb()

    return seed?.let { androidx.compose.ui.graphics.Color(it) }
}
