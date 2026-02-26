package com.group8.comp2300.di

import com.group8.comp2300.presentation.navigation.LocalNavigator
import com.group8.comp2300.presentation.navigation.Screen
import com.group8.comp2300.presentation.screens.auth.LoginScreen
import com.group8.comp2300.presentation.screens.auth.OnboardingScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val authNavigationModule = module {
    navigation<Screen.Onboarding> {
        val navigator = LocalNavigator.current
        OnboardingScreen(
            onFinish = { navigator.clearAndGoTo(Screen.Home) },
            isGuest = navigator.isGuest,
            onRequireAuth = navigator::requireAuth
        )
    }

    navigation<Screen.Login> {
        val navigator = LocalNavigator.current
        LoginScreen(
            onLoginSuccess = navigator::goBack,
            onDismiss = navigator::goBack
        )
    }
}
