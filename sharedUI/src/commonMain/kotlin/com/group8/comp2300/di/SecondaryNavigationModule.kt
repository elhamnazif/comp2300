package com.group8.comp2300.di

import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.medical.LabResultsScreen
import com.group8.comp2300.presentation.screens.medical.SelfDiagnosisScreen
import com.group8.comp2300.presentation.screens.profile.HelpSupportScreen
import com.group8.comp2300.presentation.screens.profile.NotificationsScreen
import com.group8.comp2300.presentation.screens.profile.PrivacySecurityScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val secondaryNavigationModule = module {
    navigation<Screen.SelfDiagnosis> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.medical.SelfDiagnosisScreen(
            onBack = navigator::goBack,
            onNavigateToBooking = { navigator.navigate(Screen.Booking) }
        )
    }

    navigation<Screen.LabResults> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.medical.LabResultsScreen(
            onBack = navigator::goBack,
            onScheduleTest = { navigator.navigate(Screen.Booking) }
        )
    }

    navigation<Screen.PrivacySecurity> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.profile.PrivacySecurityScreen(
            onBack = navigator::goBack
        )
    }

    navigation<Screen.Notifications> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.profile.NotificationsScreen(
            onBack = navigator::goBack
        )
    }

    navigation<Screen.HelpSupport> {
        val navigator = LocalNavigator.current
        _root_ide_package_.com.group8.comp2300.presentation.screens.profile.HelpSupportScreen(
            onBack = navigator::goBack
        )
    }
}
