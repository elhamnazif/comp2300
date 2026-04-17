package com.group8.comp2300.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.settings.SettingsNavigationRow
import com.group8.comp2300.core.ui.settings.SettingsSection
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArticleW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.InfoW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LockW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LogoutW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ShieldW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityW400Outlinedfill1
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.profile_accessibility_desc
import comp2300.i18n.generated.resources.profile_accessibility_title
import comp2300.i18n.generated.resources.profile_biometrics_enabled
import comp2300.i18n.generated.resources.profile_faqs_desc
import comp2300.i18n.generated.resources.profile_health_records_title
import comp2300.i18n.generated.resources.profile_help_support_title
import comp2300.i18n.generated.resources.profile_logout_label
import comp2300.i18n.generated.resources.profile_notifications_desc
import comp2300.i18n.generated.resources.profile_notifications_title
import comp2300.i18n.generated.resources.profile_privacy_legalese_desc
import comp2300.i18n.generated.resources.profile_privacy_legalese_title
import comp2300.i18n.generated.resources.profile_privacy_security_title
import comp2300.i18n.generated.resources.profile_track_results_desc
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileSettingsSections(
    isSignedIn: Boolean,
    onNavigateToMedicalRecords: () -> Unit,
    onNavigateToPrivacySecurity: () -> Unit,
    onNavigateToAccessibility: () -> Unit,
    onNavigateToPrivacyLegalese: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsSection {
            SettingsNavigationRow(
                icon = Icons.ArticleW400Outlined,
                title = stringResource(Res.string.profile_health_records_title),
                description = stringResource(Res.string.profile_track_results_desc),
                index = 0,
                total = 3,
                onClick = onNavigateToMedicalRecords,
            )
            SettingsNavigationRow(
                icon = Icons.LockW400Outlinedfill1,
                title = stringResource(Res.string.profile_privacy_security_title),
                description = stringResource(Res.string.profile_biometrics_enabled),
                index = 1,
                total = 3,
                onClick = onNavigateToPrivacySecurity,
            )
            SettingsNavigationRow(
                icon = Icons.ShieldW400Outlinedfill1,
                title = stringResource(Res.string.profile_privacy_legalese_title),
                description = stringResource(Res.string.profile_privacy_legalese_desc),
                index = 2,
                total = 3,
                onClick = onNavigateToPrivacyLegalese,
            )
        }

        SettingsSection {
            SettingsNavigationRow(
                icon = Icons.VisibilityW400Outlinedfill1,
                title = stringResource(Res.string.profile_accessibility_title),
                description = stringResource(Res.string.profile_accessibility_desc),
                index = 0,
                total = 3,
                onClick = onNavigateToAccessibility,
            )
            SettingsNavigationRow(
                icon = Icons.NotificationsW400Outlinedfill1,
                title = stringResource(Res.string.profile_notifications_title),
                description = stringResource(Res.string.profile_notifications_desc),
                index = 1,
                total = 3,
                onClick = onNavigateToNotifications,
            )
            SettingsNavigationRow(
                icon = Icons.InfoW400Outlined,
                title = stringResource(Res.string.profile_help_support_title),
                description = stringResource(Res.string.profile_faqs_desc),
                index = 2,
                total = 3,
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
