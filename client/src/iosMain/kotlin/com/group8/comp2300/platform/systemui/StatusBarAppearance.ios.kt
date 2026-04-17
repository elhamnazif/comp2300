package com.group8.comp2300.platform.systemui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.Foundation.NSNotificationCenter

@Composable
actual fun SetDarkStatusBar(darkIcons: Boolean) {
    val restoreDarkIcons = !isSystemInDarkTheme()

    DisposableEffect(darkIcons, restoreDarkIcons) {
        postStatusBarAppearance(darkIcons)

        onDispose {
            postStatusBarAppearance(restoreDarkIcons)
        }
    }
}

private fun postStatusBarAppearance(darkIcons: Boolean) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = STATUS_BAR_APPEARANCE_NOTIFICATION,
        `object` = if (darkIcons) STATUS_BAR_DARK_ICONS else STATUS_BAR_LIGHT_ICONS,
    )
}

private const val STATUS_BAR_APPEARANCE_NOTIFICATION = "com.group8.comp2300.statusBarAppearanceDidChange"
private const val STATUS_BAR_DARK_ICONS = "dark-icons"
private const val STATUS_BAR_LIGHT_ICONS = "light-icons"
