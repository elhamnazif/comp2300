package com.group8.comp2300.feature.auth.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.data.local.finalizeOnboardingLocalAuth
import com.group8.comp2300.data.local.LocalAuthSettingsDataSource
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.auth.completeprofile.CompleteProfileScreen
import com.group8.comp2300.feature.auth.emailverification.EmailVerificationScreen
import com.group8.comp2300.feature.auth.forgotpassword.ForgotPasswordScreen
import com.group8.comp2300.feature.auth.login.AuthScreen
import com.group8.comp2300.feature.auth.onboarding.OnboardingScreen
import com.group8.comp2300.feature.auth.resetpassword.ResetPasswordScreen
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val authGraphModule = module {
    navigation<Screen.Onboarding> {
        val navigator = LocalNavigator.current
        val authRepository = koinInject<AuthRepository>()
        val localAuthSettingsDataSource = koinInject<LocalAuthSettingsDataSource>()
        val pinDataSource = koinInject<PinDataSource>()
        val session by authRepository.session.collectAsState()
        OnboardingScreen(
            onFinish = { pendingPin ->
                finalizeOnboardingLocalAuth(
                    pendingPin = pendingPin,
                    markOnboardingCompleted = localAuthSettingsDataSource::markOnboardingCompleted,
                    savePin = pinDataSource::savePin,
                    setAppLockEnabled = localAuthSettingsDataSource::setAppLockEnabled,
                    setBiometricUnlockEnabled = localAuthSettingsDataSource::setBiometricUnlockEnabled,
                )
                navigator.clearAndGoTo(Screen.Home)
            },
            isGuest = session !is AuthSession.SignedIn,
            onRequireAuth = { navigator.requireAuth(Screen.Home) },
        )
    }

    navigation<Screen.Login> { screen ->
        val navigator = LocalNavigator.current
        AuthScreen(
            initialSuccessMessage = screen.successMessage,
            onLoginSuccess = navigator::onAuthSuccess,
            onDismiss = navigator::goBack,
            onNavigateToEmailVerification = { email ->
                navigator.navigate(Screen.EmailVerification(email))
            },
            onNavigateToForgotPassword = {
                navigator.navigate(Screen.ForgotPassword)
            },
        )
    }

    navigation<Screen.EmailVerification> { screen ->
        val navigator = LocalNavigator.current
        EmailVerificationScreen(
            email = screen.email,
            onVerified = { navigator.navigate(Screen.CompleteProfile(screen.email)) },
            onBack = { navigator.goBack() },
        )
    }

    navigation<Screen.CompleteProfile> { screen ->
        val navigator = LocalNavigator.current
        CompleteProfileScreen(
            email = screen.email,
            onComplete = { navigator.clearAndGoTo(Screen.Home) },
            onBack = { navigator.goBack() },
        )
    }

    navigation<Screen.ForgotPassword> {
        val navigator = LocalNavigator.current
        ForgotPasswordScreen(
            onBack = { navigator.goBack() },
            onCodeEntered = { code ->
                navigator.navigate(Screen.ResetPassword(code))
            },
        )
    }

    navigation<Screen.ResetPassword> { screen ->
        val navigator = LocalNavigator.current
        ResetPasswordScreen(
            token = screen.token,
            onPasswordReset = { navigator.navigate(Screen.Login()) },
            onBack = { navigator.goBack() },
        )
    }
}
