package com.group8.comp2300.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.group8.comp2300.app.navigation.*
import com.group8.comp2300.core.security.pin.PinLockViewModel
import com.group8.comp2300.core.security.pin.PinScreen
import com.group8.comp2300.data.local.LocalAuthSettingsDataSource
import com.group8.comp2300.data.local.NotificationSettingsDataSource
import com.group8.comp2300.data.local.PinDataSource
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.data.notifications.AppointmentNotificationBootstrap
import com.group8.comp2300.data.notifications.RoutineNotificationBootstrap
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.feature.auth.login.AuthViewModel
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

private val mainTabs =
    listOf(
        MainTab(Screen.Home, Icons.HomeW400Outlinedfill1, "Home"),
        MainTab(Screen.Booking, Icons.LocationOnW400Outlinedfill1, "Care"),
        MainTab(Screen.Calendar, Icons.DateRangeW400Outlinedfill1, "Track"),
        MainTab(Screen.Education, Icons.SchoolW400Outlinedfill1, "Education"),
        MainTab(Screen.Profile, Icons.PersonW400Outlinedfill1, "Me"),
    )

@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel(),
    navigator: Navigator = koinViewModel(),
    pinLockViewModel: PinLockViewModel = koinViewModel(),
) {
    val routineNotificationBootstrap: RoutineNotificationBootstrap = koinInject()
    val appointmentNotificationBootstrap: AppointmentNotificationBootstrap = koinInject()
    val localAuthSettingsDataSource: LocalAuthSettingsDataSource = koinInject()
    val notificationSettingsDataSource: NotificationSettingsDataSource = koinInject()
    val pinDataSource: PinDataSource = koinInject()
    val privacySettingsDataSource: PrivacySettingsDataSource = koinInject()
    val session by authViewModel.session.collectAsState()
    val localAuthSettings by localAuthSettingsDataSource.state.collectAsState()
    val notificationSettings by notificationSettingsDataSource.state.collectAsState()
    val hasPin by pinDataSource.pinSet.collectAsState()
    val isPinLocked by pinLockViewModel.isLocked.collectAsState()
    val isPinInputLocked by pinLockViewModel.isInputLocked.collectAsState()
    val privacySettings by privacySettingsDataSource.state.collectAsState()

    LaunchedEffect(localAuthSettings.onboardingCompleted) {
        navigator.setStartDestination(if (localAuthSettings.onboardingCompleted) Screen.Home else Screen.Onboarding)
    }

    LaunchedEffect(routineNotificationBootstrap, appointmentNotificationBootstrap) {
        snapshotFlow {
            NotificationSyncState(
                sessionKey = when (val currentSession = session) {
                    AuthSession.Restoring -> "restoring"
                    AuthSession.SignedOut -> "signed_out"
                    is AuthSession.SignedIn -> "signed_in:${currentSession.user.id}:${currentSession.isStale}"
                },
                notificationPrivacyMode = privacySettings.notificationPrivacyMode,
                notificationAlias = privacySettings.notificationAlias,
                routineRemindersEnabled = notificationSettings.routineRemindersEnabled,
                appointmentRemindersEnabled = notificationSettings.appointmentRemindersEnabled,
            )
        }
            .collectLatest {
                delay(250.milliseconds)
                routineNotificationBootstrap.synchronize()
                appointmentNotificationBootstrap.synchronize()
            }
    }

    if (session is AuthSession.Restoring) {
        AppLoadingState(modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppNavigationShell(navigator = navigator)
        PinLockOverlay(
            visible = localAuthSettings.appLockEnabled && hasPin && isPinLocked,
            errorMessage = pinLockViewModel.error.collectAsState().value,
            inputEnabled = !isPinInputLocked,
            onComplete = pinLockViewModel::onPinEntered,
            onErrorMessageCleared = pinLockViewModel::clearError,
            onBiometricSuccess = if (localAuthSettings.appLockEnabled && localAuthSettings.biometricUnlockEnabled) {
                pinLockViewModel::onBiometricUnlock
            } else {
                null
            },
        )
    }
}

private data class NotificationSyncState(
    val sessionKey: String,
    val notificationPrivacyMode: com.group8.comp2300.data.local.NotificationPrivacyMode,
    val notificationAlias: String,
    val routineRemindersEnabled: Boolean,
    val appointmentRemindersEnabled: Boolean,
)

@Composable
private fun AppLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppNavigationShell(navigator: Navigator, modifier: Modifier = Modifier) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive =
        remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val supportingPaneStrategy =
        rememberListDetailSceneStrategy<Screen>(
            backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
            directive = directive,
        )
    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            modifier = modifier.fillMaxSize(),
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

@Composable
fun MainShellScreen(navigator: Navigator = LocalNavigator.current) {
    val selectedTab = navigator.currentTab ?: Screen.Home
    val navigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState()
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val shellBackStack = navigator.mainShellBackStack
    val navigationSuiteType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)
    val useRootOverlayForShellChildren = navigationSuiteType == NavigationSuiteType.NavigationBar
    val directive =
        remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val supportingPaneStrategy =
        rememberListDetailSceneStrategy<Screen>(
            backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
            directive = directive,
        )
    val entryProvider = koinEntryProvider<Screen>()
    val mobileTabEntries = rememberMainTabEntries(entryProvider)

    CompositionLocalProvider(LocalUseRootOverlayForShellChildren provides useRootOverlayForShellChildren) {
        NavigationSuiteScaffold(
            modifier = Modifier.fillMaxSize(),
            navigationSuiteItems = {
                mainTabs.forEach { tab ->
                    item(
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                        selected = selectedTab == tab.screen,
                        onClick = { navigator.navigate(tab.screen) },
                    )
                }
            },
            layoutType = navigationSuiteType,
            state = navigationSuiteScaffoldState,
        ) {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) { _ ->
                if (useRootOverlayForShellChildren) {
                    NavDisplay(
                        entries = requireNotNull(mobileTabEntries[selectedTab]),
                        sceneStrategies = listOf(supportingPaneStrategy),
                        onBack = navigator::goBackWithinShell,
                        transitionSpec = { pushAnimation },
                        popTransitionSpec = { popAnimation },
                        predictivePopTransitionSpec = { popAnimation },
                    )
                } else {
                    NavDisplay(
                        backStack = shellBackStack,
                        sceneStrategies = listOf(supportingPaneStrategy),
                        onBack = navigator::goBackWithinShell,
                        transitionSpec = { pushAnimation },
                        popTransitionSpec = { popAnimation },
                        predictivePopTransitionSpec = { popAnimation },
                        entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberMainTabEntries(entryProvider: (Screen) -> NavEntry<Screen>): Map<Screen, List<NavEntry<Screen>>> =
    buildMap {
        mainTabs.forEach { tab ->
            val tabBackStack = remember(tab.screen) { listOf(tab.screen) }

            // Keep each tab's state and ViewModel store separate while exposing only the selected
            // tab to the mobile shell NavDisplay, so predictive back exits the app from tab roots.
            val entries =
                rememberDecoratedNavEntries(
                    backStack = tabBackStack,
                    entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    entryProvider = entryProvider,
                )

            put(tab.screen, entries)
        }
    }

@Composable
private fun PinLockOverlay(
    visible: Boolean,
    errorMessage: String?,
    inputEnabled: Boolean,
    onComplete: (String) -> Unit,
    onErrorMessageCleared: () -> Unit,
    onBiometricSuccess: (() -> Unit)?,
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
                    inputEnabled = inputEnabled,
                    onErrorMessageCleared = onErrorMessageCleared,
                    onBiometricSuccess = onBiometricSuccess,
                )
        }
    }
}

private data class MainTab(val screen: Screen, val icon: ImageVector, val label: String)
