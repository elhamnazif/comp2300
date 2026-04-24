package com.group8.comp2300.feature.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.group8.comp2300.core.ui.settings.SettingsDetailScaffold
import com.group8.comp2300.core.ui.settings.SettingsInfoCard
import com.group8.comp2300.core.ui.settings.SettingsNavigationRow
import com.group8.comp2300.core.ui.settings.SettingsSection
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LockW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MailOutlineW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountScreen(
    email: String,
    onBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToChangeEmail: () -> Unit,
    onNavigateToDeactivateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsDetailScaffold(
        title = stringResource(Res.string.account_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsInfoCard(
                title = stringResource(Res.string.account_current_email_title),
                description = email,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        item {
            SettingsSection {
                SettingsNavigationRow(
                    icon = Icons.MailOutlineW400Outlinedfill1,
                    title = stringResource(Res.string.account_change_email_title),
                    description = stringResource(Res.string.account_change_email_desc),
                    index = 0,
                    total = 3,
                    onClick = onNavigateToChangeEmail,
                )
                SettingsNavigationRow(
                    icon = Icons.LockW400Outlinedfill1,
                    title = stringResource(Res.string.account_change_password_title),
                    description = stringResource(Res.string.account_change_password_desc),
                    index = 1,
                    total = 3,
                    onClick = onNavigateToChangePassword,
                )
                SettingsNavigationRow(
                    icon = Icons.DeleteW400Outlinedfill1,
                    title = stringResource(Res.string.account_deactivate_title),
                    description = stringResource(Res.string.account_deactivate_desc),
                    index = 2,
                    total = 3,
                    onClick = onNavigateToDeactivateAccount,
                )
            }
        }
    }
}
