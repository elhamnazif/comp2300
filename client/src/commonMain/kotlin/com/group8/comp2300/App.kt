package com.group8.comp2300

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.group8.comp2300.data.notifications.RoutineNotificationBootstrap
import com.group8.comp2300.data.local.AccessibilitySettingsDataSource
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.di.*
import com.group8.comp2300.presentation.accessibility.grayscale
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.presentation.navigation.*
import com.group8.comp2300.presentation.screens.auth.AuthViewModel
import com.group8.comp2300.presentation.screens.auth.PinLockViewModel
import com.group8.comp2300.presentation.screens.auth.PinScreen
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import com.materialkolor.DynamicMaterialExpressiveTheme
import com.materialkolor.PaletteStyle
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.dsl.koinConfiguration

@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration { modules(coreModule, appModule, platformModule, navigationModule) },
        content = {
            val accessibilitySettingsDataSource: AccessibilitySettingsDataSource = koinInject()
            val accessibilitySettings by accessibilitySettingsDataSource.state.collectAsState()
            val privacySettingsDataSource: PrivacySettingsDataSource = koinInject()
            val privacySettings by privacySettingsDataSource.state.collectAsState()
            AppTheme {
                var isAppInForeground by remember { mutableStateOf(true) }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    isAppInForeground = true
                }
                LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
                    isAppInForeground = false
                }

                val shouldBlurApp = privacySettings.blurAppWhenBackgrounded && !isAppInForeground

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .grayscale(accessibilitySettings.grayscaleEnabled)
                            .let { baseModifier ->
                                if (shouldBlurApp) {
                                    baseModifier.blur(24.dp)
                                } else {
                                    baseModifier
                                }
                            },
                    ) {
                        MainApp()
                    }

                    if (shouldBlurApp) {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.28f)),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun MainApp(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel(),
    navigator: Navigator = koinViewModel(),
    pinLockViewModel: PinLockViewModel = koinViewModel(),
) {
    val notificationBootstrap: RoutineNotificationBootstrap = koinInject()
    val session by authViewModel.session.collectAsState()
    val isPinLocked by pinLockViewModel.isLocked.collectAsState()
    val isPinSet by pinLockViewModel.isPinSet.collectAsState()

    // Route to the correct start screen once PIN check completes
    LaunchedEffect(isPinSet) {
        if (isPinSet != null) {
            navigator.setStartDestination(if (isPinSet == true) Screen.Home else Screen.Onboarding)
        }
    }

    LaunchedEffect(Unit) {
        notificationBootstrap.synchronize()
    }

    if (session is AuthSession.Restoring || isPinSet == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val currentScreen = navigator.currentScreen
    val navigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState()

    val mainTabs =
        listOf(
            Triple(Screen.Home, Icons.HomeW400Outlinedfill1, "Home"),
            Triple(Screen.Booking, Icons.LocationOnW400Outlinedfill1, "Care"),
            Triple(Screen.Calendar, Icons.DateRangeW400Outlinedfill1, "Track"),
            Triple(Screen.Education, Icons.InfoW400Outlinedfill1, "Education"),
            Triple(Screen.Profile, Icons.PersonW400Outlinedfill1, "Me"),
        )
    val showNavBar = currentScreen in mainTabs.map { it.first }

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType =
        if (showNavBar) {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(
                windowAdaptiveInfo,
            )
        } else {
            NavigationSuiteType.None
        }

    // Override the defaults so that there isn't a horizontal or vertical space between the panes.
    val directive =
        remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }

    // Override the defaults so that the list detail pane can be dismissed by pressing back.
    val supportingPaneStrategy =
        rememberListDetailSceneStrategy<Any>(
            backNavigationBehavior =
            BackNavigationBehavior.PopUntilCurrentDestinationChange,
            directive = directive,
        )

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            NavigationSuiteScaffold(
                navigationSuiteItems = {
                    if (showNavBar) {
                        mainTabs.forEach { (screen, icon, label) ->
                            item(
                                icon = { Icon(icon, label) },
                                label = { Text(label) },
                                selected = currentScreen == screen,
                                onClick = { navigator.clearAndGoTo(screen) },
                            )
                        }
                    }
                },
                layoutType = layoutType,
                state = navigationSuiteScaffoldState,
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) { _ ->
                    NavDisplay(
                        backStack = navigator.backStack,
                        sceneStrategies = listOf(supportingPaneStrategy),
                        onBack = navigator::goBack,
                        transitionSpec = { pushAnimation },
                        popTransitionSpec = { popAnimation },
                        predictivePopTransitionSpec = { popAnimation },
                        entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = koinEntryProvider(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isPinLocked,
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                animationSpec = tween(300),
                targetOffsetY = { -it / 4 },
            ),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                PinScreen(
                    onComplete = { pinLockViewModel.onPinEntered(it) },
                    isSetup = false,
                    errorMessage = pinLockViewModel.error.collectAsState().value,
                    onErrorMessageCleared = { pinLockViewModel.clearError() },
                    onBiometricSuccess = { pinLockViewModel.onBiometricUnlock() },
                )
            }
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val seedColor = getWallpaperSeedColor() ?: Color(0xFF66ffc7)
    DynamicMaterialExpressiveTheme(
        seedColor = seedColor,
        isDark = isSystemInDarkTheme(),
        isAmoled = false,
        style = PaletteStyle.Content,
        animate = true,
        content = content,
    )
}

@Composable
expect fun getWallpaperSeedColor(): Color?
