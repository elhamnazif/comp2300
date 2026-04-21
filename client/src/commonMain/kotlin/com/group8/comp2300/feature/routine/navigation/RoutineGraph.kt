package com.group8.comp2300.feature.routine.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.feature.routine.RoutineScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val routineGraphModule = module {
    navigation<Screen.Routines>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        RoutineScreen(
            onBack = navigator::goBack,
        )
    }
}
