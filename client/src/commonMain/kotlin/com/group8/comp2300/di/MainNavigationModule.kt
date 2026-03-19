package com.group8.comp2300.di

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.home.HomeScreen
import com.group8.comp2300.presentation.screens.medical.MedicationScreen
import com.group8.comp2300.presentation.screens.medical.calendar.CalendarScreen
import com.group8.comp2300.presentation.screens.profile.ProfileScreen
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val mainNavigationModule = module {
    navigation<Screen.Home> {
        val navigator = LocalNavigator.current
        HomeScreen(
            onNavigateToShop = { navigator.navigate(Screen.Shop) },
            onNavigateToCalendar = { navigator.navigate(Screen.Calendar) },
            onNavigateToEducation = { navigator.navigate(Screen.Education) },
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
            onNavigateToRoutines = { navigator.navigate(Screen.Routines) },
            onNavigateToSymptomChecker = { navigator.navigate(Screen.SelfDiagnosis) },
            onNavigateToClinicMap = { navigator.navigate(Screen.Booking) },
        )
    }

    navigation<Screen.Calendar> {
        val navigator = LocalNavigator.current
        CalendarScreen(
            onNavigateToMedication = { navigator.navigate(Screen.Medication) },
        )
    }

    navigation<Screen.Profile> {
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()
        val isSignedIn = session is AuthSession.SignedIn
        ProfileScreen(
            isGuest = !isSignedIn,
            onRequireAuth = { navigator.requireAuth(Screen.Profile) },
            onNavigateToLabResults = {
                if (isSignedIn) navigator.navigate(Screen.LabResults) else navigator.requireAuth(Screen.Profile)
            },
            onNavigateToPrivacySecurity = {
                if (isSignedIn) navigator.navigate(Screen.PrivacySecurity) else navigator.requireAuth(Screen.Profile)
            },
            onNavigateToPrivacyLegalese = { navigator.navigate(Screen.PrivacyLegalese) },
            onNavigateToNotifications = {
                if (isSignedIn) navigator.navigate(Screen.Notifications) else navigator.requireAuth(Screen.Profile)
            },
            onNavigateToHelpSupport = { navigator.navigate(Screen.HelpSupport) },
        )
    }

    navigation<Screen.Medication> {
        val navigator = LocalNavigator.current
        MedicationScreen(
            onBack = navigator::goBack,
            onNavigateToRoutines = { navigator.navigate(Screen.Routines) },
        )
    }
}
