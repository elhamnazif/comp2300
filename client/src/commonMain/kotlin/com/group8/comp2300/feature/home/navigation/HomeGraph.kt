package com.group8.comp2300.feature.home.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.LocalUseRootOverlayForShellChildren
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.home.HomeScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val homeGraphModule = module {
    navigation<Screen.Home> {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        HomeScreen(
            onNavigateToShop = { navigator.navigate(Screen.Shop) },
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
            onNavigateToRoutines = { navigator.navigate(Screen.Routines) },
            onNavigateToChatbot = {
                val destination = Screen.Chatbot
                if (useRootOverlayForShellChildren) {
                    navigator.navigate(
                        destination,
                    )
                } else {
                    navigator.navigateWithinShell(destination)
                }
            },
            onNavigateToSymptomChecker = { navigator.navigate(Screen.SelfDiagnosis) },
        )
    }
}
