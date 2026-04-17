package com.group8.comp2300.feature.labresults.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.labresults.LabResultsScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val labResultsGraphModule = module {
    navigation<Screen.LabResults> {
        val navigator = LocalNavigator.current
        LabResultsScreen(
            viewModel = koinViewModel(),
            onBack = navigator::goBack,
            onScheduleTest = { navigator.navigate(Screen.Booking) },
        )
    }
}
