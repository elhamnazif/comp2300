package com.group8.comp2300.di

import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.ui.screens.medical.LabResultsScreen
import com.group8.comp2300.presentation.ui.screens.medical.SelfDiagnosisScreen
import com.group8.comp2300.presentation.ui.screens.profile.HelpSupportScreen
import com.group8.comp2300.presentation.ui.screens.profile.NotificationsScreen
import com.group8.comp2300.presentation.ui.screens.profile.PrivacySecurityScreen
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val secondaryNavigationModule = module {
    navigation<Screen.SelfDiagnosis> {
        val navigator = LocalNavigator.current
        SelfDiagnosisScreen(onBack = navigator::goBack, onNavigateToBooking = { navigator.navigate(Screen.Booking) })
    }

    navigation<Screen.LabResults> {
        val navigator = LocalNavigator.current
        LabResultsScreen(onBack = navigator::goBack, onScheduleTest = { navigator.navigate(Screen.Booking) })
    }

    navigation<Screen.PrivacySecurity> {
        val navigator = LocalNavigator.current
        PrivacySecurityScreen(onBack = navigator::goBack)
    }

    navigation<Screen.Notifications> {
        val navigator = LocalNavigator.current
        NotificationsScreen(onBack = navigator::goBack)
    }

    navigation<Screen.HelpSupport> {
        val navigator = LocalNavigator.current
        HelpSupportScreen(onBack = navigator::goBack)
    }
}
