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
        aName = StatusBarAppearanceNotification,
        `object` = if (darkIcons) StatusBarDarkIcons else StatusBarLightIcons,
    )
}

private const val StatusBarAppearanceNotification = "com.group8.comp2300.statusBarAppearanceDidChange"
private const val StatusBarDarkIcons = "dark-icons"
private const val StatusBarLightIcons = "light-icons"
