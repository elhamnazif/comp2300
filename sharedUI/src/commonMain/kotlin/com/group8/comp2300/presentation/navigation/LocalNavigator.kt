package com.group8.comp2300.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf

@Suppress("CompositionLocalAllowlist")
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator has not been provided! Make sure to wrap your app in CompositionLocalProvider.")
}
