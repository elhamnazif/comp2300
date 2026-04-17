package com.group8.comp2300.platform.systemui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun SetDarkStatusBar(darkIcons: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return
    val restoreDarkIcons = !isSystemInDarkTheme()

    DisposableEffect(activity, darkIcons, restoreDarkIcons) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = darkIcons

        onDispose {
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars =
                restoreDarkIcons
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
