@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.profile

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.presentation.components.ScreenHeader
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.model.medical.LabStatus
import com.group8.comp2300.presentation.components.shimmerEffect
import com.group8.comp2300.presentation.util.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ChevronRightW400Outlined
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    isGuest: Boolean = false,
    onRequireAuth: () -> Unit = {},
    onNavigateToLabResults: () -> Unit = {},
    onNavigateToPrivacySecurity: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isGuest) {
        NotLoggedInContent(onRequireAuth = onRequireAuth)
    } else {
        val uiState by viewModel.state.collectAsState()
        val pullToRefreshState = rememberPullToRefreshState()

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
                .background(MaterialTheme.colorScheme.surface)
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isLoading,
                    onRefresh = viewModel::refresh
                )
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ScreenHeader(horizontalPadding = 0.dp) {
                    InsetContent(uiState, onNavigateToLabResults)
                }
                EdgeToEdgeSettings(
                    onNavigateToPrivacySecurity = onNavigateToPrivacySecurity,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onNavigateToHelpSupport = onNavigateToHelpSupport
                )
            }

            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .graphicsLayer {
                        scaleX = scaleFraction()
                        scaleY = scaleFraction()
                    }
            ) {
                PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = uiState.isLoading)
            }
        }
    }
}

/* ------------------  NOT LOGGED IN CONTENT  ------------------ */
@Composable
private fun NotLoggedInContent(onRequireAuth: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(horizontalPadding = 0.dp) {
            Spacer(Modifier.height(24.dp))

            // Hero Section
            HeroSection()

            Spacer(Modifier.height(40.dp))

            // Feature Preview Cards
            Text(
                stringResource(Res.string.profile_why_account_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            FeatureCard(
                icon = Icons.CheckCircleW400Outlinedfill1,
                title = stringResource(Res.string.profile_track_results_title),
                description = stringResource(Res.string.profile_track_results_desc)
            )

            Spacer(Modifier.height(12.dp))

            FeatureCard(
                icon = Icons.DateRangeW400Outlinedfill1,
                title = stringResource(Res.string.profile_schedule_screenings_title),
                description = stringResource(Res.string.profile_schedule_screenings_desc)
            )

            Spacer(Modifier.height(12.dp))

            FeatureCard(
                icon = Icons.LockW400Outlinedfill1,
                title = stringResource(Res.string.profile_private_secure_title),
                description = stringResource(Res.string.profile_private_secure_desc)
            )

            Spacer(Modifier.height(12.dp))

            FeatureCard(
                icon = Icons.SendW400Outlinedfill1,
                title = stringResource(Res.string.profile_anonymous_partner_title),
                description = stringResource(Res.string.profile_anonymous_partner_desc)
            )

            Spacer(Modifier.height(32.dp))

            // Call to Action Section
            Button(
                onClick = onRequireAuth,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    stringResource(Res.string.profile_sign_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(onClick = onRequireAuth, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(Res.string.profile_create_account), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(Res.string.profile_guest_limited_access),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeroSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Hero Icon
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.PersonW400Outlinedfill1,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Hero Text
        Text(
            text = stringResource(Res.string.profile_hero_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.profile_hero_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/* ------------------  INSET CONTENT  ------------------ */
@Composable
private fun InsetContent(
    state: ProfileViewModel.State,
    onNavigateToLabResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        Header(state)
        Spacer(Modifier.height(24.dp))
        RecentResultsCard(state.recentResults, onNavigateToLabResults)
        Spacer(Modifier.height(24.dp))
        CommunityCard()
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(Res.string.profile_settings_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/* ------------------  HEADER  ------------------ */
@Composable
private fun Header(state: ProfileViewModel.State) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            state.userInitials.ifEmpty {
                stringResource(Res.string.profile_default_user_initials)
            },
            isLoading = state.isLoading
        )
        Spacer(Modifier.width(16.dp))
        UserInfo(state.userName, state.memberSince, isLoading = state.isLoading)
    }
}

@Composable
private fun Avatar(initials: String, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier =
            modifier.size(80.dp).then(if (isLoading && initials.isEmpty()) Modifier.shimmerEffect() else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!isLoading || initials.isNotEmpty()) {
                Text(
                    initials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun UserInfo(name: String, memberSince: String, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    Column(modifier) {
        if (isLoading && name.isEmpty()) {
            Box(Modifier.width(120.dp).height(24.dp).shimmerEffect())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(80.dp).height(16.dp).shimmerEffect())
        } else {
            Text(
                name.ifEmpty { stringResource(Res.string.profile_default_user_name) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                memberSince.ifEmpty {
                    stringResource(Res.string.profile_member_since_format, "2024")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/* ------------------  RECENT RESULTS  ------------------ */
@Composable
private fun RecentResultsCard(
    results: List<LabResult>,
    onNavigateToLabResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
        onClick = onNavigateToLabResults
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(Res.string.profile_recent_results_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            if (results.isEmpty()) {
                // Simplified loading state for results
                repeat(2) {
                    ResultShimmer()
                    Spacer(Modifier.height(12.dp))
                }
            } else {
                results.forEachIndexed { index, result ->
                    ResultRow(result)
                    if (index < results.size - 1) {
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.profile_schedule_next_screening))
            }
        }
    }
}

@Composable
private fun ResultRow(result: LabResult, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(result.testName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                DateFormatter.formatMonthDayYear(result.testDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        StatusSurface(result)
    }
}

@Composable
private fun ResultShimmer() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.6f).height(16.dp).shimmerEffect())
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(0.3f).height(12.dp).shimmerEffect())
        }
        Box(Modifier.width(80.dp).height(24.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
    }
}

@Composable
private fun StatusSurface(result: LabResult) {
    val bgColor = if (result.isPositive) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val textColor = if (result.isPositive) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
    val statusRes =
        when (result.status) {
            LabStatus.PENDING -> Res.string.lab_status_pending
            LabStatus.NEGATIVE -> Res.string.lab_status_negative
            LabStatus.POSITIVE -> Res.string.lab_status_positive
            LabStatus.INCONCLUSIVE -> Res.string.lab_status_inconclusive
        }
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            stringResource(statusRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ------------------  COMMUNITY CARD  ------------------ */
@Composable
private fun CommunityCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        onClick = {},
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.SendW400Outlinedfill1, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(Res.string.profile_notify_partners_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    stringResource(Res.string.profile_notify_partners_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

/* ------------------  EDGE-TO-EDGE SETTINGS  ------------------ */
@Composable
private fun EdgeToEdgeSettings(
    onNavigateToPrivacySecurity: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        SettingsItem(
            icon = Icons.LockW400Outlinedfill1,
            title = stringResource(Res.string.profile_privacy_security_title),
            subtitle = stringResource(Res.string.profile_biometrics_enabled),
            onClick = onNavigateToPrivacySecurity
        )
        SettingsItem(
            icon = Icons.NotificationsW400Outlinedfill1,
            title = stringResource(Res.string.profile_notifications_title),
            subtitle = stringResource(Res.string.profile_notifications_desc),
            onClick = onNavigateToNotifications
        )
        SettingsItem(
            icon = Icons.InfoW400Outlined,
            title = stringResource(Res.string.profile_help_support_title),
            subtitle = stringResource(Res.string.profile_faqs_desc),
            onClick = onNavigateToHelpSupport
        )
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(vertical = 12.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Icon(
                Icons.ChevronRightW400Outlined,
                null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
