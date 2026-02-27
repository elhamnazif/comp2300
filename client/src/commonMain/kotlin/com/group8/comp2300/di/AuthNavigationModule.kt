package com.group8.comp2300.di

import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.auth.AuthScreen
import com.group8.comp2300.presentation.screens.auth.CompleteProfileScreen
import com.group8.comp2300.presentation.screens.auth.EmailVerificationScreen
import com.group8.comp2300.presentation.screens.auth.ForgotPasswordScreen
import com.group8.comp2300.presentation.screens.auth.OnboardingScreen
import com.group8.comp2300.presentation.screens.auth.ResetPasswordScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val authNavigationModule = module {
    navigation<Screen.Onboarding> {
        val navigator = LocalNavigator.current
        OnboardingScreen(
            onFinish = { navigator.clearAndGoTo(Screen.Home) },
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth,
        )
    }

    navigation<Screen.Login> {
        val navigator = LocalNavigator.current
        AuthScreen(
            onLoginSuccess = navigator::goBack,
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
            onPasswordReset = { navigator.navigate(Screen.Login) },
            onBack = { navigator.goBack() },
        )
    }
}
