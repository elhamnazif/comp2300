package com.group8.comp2300.feature.profile

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToGuestSignIn: () -> Unit = {},
    onNavigateToLabResults: () -> Unit = {},
    onNavigateToMedicalRecords: () -> Unit = {},
    onNavigateToPrivacySecurity: () -> Unit = {},
    onNavigateToAccessibility: () -> Unit = {},
    onNavigateToPrivacyLegalese: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.state.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(Res.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(Res.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.logout()
                }) {
                    Text(stringResource(Res.string.profile_logout_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

    val scaleFraction = {
        if (uiState.isLoading) {
            1f
        } else {
            LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = uiState.isLoading,
                onRefresh = viewModel::refresh,
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProfileOverviewSection(
                    state = uiState,
                    onNavigateToLabResults = onNavigateToLabResults,
                    onNavigateToGuestSignIn = onNavigateToGuestSignIn,
                )
            }
            item {
                ProfileSettingsSections(
                    isSignedIn = uiState.userName.isNotEmpty(),
                    onNavigateToMedicalRecords = onNavigateToMedicalRecords,
                    onNavigateToPrivacySecurity = onNavigateToPrivacySecurity,
                    onNavigateToAccessibility = onNavigateToAccessibility,
                    onNavigateToPrivacyLegalese = onNavigateToPrivacyLegalese,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToHelpSupport = onNavigateToHelpSupport,
                    onLogout = { showLogoutConfirm = true },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .graphicsLayer {
                    scaleX = scaleFraction()
                    scaleY = scaleFraction()
                },
        ) {
            PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = uiState.isLoading)
        }
    }
}
