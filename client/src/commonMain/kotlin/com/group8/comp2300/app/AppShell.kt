package com.group8.comp2300.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.group8.comp2300.app.navigation.*
import com.group8.comp2300.core.security.pin.PinLockViewModel
import com.group8.comp2300.core.security.pin.PinScreen
import com.group8.comp2300.data.notifications.RoutineNotificationBootstrap
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.feature.auth.login.AuthViewModel
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel

private val mainTabs =
    listOf(
        MainTab(Screen.Home, Icons.HomeW400Outlinedfill1, "Home"),
        MainTab(Screen.Booking, Icons.LocationOnW400Outlinedfill1, "Care"),
        MainTab(Screen.Calendar, Icons.DateRangeW400Outlinedfill1, "Track"),
        MainTab(Screen.Education, Icons.InfoW400Outlinedfill1, "Education"),
        MainTab(Screen.Profile, Icons.PersonW400Outlinedfill1, "Me"),
    )

@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel(),
    navigator: Navigator = koinViewModel(),
    pinLockViewModel: PinLockViewModel = koinViewModel(),
) {
    val notificationBootstrap: RoutineNotificationBootstrap = koinInject()
    val session by authViewModel.session.collectAsState()
    val isPinLocked by pinLockViewModel.isLocked.collectAsState()
    val isPinSet by pinLockViewModel.isPinSet.collectAsState()

    LaunchedEffect(isPinSet) {
        if (isPinSet != null) {
            navigator.setStartDestination(if (isPinSet == true) Screen.Home else Screen.Onboarding)
        }
    }

    LaunchedEffect(Unit) {
        notificationBootstrap.synchronize()
    }

    if (session is AuthSession.Restoring || isPinSet == null) {
        AppLoadingState(modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppNavigationShell(navigator = navigator)
        PinLockOverlay(
            visible = isPinLocked,
            errorMessage = pinLockViewModel.error.collectAsState().value,
            onComplete = pinLockViewModel::onPinEntered,
            onErrorMessageCleared = pinLockViewModel::clearError,
            onBiometricSuccess = pinLockViewModel::onBiometricUnlock,
    )
}
}

@Composable
private fun AppLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppNavigationShell(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val currentScreen = navigator.currentScreen
    val showNavBar = currentScreen in mainTabs.map(MainTab::screen)
    val navigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType =
        if (showNavBar) {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)
        } else {
            NavigationSuiteType.None
        }
    val directive =
        remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val supportingPaneStrategy =
        rememberListDetailSceneStrategy<Any>(
            backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
            directive = directive,
        )

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavigationSuiteScaffold(
            modifier = modifier.fillMaxSize(),
            navigationSuiteItems = {
                if (showNavBar) {
                    mainTabs.forEach { tab ->
                        item(
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) },
                            selected = currentScreen == tab.screen,
                            onClick = { navigator.clearAndGoTo(tab.screen) },
                        )
                    }
                }
            },
            layoutType = layoutType,
            state = navigationSuiteScaffoldState,
        ) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
}

@Composable
private fun PinLockOverlay(
    visible: Boolean,
    errorMessage: String?,
    onComplete: (String) -> Unit,
    onErrorMessageCleared: () -> Unit,
    onBiometricSuccess: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
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
                onComplete = onComplete,
                isSetup = false,
                errorMessage = errorMessage,
                onErrorMessageCleared = onErrorMessageCleared,
                onBiometricSuccess = onBiometricSuccess,
            )
        }
    }
}

private data class MainTab(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
)
