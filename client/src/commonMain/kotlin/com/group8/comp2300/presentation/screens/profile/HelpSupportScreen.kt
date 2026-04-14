package com.group8.comp2300.presentation.screens.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.group8.comp2300.mock.faqs
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun HelpSupportScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }

    SettingsDetailScaffold(
        title = stringResource(Res.string.help_support_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsSection(title = stringResource(Res.string.help_support_faq_title)) {
                faqs.forEachIndexed { index, (question, answer) ->
                    SettingsExpandableRow(
                        title = question,
                        description = answer,
                        expanded = expandedFaqIndex == index,
                        index = index,
                        total = faqs.size,
                        onClick = {
                            expandedFaqIndex =
                                if (expandedFaqIndex == index) {
                                    null
                                } else {
                                    index
                                }
                        },
                    )
                }
            }
        }
        item {
            SettingsSection(title = stringResource(Res.string.help_support_contact_title)) {
                SettingsNavigationRow(
                    icon = Icons.MailOutlineW400Outlinedfill1,
                    title = stringResource(Res.string.help_support_email_label),
                    description = stringResource(Res.string.help_support_email_val),
                    index = 0,
                    total = 3,
                    onClick = {},
                )
                SettingsNavigationRow(
                    icon = Icons.CallW400Outlinedfill1,
                    title = stringResource(Res.string.help_support_phone_label),
                    description = stringResource(Res.string.help_support_phone_val),
                    index = 1,
                    total = 3,
                    onClick = {},
                )
                SettingsNavigationRow(
                    icon = Icons.InfoW400Outlinedfill1,
                    title = stringResource(Res.string.help_support_resources_label),
                    description = stringResource(Res.string.help_support_resources_desc),
                    index = 2,
                    total = 3,
                    onClick = {},
                )
            }
        }
        item {
            SettingsInfoCard(
                title = stringResource(Res.string.help_support_emergency_title),
                description = stringResource(Res.string.help_support_emergency_desc),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
