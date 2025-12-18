package com.group8.comp2300.navigation

import androidx.compose.runtime.staticCompositionLocalOf

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator has not been provided! Make sure to wrap your app in CompositionLocalProvider.")
}