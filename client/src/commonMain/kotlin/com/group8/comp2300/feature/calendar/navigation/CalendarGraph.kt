package com.group8.comp2300.feature.calendar.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.feature.calendar.CalendarScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val calendarGraphModule = module {
    navigation<Screen.Calendar> {
        val navigator = LocalNavigator.current
        CalendarScreen(
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
        )
    }
}
