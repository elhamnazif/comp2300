package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.ScreenHeader
import com.group8.comp2300.core.ui.components.shimmerEffect
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileOverviewSection(
    state: ProfileViewModel.State,
    onNavigateToGuestSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenHeader(horizontalPadding = 0.dp, topPadding = 16.dp) {
        Column(modifier.padding(horizontal = 16.dp)) {
            Header(state, onNavigateToGuestSignIn = onNavigateToGuestSignIn)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(Res.string.profile_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
internal fun NotLoggedInContent(onRequireAuth: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(Res.string.profile_why_account_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.CheckCircleW400Outlinedfill1,
            title = stringResource(Res.string.profile_track_results_title),
            description = stringResource(Res.string.profile_track_results_desc),
        )

        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.DateRangeW400Outlinedfill1,
            title = stringResource(Res.string.profile_schedule_screenings_title),
            description = stringResource(Res.string.profile_schedule_screenings_desc),
        )

        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.LockW400Outlinedfill1,
            title = stringResource(Res.string.profile_private_secure_title),
            description = stringResource(Res.string.profile_private_secure_desc),
        )

        Spacer(Modifier.height(12.dp))
        FeatureCard(
            icon = Icons.SendW400Outlinedfill1,
            title = stringResource(Res.string.profile_anonymous_partner_title),
            description = stringResource(Res.string.profile_anonymous_partner_desc),
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequireAuth,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                stringResource(Res.string.profile_sign_in_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, description: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun Header(state: ProfileViewModel.State, onNavigateToGuestSignIn: () -> Unit) {
    if (!state.isLoading && state.userName.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(28.dp),
            onClick = onNavigateToGuestSignIn,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(stringResource(Res.string.profile_default_user_initials))
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.profile_sign_in_label),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(Res.string.profile_sign_in_card_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Icon(
                    Icons.ChevronRightW400Outlined,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                state.userInitials.ifEmpty {
                    stringResource(Res.string.profile_default_user_initials)
                },
                isLoading = state.isLoading,
            )
            Spacer(Modifier.size(16.dp))
            UserInfo(state.userName, state.memberSince, isLoading = state.isLoading)
        }
    }
}

@Composable
private fun Avatar(initials: String, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier =
        modifier.size(80.dp).then(if (isLoading && initials.isEmpty()) Modifier.shimmerEffect() else Modifier),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!isLoading || initials.isNotEmpty()) {
                Text(
                    initials,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun UserInfo(name: String, memberSince: String, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    Column(modifier) {
        if (isLoading && name.isEmpty()) {
            Box(Modifier.fillMaxWidth(0.4f).height(24.dp).shimmerEffect())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(0.25f).height(16.dp).shimmerEffect())
        } else {
            Text(
                name.ifEmpty { stringResource(Res.string.profile_default_user_name) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                memberSince.ifEmpty {
                    stringResource(Res.string.profile_member_since_format, "2024")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
