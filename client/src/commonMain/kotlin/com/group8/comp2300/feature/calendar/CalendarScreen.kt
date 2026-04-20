package com.group8.comp2300.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.core.ui.components.ConsumeSnackbarMessage
import com.group8.comp2300.core.ui.components.EmptyStateMessage
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.calendar.components.*
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onNavigateToMedication: () -> Unit = {},
    onManageAppointment: (Appointment) -> Unit = {},
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val timeZone = remember { TimeZone.currentSystemDefault() }

    var sheetUiState by remember { mutableStateOf<CalendarSheetState>(CalendarSheetState.Hidden) }
    var selectedDateForEntry by remember { mutableStateOf<CalendarDay?>(null) }
    var viewMode by rememberSaveable { mutableStateOf(CalendarViewMode.CALENDAR) }
    var entryDate by remember { mutableStateOf(today) }
    var entryTime by remember { mutableStateOf(9 to 0) }

    LaunchedEffect(Unit) {
        selectedDateForEntry =
            CalendarDay(today.day, today, AdherenceStatus.NONE, isToday = true, isCurrentMonth = true)
        viewModel.loadAgendaForDate(today.toString())
    }

    ConsumeSnackbarMessage(
        message = state.error,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::dismissError,
    )

    val selectedDate = selectedDateForEntry?.date ?: today
    val activeMedications = state.medications.filter { it.status == MedicationStatus.ACTIVE }
    val selectedAppointments = remember(state.appointments, selectedDate) {
        state.appointments.filter { appointment ->
            Instant.fromEpochMilliseconds(appointment.appointmentTime)
                .toLocalDateTime(timeZone)
                .date == selectedDate
        }
    }
    val agendaAppointmentsByDate = remember(state.appointments, state.agendaDays) {
        state.agendaDays.associate { day ->
            day.date to state.appointments.filter { appointment ->
                Instant.fromEpochMilliseconds(appointment.appointmentTime)
                    .toLocalDateTime(timeZone)
                    .date == day.date
            }
        }
    }
    val agendaVisibleDays = remember(state.agendaDays, agendaAppointmentsByDate) {
        state.agendaDays.filter { day -> hasAgendaContent(day, agendaAppointmentsByDate[day.date].orEmpty()) }
    }
    val agendaEndDate = remember(selectedDate) { selectedDate.plus(6, DateTimeUnit.DAY) }
    var expandedRoutineKeys by remember { mutableStateOf(emptySet<String>()) }

    val onToggleRoutineExpansion: (String) -> Unit = { routineKey ->
        expandedRoutineKeys = expandedRoutineKeys.toMutableSet().apply {
            if (!add(routineKey)) remove(routineKey)
        }
    }
    val onLogMedication: (RoutineDayAgenda, String, MedicationLogStatus) -> Unit = { routine, medicationId, status ->
        viewModel.logMedication(
            routineLogRequest(
                routine = routine,
                medicationId = medicationId,
                status = status,
            ),
        )
    }
    val onLogAll: (RoutineDayAgenda, MedicationLogStatus) -> Unit = { routine, status ->
        val loggedAt = Clock.System.now().toEpochMilliseconds()
        routine.medications.forEach { medication ->
            viewModel.logMedication(
                routineLogRequest(
                    routine = routine,
                    medicationId = medication.medicationId,
                    status = status,
                    nowMs = loggedAt,
                ),
            )
        }
    }
    val onMoveDose: (RoutineDayAgenda) -> Unit = { routine ->
        val occurrence = Instant.fromEpochMilliseconds(routine.occurrenceTimeMs)
            .toLocalDateTime(timeZone)
        entryDate = occurrence.date
        entryTime = occurrence.hour to occurrence.minute
        sheetUiState = CalendarSheetState.FormReschedule(routine)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(title = { Text(stringResource(Res.string.calendar_title), fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedDateForEntry =
                    CalendarDay(
                        selectedDate.day,
                        selectedDate,
                        AdherenceStatus.NONE,
                        isToday = selectedDate == today,
                        isCurrentMonth = true,
                    )
                entryDate = selectedDate
                val now = Clock.System.now().toLocalDateTime(timeZone)
                entryTime = now.hour to now.minute
                sheetUiState = CalendarSheetState.Menu
            }) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.calendar_add_entry_desc),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                CalendarViewModeSwitch(
                    viewMode = viewMode,
                    onSelect = { viewMode = it },
                )
            }

            if (viewMode == CalendarViewMode.CALENDAR) {
                item {
                    CalendarCard(
                        baseDate = today,
                        overview = state.overview,
                        selectedDate = selectedDateForEntry,
                        onDayClick = { day ->
                            selectedDateForEntry = day
                            entryDate = day.date
                            viewModel.loadAgendaForDate(day.date.toString())
                        },
                        onMonthChange = viewModel::loadOverviewForMonth,
                    )
                }

                item {
                    CalendarDayHeader(
                        date = selectedDate,
                        agenda = state.routineAgenda,
                        extraLogCount = state.manualLogs.size,
                        appointmentCount = selectedAppointments.size,
                        moodCount = state.dayMoodEntries.size,
                    )
                }

                if (state.dayMoodEntries.isNotEmpty()) {
                    item {
                        DailyMoodSummaryCard(moods = state.dayMoodEntries)
                    }
                }

                if (state.monthMoodSummary.isNotEmpty()) {
                    item {
                        MonthlyMoodChart(moodCounts = state.monthMoodSummary)
                    }
                }

                item {
                    Text(
                        stringResource(Res.string.calendar_scheduled_doses),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (state.routineAgenda.isEmpty()) {
                    item {
                        EmptyStateMessage(stringResource(Res.string.calendar_no_scheduled_doses_day))
                    }
                } else {
                    items(
                        items = state.routineAgenda,
                        key = { "${it.routineId}:${it.occurrenceTimeMs}" },
                    ) { routine ->
                        val routineKey = "${routine.routineId}:${routine.occurrenceTimeMs}"
                        RoutineAgendaCard(
                            routine = routine,
                            isExpanded = routineKey in expandedRoutineKeys,
                            onToggleExpansion = { onToggleRoutineExpansion(routineKey) },
                            onLogMedication = { medicationId, status ->
                                onLogMedication(routine, medicationId, status)
                            },
                            onLogAll = { status -> onLogAll(routine, status) },
                            onMoveDose = { onMoveDose(routine) },
                        )
                    }
                }

                if (state.manualLogs.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(Res.string.calendar_extra_logs),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(
                        items = state.manualLogs,
                        key = { it.id.ifBlank { "manual:${it.medicationTime}" } },
                    ) { log ->
                        ManualLogCard(log = log)
                    }
                }

                item {
                    if (selectedAppointments.isNotEmpty()) {
                        Text(
                            stringResource(Res.string.calendar_appointments),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(
                    items = selectedAppointments,
                    key = { it.id.ifBlank { "appt:${it.appointmentTime}" } },
                ) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        onClick = { sheetUiState = CalendarSheetState.AppointmentDetails(appointment) },
                    )
                }
            } else {
                item {
                    AgendaRangeHeader(
                        startDate = selectedDate,
                        endDate = agendaEndDate,
                        onPrevious = {
                            val previousStart = selectedDate.minus(7, DateTimeUnit.DAY)
                            selectedDateForEntry = CalendarDay(
                                day = previousStart.day,
                                date = previousStart,
                                status = AdherenceStatus.NONE,
                                isToday = previousStart == today,
                                isCurrentMonth = true,
                            )
                            entryDate = previousStart
                            viewModel.loadAgendaForDate(previousStart.toString())
                        },
                        onToday = {
                            selectedDateForEntry = CalendarDay(
                                day = today.day,
                                date = today,
                                status = AdherenceStatus.NONE,
                                isToday = true,
                                isCurrentMonth = true,
                            )
                            entryDate = today
                            viewModel.loadAgendaForDate(today.toString())
                        },
                        onNext = {
                            val nextStart = selectedDate.plus(7, DateTimeUnit.DAY)
                            selectedDateForEntry = CalendarDay(
                                day = nextStart.day,
                                date = nextStart,
                                status = AdherenceStatus.NONE,
                                isToday = nextStart == today,
                                isCurrentMonth = true,
                            )
                            entryDate = nextStart
                            viewModel.loadAgendaForDate(nextStart.toString())
                        },
                    )
                }

                if (agendaVisibleDays.isEmpty()) {
                    item {
                        AgendaEmptyState()
                    }
                } else {
                    items(
                        items = agendaVisibleDays,
                        key = { it.date.toString() },
                    ) { day ->
                        AgendaDaySection(
                            day = day,
                            appointments = agendaAppointmentsByDate[day.date].orEmpty(),
                            expandedRoutineKeys = expandedRoutineKeys,
                            onToggleRoutineExpansion = onToggleRoutineExpansion,
                            onLogMedication = onLogMedication,
                            onLogAll = onLogAll,
                            onMoveDose = onMoveDose,
                            onAppointmentClick = { appointment ->
                                sheetUiState = CalendarSheetState.AppointmentDetails(appointment)
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (sheetUiState != CalendarSheetState.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { sheetUiState = CalendarSheetState.Hidden },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (val activeSheet = sheetUiState) {
                CalendarSheetState.Hidden -> Unit

                CalendarSheetState.Menu -> AddEntryMenu(
                    selectedDate = selectedDate,
                    onSelectType = { sheetUiState = it },
                )

                CalendarSheetState.FormMedication -> WrapperFormLayout(
                    title = stringResource(Res.string.form_medication_title),
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = { sheetUiState = CalendarSheetState.Menu },
                ) {
                    ManualMedicationLogForm(
                        medications = activeMedications,
                        onSave = { medicationId, status ->
                            val timestampMs = entryDateTimeToEpochMs(entryDate, entryTime, timeZone)
                            val baseRequest = MedicationLogRequest(
                                medicationId = medicationId,
                                status = status.name,
                                timestampMs = timestampMs,
                            )
                            viewModel.getMedicationOccurrenceCandidates(
                                medicationId = medicationId,
                                timestampMs = timestampMs,
                            ) { candidates ->
                                if (candidates.isEmpty()) {
                                    viewModel.logMedication(
                                        baseRequest.copy(linkMode = MedicationLogLinkMode.EXTRA_DOSE),
                                    )
                                    sheetUiState = CalendarSheetState.Hidden
                                } else {
                                    sheetUiState = CalendarSheetState.ResolveMedication(
                                        pendingLog = baseRequest,
                                        candidates = candidates,
                                    )
                                }
                            }
                        },
                        onOpenMedicationCabinet = {
                            sheetUiState = CalendarSheetState.Hidden
                            onNavigateToMedication()
                        },
                    )
                }

                is CalendarSheetState.FormReschedule -> WrapperFormLayout(
                    title = stringResource(Res.string.calendar_move_scheduled_dose_title),
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = { sheetUiState = CalendarSheetState.Menu },
                ) {
                    val originalOccurrence = Instant.fromEpochMilliseconds(activeSheet.routine.originalOccurrenceTimeMs)
                        .toLocalDateTime(timeZone)
                    Text(
                        stringResource(Res.string.calendar_move_scheduled_dose_desc),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (activeSheet.routine.isRescheduled) {
                        Text(
                            stringResource(
                                Res.string.calendar_move_scheduled_dose_originally_due,
                                DateFormatter.formatDayMonthYear(originalOccurrence.date),
                                DateFormatter.formatTime(originalOccurrence.hour, originalOccurrence.minute),
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Button(
                        onClick = {
                            val targetDateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
                            viewModel.rescheduleRoutineOccurrence(
                                RoutineOccurrenceOverrideRequest(
                                    routineId = activeSheet.routine.routineId,
                                    originalOccurrenceTimeMs = activeSheet.routine.originalOccurrenceTimeMs,
                                    rescheduledOccurrenceTimeMs =
                                    targetDateTime.toInstant(timeZone).toEpochMilliseconds(),
                                ),
                            )
                            sheetUiState = CalendarSheetState.Hidden
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.calendar_save_moved_dose))
                    }
                }

                is CalendarSheetState.ResolveMedication -> ResolveMedicationLogSheet(
                    medicationName = state.medications.firstOrNull {
                        it.id == activeSheet.pendingLog.medicationId
                    }?.name.orEmpty(),
                    candidates = activeSheet.candidates,
                    onBack = { sheetUiState = CalendarSheetState.FormMedication },
                    onAttach = { candidate ->
                        viewModel.logMedication(
                            activeSheet.pendingLog.copy(
                                routineId = candidate.routineId,
                                occurrenceTimeMs = candidate.occurrenceTimeMs,
                                linkMode = MedicationLogLinkMode.ATTACH_TO_OCCURRENCE,
                            ),
                        )
                        sheetUiState = CalendarSheetState.Hidden
                    },
                    onLogExtraDose = {
                        viewModel.logMedication(
                            activeSheet.pendingLog.copy(linkMode = MedicationLogLinkMode.EXTRA_DOSE),
                        )
                        sheetUiState = CalendarSheetState.Hidden
                    },
                )

                CalendarSheetState.FormMood -> WrapperFormLayout(
                    title = stringResource(Res.string.form_mood_title),
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = { sheetUiState = CalendarSheetState.Menu },
                ) {
                    MoodEntryForm { score, notes ->
                        val timestampMs = entryDateTimeToEpochMs(entryDate, entryTime, timeZone)
                        viewModel.logMood(score, emptyList(), emptyList(), notes, timestampMs)
                        sheetUiState = CalendarSheetState.Hidden
                    }
                }

                is CalendarSheetState.AppointmentDetails ->
                    AppointmentDetailSheetContent(
                        appointment = activeSheet.appointment,
                        onManageInCare = {
                            sheetUiState = CalendarSheetState.Hidden
                            onManageAppointment(activeSheet.appointment)
                        },
                        onClose = { sheetUiState = CalendarSheetState.Hidden },
                    )
            }
        }
    }
}
