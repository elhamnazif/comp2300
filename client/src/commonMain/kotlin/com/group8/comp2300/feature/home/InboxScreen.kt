package com.group8.comp2300.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.AppTopBar
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.home_inbox_empty_body
import comp2300.i18n.generated.resources.home_inbox_empty_title
import comp2300.i18n.generated.resources.home_inbox_section_attention
import comp2300.i18n.generated.resources.home_inbox_section_today
import comp2300.i18n.generated.resources.home_inbox_title
import comp2300.i18n.generated.resources.home_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InboxScreen(
    state: HomeUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (HomeInboxAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.home_inbox_title), fontWeight = FontWeight.Bold) },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                HomeCenteredMessage(
                    title = "Loading inbox",
                    showLoading = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            state.errorMessage != null && state.inboxItems.isEmpty() -> {
                HomeCenteredMessage(
                    title = state.errorMessage,
                    body = stringResource(Res.string.home_inbox_empty_body),
                    buttonLabel = stringResource(Res.string.home_retry),
                    onAction = onRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            state.inboxItems.isEmpty() -> {
                HomeCenteredMessage(
                    title = stringResource(Res.string.home_inbox_empty_title),
                    body = stringResource(Res.string.home_inbox_empty_body),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    state.errorMessage?.let { message ->
                        item {
                            HomeInlineMessage(
                                title = message,
                                actionLabel = stringResource(Res.string.home_retry),
                                onAction = onRetry,
                            )
                        }
                    }

                    val grouped = state.inboxItems.groupBy(HomeInboxItem::group)
                    grouped[HomeInboxGroup.ATTENTION]?.let { items ->
                        item {
                            InboxSection(
                                title = stringResource(Res.string.home_inbox_section_attention),
                                items = items,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                    grouped[HomeInboxGroup.TODAY]?.let { items ->
                        item {
                            InboxSection(
                                title = stringResource(Res.string.home_inbox_section_today),
                                items = items,
                                onItemClick = onItemClick,
                            )
                        }
                    }
                }
            }
        }
    }
}
