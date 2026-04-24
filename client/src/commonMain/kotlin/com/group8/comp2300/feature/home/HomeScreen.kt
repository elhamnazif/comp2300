package com.group8.comp2300.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.home_retry
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    userFirstName: String?,
    onRetry: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToBookingHistory: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSymptomChecker: () -> Unit = {},
) {
    val liveGreetingPeriod = rememberGreetingPeriod(initialPeriod = state.greetingPeriod)

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
                            greetingPeriod = liveGreetingPeriod,
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
                            onOpenCalendar = onNavigateToCalendar,
                            onOpenAppointment = state.todaySummary.nextAppointment?.let { appointment ->
                                { onNavigateToBookingHistory(appointment.appointmentId) }
                            },
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

@Composable
private fun rememberGreetingPeriod(initialPeriod: GreetingPeriod): GreetingPeriod {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val greetingPeriod by produceState(initialValue = initialPeriod, key1 = timeZone) {
        while (true) {
            val now = Clock.System.now().toEpochMilliseconds()
            value = buildGreetingPeriod(nowMs = now, timeZone = timeZone)
            delay(millisUntilNextGreetingPeriod(nowMs = now, timeZone = timeZone).milliseconds)
        }
    }
    return greetingPeriod
}

private fun millisUntilNextGreetingPeriod(nowMs: Long, timeZone: TimeZone): Long {
    val now = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone)
    val nextTransition = when {
        now.hour < 12 -> LocalDateTime(now.date, LocalTime(hour = 12, minute = 0))

        now.hour < 17 -> LocalDateTime(now.date, LocalTime(hour = 17, minute = 0))

        else -> LocalDateTime(
            date = now.date.plus(1, DateTimeUnit.DAY),
            time = LocalTime(hour = 0, minute = 0),
        )
    }

    return (nextTransition.toInstant(timeZone).toEpochMilliseconds() - nowMs)
        .coerceAtLeast(60_000L)
}
