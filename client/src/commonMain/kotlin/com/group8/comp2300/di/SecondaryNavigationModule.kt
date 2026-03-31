package com.group8.comp2300.di

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.medical.LabResultsScreen
import com.group8.comp2300.presentation.screens.medical.RoutineScreen
import com.group8.comp2300.presentation.screens.medical.SelfDiagnosisScreen
import com.group8.comp2300.presentation.screens.profile.HelpSupportScreen
import com.group8.comp2300.presentation.screens.profile.NotificationsScreen
import com.group8.comp2300.presentation.screens.profile.PrivacyLegaleseScreen
import com.group8.comp2300.presentation.screens.profile.PrivacySecurityScreen
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val secondaryNavigationModule = module {
    navigation<Screen.SelfDiagnosis> {
        val navigator = LocalNavigator.current
        SelfDiagnosisScreen(
            onBack = navigator::goBack,
            onNavigateToBooking = { navigator.navigate(Screen.Booking) },
        )
    }

    navigation<Screen.LabResults> {
        val navigator = LocalNavigator.current
        LabResultsScreen(
            onBack = navigator::goBack,
            onScheduleTest = { navigator.navigate(Screen.Booking) },
        )
    }

    navigation<Screen.PrivacySecurity> {
        val navigator = LocalNavigator.current
        val pinDataSource = koinInject<PinDataSource>()
        val scope = rememberCoroutineScope()
        PrivacySecurityScreen(
            onBack = navigator::goBack,
            onChangePin = { pin -> scope.launch { pinDataSource.savePin(pin) } },
        )
    }

    navigation<Screen.Notifications> {
        val navigator = LocalNavigator.current
        NotificationsScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.Routines> {
        val navigator = LocalNavigator.current
        RoutineScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.HelpSupport> {
        val navigator = LocalNavigator.current
        HelpSupportScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.PrivacyLegalese> {
        val navigator = LocalNavigator.current
        PrivacyLegaleseScreen(
            onBack = navigator::goBack,
        )
    }
}
