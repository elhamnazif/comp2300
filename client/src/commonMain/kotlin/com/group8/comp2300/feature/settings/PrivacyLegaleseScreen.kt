package com.group8.comp2300.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.group8.comp2300.app.navigation.PrivacyLegalDocument
import com.group8.comp2300.core.ui.settings.SettingsInfoCard
import com.group8.comp2300.core.ui.settings.SettingsNavigationRow
import com.group8.comp2300.core.ui.settings.SettingsSection
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DescriptionW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.MailOutlineW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ShieldW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val LegalSupportEmail = "vita@elham.dev"

@Composable
fun PrivacyLegaleseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialDocument: PrivacyLegalDocument? = null,
) {
    var activeDocumentName by rememberSaveable(initialDocument) { mutableStateOf(initialDocument?.name) }
    val activeDocument = remember(activeDocumentName) {
        activeDocumentName?.let { savedName -> PrivacyLegalDocument.entries.firstOrNull { it.name == savedName } }
    }
    val uriHandler = LocalUriHandler.current

    LegalDocumentScaffold(
        title = stringResource(Res.string.profile_privacy_legalese_title),
        onBack = onBack,
        modifier = modifier,
        listState = rememberLazySettingsListState(),
    ) {
        item {
            SettingsSection(
                title = stringResource(Res.string.privacy_legalese_overview_title),
                description = stringResource(Res.string.privacy_legalese_overview_body),
            ) {
                PrivacyLegalDocument.entries.forEachIndexed { index, document ->
                    SettingsNavigationRow(
                        icon = document.icon(),
                        title = stringResource(document.titleRes()),
                        description = stringResource(document.descriptionRes()),
                        index = index,
                        total = PrivacyLegalDocument.entries.size,
                        onClick = { activeDocumentName = document.name },
                    )
                }
            }
        }
        item {
            SettingsSection {
                SettingsNavigationRow(
                    icon = Icons.MailOutlineW400Outlinedfill1,
                    title = stringResource(Res.string.privacy_legalese_email_us),
                    description = LegalSupportEmail,
                    index = 0,
                    total = 1,
                    onClick = {
                        uriHandler.openUri("mailto:$LegalSupportEmail")
                    },
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

    if (activeDocument != null) {
        LegalDocumentViewerDialog(
            document = activeDocument,
            onDismiss = { activeDocumentName = null },
        )
    }
}

@Composable
fun LegalDocumentViewerDialog(document: PrivacyLegalDocument, onDismiss: () -> Unit) {
    val title = stringResource(document.titleRes())
    val content = stringResource(document.contentRes())
    val sections = remember(document, title, content) {
        content
            .removePrefix("$title\n\n")
            .split("\n\n")
            .map(String::trim)
            .filter(String::isNotEmpty)
    }
    val listState = rememberSaveable(document.name, saver = LazyListState.Saver) {
        LazyListState()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            LegalDocumentScaffold(
                title = title,
                onBack = onDismiss,
                listState = listState,
            ) {
                item {
                    Text(
                        text = stringResource(Res.string.privacy_legalese_overview_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                sections.forEach { section ->
                    item {
                        SelectionContainer {
                            SettingsInfoCard(
                                description = section,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberLazySettingsListState(): LazyListState = rememberSaveable(saver = LazyListState.Saver) {
    LazyListState()
}

@Composable
private fun LegalDocumentScaffold(
    title: String,
    onBack: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = containerColor,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.ArrowBackW400Outlinedfill1,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private fun PrivacyLegalDocument.titleRes() = when (this) {
    PrivacyLegalDocument.TermsOfService -> Res.string.privacy_legalese_terms_title
    PrivacyLegalDocument.PrivacyPolicy -> Res.string.privacy_legalese_privacy_title
}

private fun PrivacyLegalDocument.descriptionRes() = when (this) {
    PrivacyLegalDocument.TermsOfService -> Res.string.privacy_legalese_terms_desc
    PrivacyLegalDocument.PrivacyPolicy -> Res.string.privacy_legalese_privacy_desc
}

private fun PrivacyLegalDocument.contentRes() = when (this) {
    PrivacyLegalDocument.TermsOfService -> Res.string.privacy_legalese_terms_content
    PrivacyLegalDocument.PrivacyPolicy -> Res.string.privacy_legalese_privacy_content
}

private fun PrivacyLegalDocument.icon() = when (this) {
    PrivacyLegalDocument.TermsOfService -> Icons.DescriptionW400Outlinedfill1
    PrivacyLegalDocument.PrivacyPolicy -> Icons.ShieldW400Outlinedfill1
}
