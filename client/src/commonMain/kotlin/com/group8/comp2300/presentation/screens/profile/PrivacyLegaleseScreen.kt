package com.group8.comp2300.presentation.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacyLegaleseScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = stringResource(Res.string.profile_privacy_legalese_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            SettingsSection(
                title = stringResource(Res.string.privacy_legalese_overview_title),
                description = stringResource(Res.string.privacy_legalese_overview_body),
            ) {
                SettingsNavigationRow(
                    icon = Icons.DescriptionW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_legalese_terms_title),
                    description = stringResource(Res.string.privacy_legalese_terms_desc),
                    index = 0,
                    total = 2,
                    onClick = { showTerms = true },
                )
                SettingsNavigationRow(
                    icon = Icons.ShieldW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_legalese_privacy_title),
                    description = stringResource(Res.string.privacy_legalese_privacy_desc),
                    index = 1,
                    total = 2,
                    onClick = { showPrivacyPolicy = true },
                )
            }
        }
        item {
            SettingsInfoCard(
                title = stringResource(Res.string.privacy_legalese_contact_title),
                description = stringResource(Res.string.privacy_legalese_contact_body),
            )
        }
    }

    // Terms of Service Dialog
    if (showTerms) {
        LegalDocumentDialog(
            title = stringResource(Res.string.privacy_legalese_terms_title),
            content = stringResource(Res.string.privacy_legalese_terms_content),
            onDismiss = { showTerms = false },
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicy) {
        LegalDocumentDialog(
            title = stringResource(Res.string.privacy_legalese_privacy_title),
            content = stringResource(Res.string.privacy_legalese_privacy_content),
            onDismiss = { showPrivacyPolicy = false },
        )
    }
}

@Composable
private fun LegalDocumentDialog(title: String, content: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(content, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.privacy_legalese_close))
            }
        },
    )
}
