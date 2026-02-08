@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.mock.faqs
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.help_support_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.ArrowBackW400Outlinedfill1, stringResource(Res.string.auth_back_desc))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            Text(
                stringResource(Res.string.help_support_faq_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // faqs is imported from shared mock data

            faqs.forEachIndexed { index, (question, answer) ->
                FaqItem(
                    question = question,
                    answer = answer,
                    expanded = expandedFaqIndex == index,
                    onClick = {
                        expandedFaqIndex =
                            if (expandedFaqIndex == index) {
                                null
                            } else {
                                index
                            }
                    }
                )
                if (index < faqs.size - 1) {
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(Res.string.help_support_contact_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SupportOptionCard(
                icon = Icons.MailOutlineW400Outlinedfill1,
                title = stringResource(Res.string.help_support_email_label),
                description = stringResource(Res.string.help_support_email_val),
                onClick = {}
            )

            Spacer(Modifier.height(8.dp))

            SupportOptionCard(
                icon = Icons.CallW400Outlinedfill1,
                title = stringResource(Res.string.help_support_phone_label),
                description = stringResource(Res.string.help_support_phone_val),
                onClick = {}
            )

            Spacer(Modifier.height(8.dp))

            SupportOptionCard(
                icon = Icons.InfoW400Outlinedfill1,
                title = stringResource(Res.string.help_support_resources_label),
                description = stringResource(Res.string.help_support_resources_desc),
                onClick = {}
            )

            Spacer(Modifier.height(24.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(Res.string.help_support_emergency_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(Res.string.help_support_emergency_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.clickable(onClick = onClick).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.ExpandLessW400Outlined
                        } else {
                            Icons.ExpandMoreW400Outlined
                        },
                    contentDescription =
                        if (expanded) {
                            stringResource(Res.string.help_support_collapse_desc)
                        } else {
                            stringResource(Res.string.help_support_expand_desc)
                        },
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun SupportOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
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
