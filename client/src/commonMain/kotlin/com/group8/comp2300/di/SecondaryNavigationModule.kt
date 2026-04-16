package com.group8.comp2300.di

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.medical.labresults.LabResultsScreen
import com.group8.comp2300.presentation.screens.medical.record.MedicalRecordScreen
import com.group8.comp2300.presentation.screens.medical.routine.RoutineScreen
import com.group8.comp2300.presentation.screens.medical.selfdiagnosis.SelfDiagnosisScreen
import com.group8.comp2300.presentation.screens.profile.GuestSignInScreen
import com.group8.comp2300.presentation.screens.profile.HelpSupportScreen
import com.group8.comp2300.presentation.screens.profile.AccessibilityScreen
import com.group8.comp2300.presentation.screens.profile.NotificationsScreen
import com.group8.comp2300.presentation.screens.profile.PrivacyLegaleseScreen
import com.group8.comp2300.presentation.screens.profile.PrivacySecurityScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
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
        var isPinEnabled by remember { mutableStateOf(pinDataSource.isPinSet()) }
        PrivacySecurityScreen(
            onBack = navigator::goBack,
            isPinEnabled = isPinEnabled,
            onVerifyPin = { pin -> pinDataSource.verifyPin(pin) },
            onSavePin = { pin ->
                scope.launch { pinDataSource.savePin(pin) }
                isPinEnabled = true
            },
            onClearPin = {
                scope.launch { pinDataSource.clearPin() }
                isPinEnabled = false
            },
        )
    }

    navigation<Screen.Notifications> {
        val navigator = LocalNavigator.current
        NotificationsScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.Accessibility> {
        val navigator = LocalNavigator.current
        AccessibilityScreen(
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

    navigation<Screen.GuestSignIn> {
        val navigator = LocalNavigator.current
        GuestSignInScreen(
            onRequireAuth = { navigator.requireAuth() },
        )
    }

    navigation<Screen.MedicalRecords> {
        val navigator = LocalNavigator.current
        MedicalRecordScreen(
            viewModel = koinViewModel(),
            onNavigateBack = navigator::goBack,
        )
    }
}
