package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onRequireAuth: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenHeader(horizontalPadding = 0.dp, topPadding = 16.dp) {
        Column(modifier.padding(horizontal = 16.dp)) {
            Header(
                state,
                onRequireAuth = onRequireAuth,
                onNavigateToEditProfile = onNavigateToEditProfile,
            )
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
private fun Header(
    state: ProfileViewModel.State,
    onRequireAuth: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
) {
    if (!state.isLoading && !state.isSignedIn) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(28.dp),
            onClick = onRequireAuth,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(
                    initials = stringResource(Res.string.profile_default_user_initials),
                    imageModel = null,
                )
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
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            onClick = onNavigateToEditProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(
                    state.userInitials.ifEmpty {
                        stringResource(Res.string.profile_default_user_initials)
                    },
                    imageModel = state.profileImageUrl,
                    isLoading = state.isLoading,
                )
                Spacer(Modifier.size(16.dp))
                UserInfo(
                    name = state.userName,
                    memberSince = state.memberSince,
                    isLoading = state.isLoading,
                    modifier = Modifier.weight(1f),
                )
                if (state.isSignedIn) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.ChevronRightW400Outlined,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
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
