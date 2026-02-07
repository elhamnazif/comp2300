@file:Suppress("FunctionName")

package com.group8.comp2300

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.di.appModule
import com.group8.comp2300.di.navigationModule
import com.group8.comp2300.di.previewModule
import com.group8.comp2300.domain.model.Screen
import com.group8.comp2300.presentation.navigation.*
import com.group8.comp2300.presentation.ui.screens.auth.AuthViewModel
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import org.koin.compose.KoinApplication
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@OptIn(
    KoinExperimentalAPI::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun App() {
    KoinApplication(
        configuration = koinConfiguration { modules(appModule, navigationModule) },
    ) {
        AppTheme {
            MainApp()
        }
    }
}

/* ------------------------------------------------------------------
 * Theme resolution
 * ------------------------------------------------------------------ */
@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val seedColor = getWallpaperSeedColor() ?: Color(0xFF66ffc7)
    DynamicMaterialTheme(
        seedColor = seedColor,
        isDark = isSystemInDarkTheme(),
        // specVersion = ColorSpec.SpecVersion.SPEC_2025,
        isAmoled = false,
        style = PaletteStyle.TonalSpot,
        animate = true,
        content = content,
    )
}

@Composable
expect fun getWallpaperSeedColor(): Color?

@OptIn(
    KoinExperimentalAPI::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun MainApp(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel(),
    navigator: Navigator = koinViewModel(),
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    // Sync guest state (optional, or handle inside VMs)
    LaunchedEffect(currentUser) { navigator.isGuest = (currentUser == null) }

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

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavigationSuiteScaffold(
            modifier = modifier,
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
                    sceneStrategy = supportingPaneStrategy,
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

@PreviewScreenSizes
@Preview(name = "Onboarding")
@Composable
internal fun PreviewMainApp() {
    KoinApplicationPreview(
        application = {
            modules(
                previewModule,
                navigationModule,
                module { viewModel<Navigator> { FakeNavigator(Screen.Onboarding) } },
            )
        },
    ) {
        val dispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
            MainApp()
        }
    }
}

@PreviewScreenSizes
@Preview(name = "Navigation Tabs")
@Composable
internal fun PreviewNavigationTabs() {
    KoinApplicationPreview(
        application = {
            modules(
                previewModule,
                navigationModule,
                module { viewModel<Navigator> { FakeNavigator(Screen.Home) } },
            )
        },
    ) {
        val dispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
            MainApp()
        }
    }
}
