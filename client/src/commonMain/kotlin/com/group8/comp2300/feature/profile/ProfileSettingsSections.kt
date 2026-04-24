package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.settings.SettingsNavigationRow
import com.group8.comp2300.core.ui.settings.SettingsSection
import com.group8.comp2300.platform.biometrics.isBiometricAvailable
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileSettingsSections(
    isSignedIn: Boolean,
    appLockEnabled: Boolean,
    biometricUnlockEnabled: Boolean,
    onNavigateToMedicalRecords: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToPrivacySecurity: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToPrivacyLegalese: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val privacySecurityDescription = when {
        !appLockEnabled -> stringResource(Res.string.profile_app_lock_off)
        biometricUnlockEnabled && isBiometricAvailable() -> stringResource(Res.string.profile_biometrics_enabled)
        else -> stringResource(Res.string.profile_pin_enabled)
    }
    val primarySectionTotal = if (isSignedIn) 4 else 3

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSection {
            SettingsNavigationRow(
                icon = Icons.ArticleW400Outlined,
                title = stringResource(Res.string.profile_health_records_title),
                description = stringResource(Res.string.profile_track_results_desc),
                index = 0,
                total = primarySectionTotal,
                onClick = onNavigateToMedicalRecords,
            )
            if (isSignedIn) {
                SettingsNavigationRow(
                    icon = Icons.AccountBoxW400Outlinedfill1,
                    title = stringResource(Res.string.profile_account_title),
                    description = stringResource(Res.string.profile_account_desc),
                    index = 1,
                    total = primarySectionTotal,
                    onClick = onNavigateToAccount,
                )
            }
            SettingsNavigationRow(
                icon = Icons.LockW400Outlinedfill1,
                title = stringResource(Res.string.profile_privacy_security_title),
                description = privacySecurityDescription,
                index = if (isSignedIn) 2 else 1,
                total = primarySectionTotal,
                onClick = onNavigateToPrivacySecurity,
            )
            SettingsNavigationRow(
                icon = Icons.ShieldW400Outlinedfill1,
                title = stringResource(Res.string.profile_privacy_legalese_title),
                description = stringResource(Res.string.profile_privacy_legalese_desc),
                index = if (isSignedIn) 3 else 2,
                total = primarySectionTotal,
                onClick = onNavigateToPrivacyLegalese,
            )
        }

        SettingsSection {
            SettingsNavigationRow(
                icon = Icons.VisibilityW400Outlinedfill1,
                title = stringResource(Res.string.profile_accessibility_title),
                description = stringResource(Res.string.profile_accessibility_desc),
                index = 0,
                total = 4,
                onClick = onNavigateToAccessibility,
            )
            SettingsNavigationRow(
                icon = Icons.LightbulbW400Outlinedfill1,
                title = stringResource(Res.string.profile_appearance_title),
                description = stringResource(Res.string.profile_appearance_desc),
                index = 1,
                total = 4,
                onClick = onNavigateToAppearance,
            )
            SettingsNavigationRow(
                icon = Icons.NotificationsW400Outlinedfill1,
                title = stringResource(Res.string.profile_notifications_title),
                description = stringResource(Res.string.profile_notifications_desc),
                index = 2,
                total = 4,
                onClick = onNavigateToNotifications,
            )
            SettingsNavigationRow(
                icon = Icons.InfoW400Outlined,
                title = stringResource(Res.string.profile_help_support_title),
                description = stringResource(Res.string.profile_faqs_desc),
                index = 3,
                total = 4,
                onClick = onNavigateToHelpSupport,
            )
        }

        if (isSignedIn) {
            SettingsSection {
                SettingsNavigationRow(
                    icon = Icons.LogoutW400Outlined,
                    title = stringResource(Res.string.profile_logout_label),
                    index = 0,
                    total = 1,
                    onClick = onLogout,
                )
            }
        }
    }
}
