package com.group8.comp2300.feature.profile.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.data.local.LocalAuthSettingsDataSource
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.profile.AccountScreen
import com.group8.comp2300.feature.profile.ChangeEmailScreen
import com.group8.comp2300.feature.profile.ChangePasswordScreen
import com.group8.comp2300.feature.profile.DeactivateAccountScreen
import com.group8.comp2300.feature.profile.EditProfileScreen
import com.group8.comp2300.feature.profile.ProfileScreen
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
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
            onRequireAuth = { navigator.requireAuth() },
            onNavigateToEditProfile = { navigator.navigate(Screen.EditProfile) },
            appLockEnabled = localAuthSettings.appLockEnabled,
            biometricUnlockEnabled = localAuthSettings.biometricUnlockEnabled,
            onNavigateToMedicalRecords = {
                if (isSignedIn) navigator.navigate(Screen.MedicalRecords) else navigator.requireAuth(Screen.Profile)
            },
            onNavigateToAccount = {
                if (isSignedIn) navigator.navigate(Screen.Account) else navigator.requireAuth(Screen.Profile)
            },
            onNavigateToPrivacySecurity = { navigator.navigate(Screen.PrivacySecurity) },
            onNavigateToAccessibility = { navigator.navigate(Screen.Accessibility) },
            onNavigateToAppearance = { navigator.navigate(Screen.Appearance) },
            onNavigateToPrivacyLegalese = { navigator.navigate(Screen.PrivacyLegalese()) },
            onNavigateToNotifications = { navigator.navigate(Screen.Notifications) },
            onNavigateToHelpSupport = { navigator.navigate(Screen.HelpSupport) },
        )
    }

    navigation<Screen.EditProfile>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        EditProfileScreen(
            onSave = { navigator.goBack() },
            onBack = { navigator.goBack() },
        )
    }

    navigation<Screen.Account>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()
        AccountScreen(
            email = session.userOrNull?.email.orEmpty(),
            onBack = navigator::goBack,
            onNavigateToChangePassword = { navigator.navigate(Screen.ChangePassword) },
            onNavigateToChangeEmail = { navigator.navigate(Screen.ChangeEmail) },
            onNavigateToDeactivateAccount = { navigator.navigate(Screen.DeactivateAccount) },
        )
    }

    navigation<Screen.ChangePassword>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val successMessage = stringResource(Res.string.account_change_password_success)
        ChangePasswordScreen(
            onBack = navigator::goBack,
            onPasswordChange = {
                navigator.clearAndGoTo(Screen.Login(successMessage = successMessage))
            },
        )
    }

    navigation<Screen.ChangeEmail>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()
        ChangeEmailScreen(
            currentEmail = session.userOrNull?.email.orEmpty(),
            onBack = navigator::goBack,
            onEmailChange = navigator::goBack,
        )
    }

    navigation<Screen.DeactivateAccount>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val successMessage = stringResource(Res.string.account_deactivate_success)
        DeactivateAccountScreen(
            onBack = navigator::goBack,
            onAccountDeactivate = {
                navigator.clearAndGoTo(Screen.Login(successMessage = successMessage))
            },
        )
    }
}
