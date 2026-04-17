package com.group8.comp2300.feature.settings.navigation

import androidx.compose.runtime.*
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.feature.settings.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val settingsGraphModule = module {
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
