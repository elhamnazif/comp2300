package com.group8.comp2300.app

import com.group8.comp2300.app.navigation.Screen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val mainShellGraphModule = module {
    navigation<Screen.MainShell> {
        MainShellScreen()
    }
}
