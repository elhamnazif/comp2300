package com.group8.comp2300.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.home_hero_supporting
import comp2300.i18n.generated.resources.home_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    userFirstName: String?,
    onRetry: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSymptomChecker: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        when {
            state.isLoading -> {
                HomeCenteredMessage(
                    title = "Loading home",
                    showLoading = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            state.errorMessage != null && state.todaySummary.totalMedicationCount == 0 &&
                state.todaySummary.nextAppointment == null && state.inboxItems.isEmpty() &&
                state.activeMedicationCount == 0 -> {
                HomeCenteredMessage(
                    title = state.errorMessage,
                    body = stringResource(Res.string.home_hero_supporting),
                    buttonLabel = stringResource(Res.string.home_retry),
                    onAction = onRetry,
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
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        HomeHero(
                            greetingPeriod = state.greetingPeriod,
                            userFirstName = userFirstName,
                            showInboxBadge = state.inboxItems.isNotEmpty(),
                            onOpenInbox = onNavigateToInbox,
                        )
                    }

                    state.errorMessage?.let { message ->
                        item {
                            HomeInlineMessage(
                                title = message,
                                actionLabel = stringResource(Res.string.home_retry),
                                onAction = onRetry,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }

                    item {
                        TodaySummaryCard(
                            summary = state.todaySummary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    item {
                        AskVitaRow(
                            onClick = onNavigateToChatbot,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    item {
                        ActionList(
                            activeMedicationCount = state.activeMedicationCount,
                            onNavigateToSymptomChecker = onNavigateToSymptomChecker,
                            onNavigateToMedication = onNavigateToMedication,
                            onNavigateToRoutines = onNavigateToRoutines,
                            onNavigateToShop = onNavigateToShop,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
