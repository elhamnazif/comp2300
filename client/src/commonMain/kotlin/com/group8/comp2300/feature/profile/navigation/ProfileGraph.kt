package com.group8.comp2300.feature.profile.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.data.local.LocalAuthSettingsDataSource
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.profile.GuestSignInScreen
import com.group8.comp2300.feature.profile.ProfileScreen
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val profileGraphModule = module {
    navigation<Screen.Profile> {
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val localAuthSettingsDataSource = koinInject<LocalAuthSettingsDataSource>()
        val session by authRepository.session.collectAsState()
        val localAuthSettings by localAuthSettingsDataSource.state.collectAsState()
        val isSignedIn = session is AuthSession.SignedIn
        ProfileScreen(
            onNavigateToGuestSignIn = { navigator.navigate(Screen.GuestSignIn) },
            appLockEnabled = localAuthSettings.appLockEnabled,
            biometricUnlockEnabled = localAuthSettings.biometricUnlockEnabled,
            onNavigateToMedicalRecords = {
                if (isSignedIn) navigator.navigate(Screen.MedicalRecords) else navigator.requireAuth(Screen.Profile)
            },
            onNavigateToPrivacySecurity = { navigator.navigate(Screen.PrivacySecurity) },
            onNavigateToAccessibility = { navigator.navigate(Screen.Accessibility) },
            onNavigateToPrivacyLegalese = { navigator.navigate(Screen.PrivacyLegalese) },
            onNavigateToNotifications = { navigator.navigate(Screen.Notifications) },
            onNavigateToHelpSupport = { navigator.navigate(Screen.HelpSupport) },
        )
    }

    navigation<Screen.GuestSignIn>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        GuestSignInScreen(
            onRequireAuth = { navigator.requireAuth() },
        )
    }
}
