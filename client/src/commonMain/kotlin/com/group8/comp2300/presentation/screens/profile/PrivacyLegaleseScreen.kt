package com.group8.comp2300.presentation.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacyLegaleseScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var showTerms by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.profile_privacy_legalese_title)) },
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
            Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(
                stringResource(Res.string.privacy_legalese_overview_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                stringResource(Res.string.privacy_legalese_overview_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            LegalSectionCard(
                icon = Icons.DescriptionW400Outlinedfill1,
                title = stringResource(Res.string.privacy_legalese_terms_title),
                description = stringResource(Res.string.privacy_legalese_terms_desc),
                onClick = { showTerms = true },
            )

            Spacer(Modifier.height(12.dp))

            LegalSectionCard(
                icon = Icons.ShieldW400Outlinedfill1,
                title = stringResource(Res.string.privacy_legalese_privacy_title),
                description = stringResource(Res.string.privacy_legalese_privacy_desc),
                onClick = { showPrivacyPolicy = true },
            )

            Spacer(Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.privacy_legalese_contact_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.privacy_legalese_contact_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
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
private fun LegalSectionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier =
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
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

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Icon(
                Icons.ChevronRightW400Outlined,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
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
