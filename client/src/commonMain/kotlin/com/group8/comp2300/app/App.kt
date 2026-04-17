package com.group8.comp2300.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.group8.comp2300.core.ui.accessibility.grayscale
import com.group8.comp2300.data.local.AccessibilitySettingsDataSource
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.di.coreModule
import com.group8.comp2300.di.platformModule
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration { modules(coreModule, appModule, platformModule, navigationGraphModule) },
        content = {
            val accessibilitySettingsDataSource: AccessibilitySettingsDataSource = koinInject()
            val accessibilitySettings by accessibilitySettingsDataSource.state.collectAsState()
            val privacySettingsDataSource: PrivacySettingsDataSource = koinInject()
            val privacySettings by privacySettingsDataSource.state.collectAsState()
            AppTheme {
                AppPrivacyMask(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .grayscale(accessibilitySettings.grayscaleEnabled),
                    shouldBlurWhenBackgrounded = privacySettings.blurAppWhenBackgrounded,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppShell()
                    }
                }
            }
        },
    )
}
