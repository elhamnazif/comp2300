package com.group8.comp2300.feature.settings.navigation

import androidx.compose.runtime.*
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.data.local.LocalAuthSettingsDataSource
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.feature.settings.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val settingsGraphModule = module {
    navigation<Screen.PrivacySecurity>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val localAuthSettingsDataSource = koinInject<LocalAuthSettingsDataSource>()
        val pinDataSource = koinInject<PinDataSource>()
        val localAuthSettings by localAuthSettingsDataSource.state.collectAsState()
        val scope = rememberCoroutineScope()
        val isPinEnabled by pinDataSource.pinSet.collectAsState()
        PrivacySecurityScreen(
            onBack = navigator::goBack,
            appLockEnabled = localAuthSettings.appLockEnabled,
            biometricsEnabled = localAuthSettings.biometricUnlockEnabled,
            onVerifyPin = { pin -> pinDataSource.verifyPin(pin) },
            onSavePin = { pin ->
                scope.launch {
                    pinDataSource.savePin(pin)
                    localAuthSettingsDataSource.setAppLockEnabled(true)
                    localAuthSettingsDataSource.setBiometricUnlockEnabled(true)
                }
            },
            onDisableAppLock = {
                scope.launch {
                    pinDataSource.clearPin()
                    localAuthSettingsDataSource.setAppLockEnabled(false)
                    localAuthSettingsDataSource.setBiometricUnlockEnabled(false)
                }
            },
            onBiometricsEnabledChange = localAuthSettingsDataSource::setBiometricUnlockEnabled,
        )
    }

    navigation<Screen.Notifications>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        NotificationsScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.Accessibility>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        AccessibilityScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.HelpSupport>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        HelpSupportScreen(
            onBack = navigator::goBack,
        )
    }

    navigation<Screen.PrivacyLegalese>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        PrivacyLegaleseScreen(
            onBack = navigator::goBack,
        )
    }
}
