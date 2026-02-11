package com.group8.comp2300.di

import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.home.HomeScreen
import com.group8.comp2300.presentation.screens.medical.MedicationScreen
import com.group8.comp2300.presentation.screens.medical.calendar.CalendarScreen
import com.group8.comp2300.presentation.screens.profile.ProfileScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val mainNavigationModule = module {
    navigation<Screen.Home> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.home.HomeScreen(
            onNavigateToShop = { navigator.navigate(Screen.Shop) },
            onNavigateToCalendar = { navigator.navigate(Screen.Calendar) },
            onNavigateToEducation = { navigator.navigate(Screen.Education) },
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
            onNavigateToSymptomChecker = { navigator.navigate(Screen.SelfDiagnosis) },
            onNavigateToClinicMap = { navigator.navigate(Screen.Booking) }
        )
    }

    navigation<Screen.Calendar> {
        _root_ide_package_.com.group8.comp2300.presentation.screens.medical.calendar.CalendarScreen()
    }

    navigation<Screen.Profile> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.profile.ProfileScreen(
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
            onNavigateToLabResults = { navigator.navigate(Screen.LabResults) },
            onNavigateToPrivacySecurity = { navigator.navigate(Screen.PrivacySecurity) },
            onNavigateToNotifications = { navigator.navigate(Screen.Notifications) },
            onNavigateToHelpSupport = { navigator.navigate(Screen.HelpSupport) }
        )
    }

    navigation<Screen.Medication> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.medical.MedicationScreen(
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth
        )
    }
}
