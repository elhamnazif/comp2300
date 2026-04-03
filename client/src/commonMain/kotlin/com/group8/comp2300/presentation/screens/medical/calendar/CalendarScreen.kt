@file:Suppress("ktlint:standard:max-line-length")

package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.presentation.screens.medical.components.DatePickerSheet
import com.group8.comp2300.presentation.screens.medical.components.DateValueField
import com.group8.comp2300.presentation.screens.medical.components.EmptyStateMessage
import com.group8.comp2300.presentation.screens.medical.components.TimePickerSheet
import com.group8.comp2300.presentation.screens.medical.components.TimeValueField
import com.group8.comp2300.presentation.util.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import comp2300.i18n.generated.resources.Res
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

private sealed interface CalendarSheetState {
    data object Hidden : CalendarSheetState

    data object Menu : CalendarSheetState

    data object FormMedication : CalendarSheetState

    data class FormReschedule(val routine: RoutineDayAgenda) : CalendarSheetState

    data class ResolveMedication(
        val pendingLog: MedicationLogRequest,
        val candidates: List<MedicationOccurrenceCandidate>,
    ) : CalendarSheetState

    data object FormAppointment : CalendarSheetState

    data object FormMood : CalendarSheetState

    data class AppointmentDetails(val appointment: Appointment) : CalendarSheetState
}

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    onNavigateToMedication: () -> Unit = {},
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val timeZone = remember { TimeZone.currentSystemDefault() }

    var sheetUiState by remember { mutableStateOf<CalendarSheetState>(CalendarSheetState.Hidden) }
    var selectedDateForEntry by remember { mutableStateOf<CalendarDay?>(null) }
    var entryDate by remember { mutableStateOf(today) }
    var entryTime by remember { mutableStateOf(9 to 0) }

    LaunchedEffect(Unit) {
        selectedDateForEntry =
            CalendarDay(today.day, today, AdherenceStatus.NONE, isToday = true, isCurrentMonth = true)
        viewModel.loadAgendaForDate(today.toString())
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val selectedDate = selectedDateForEntry?.date ?: today
    val activeMedications = state.medications.filter { it.status == MedicationStatus.ACTIVE }
    val selectedAppointments = remember(state.appointments, selectedDate) {
        state.appointments.filter { appointment ->
            Instant.fromEpochMilliseconds(appointment.appointmentTime)
                .toLocalDateTime(timeZone)
                .date == selectedDate
        }
    }
    var expandedRoutineKeys by remember(
        selectedDate,
        state.routineAgenda.map { "${it.routineId}:${it.occurrenceTimeMs}" },
    ) { mutableStateOf(emptySet<String>()) }

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
                        onToggleExpansion = {
                            expandedRoutineKeys = expandedRoutineKeys.toMutableSet().apply {
                                if (!add(routineKey)) remove(routineKey)
                            }
                        },
                        onLogMedication = { medicationId, status ->
                            viewModel.logMedication(
                                routineLogRequest(
                                    routine = routine,
                                    medicationId = medicationId,
                                    status = status,
                                ),
                            )
                        },
                        onLogAll = { status ->
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
                        },
                        onMoveDose = {
                            val occurrence = Instant.fromEpochMilliseconds(routine.occurrenceTimeMs)
                                .toLocalDateTime(timeZone)
                            entryDate = occurrence.date
                            entryTime = occurrence.hour to occurrence.minute
                            sheetUiState = CalendarSheetState.FormReschedule(routine)
                        },
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

            if (selectedAppointments.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.calendar_appointments),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

                CalendarSheetState.FormAppointment -> WrapperFormLayout(
                    title = stringResource(Res.string.calendar_form_add_appointment_title),
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = { sheetUiState = CalendarSheetState.Menu },
                ) {
                    AppointmentForm { doctorName, appointmentType ->
                        val dateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
                        viewModel.scheduleAppointment(
                            doctorName,
                            appointmentType,
                            dateTime.toInstant(timeZone).toEpochMilliseconds(),
                        )
                        sheetUiState = CalendarSheetState.Hidden
                    }
                }

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
                        activeSheet.appointment,
                        onClose = { sheetUiState = CalendarSheetState.Hidden },
                    )
            }
        }
    }
}

@Composable
private fun CalendarDayHeader(
    date: LocalDate,
    agenda: List<RoutineDayAgenda>,
    extraLogCount: Int,
    appointmentCount: Int,
    moodCount: Int = 0,
) {
    val allDoses = agenda.flatMap(RoutineDayAgenda::medications)
    val taken = allDoses.count { it.status == MedicationLogStatus.TAKEN }
    val unresolved = allDoses.count {
        it.status == MedicationLogStatus.PENDING ||
            it.status == MedicationLogStatus.MISSED ||
            it.status == MedicationLogStatus.SNOOZED
    }
    val summary =
        if (allDoses.isEmpty()) {
            stringResource(Res.string.calendar_no_scheduled_doses)
        } else {
            stringResource(Res.string.calendar_taken_summary, taken, allDoses.size)
        }
    val metaItems = mutableListOf<String>()
    if (allDoses.isNotEmpty() && unresolved > 0) {
        metaItems +=
            if (unresolved == 1) {
                stringResource(Res.string.calendar_unresolved_one)
            } else {
                stringResource(Res.string.calendar_unresolved_many, unresolved)
            }
    }
    if (extraLogCount > 0) {
        metaItems +=
            if (extraLogCount == 1) {
                stringResource(Res.string.calendar_extra_log_one)
            } else {
                stringResource(Res.string.calendar_extra_log_many, extraLogCount)
            }
    }
    if (appointmentCount > 0) {
        metaItems +=
            if (appointmentCount == 1) {
                stringResource(Res.string.calendar_appointment_one)
            } else {
                stringResource(Res.string.calendar_appointment_many, appointmentCount)
            }
    }
    if (moodCount > 0) {
        metaItems +=
            if (moodCount == 1) {
                stringResource(Res.string.calendar_mood_one)
            } else {
                stringResource(Res.string.calendar_mood_many, moodCount)
            }
    }
    val meta = metaItems.joinToString(" • ")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            DateFormatter.formatDayMonthYear(date),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            summary,
            style = MaterialTheme.typography.titleMedium,
            color = if (allDoses.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoutineAgendaCard(
    routine: RoutineDayAgenda,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onLogMedication: (String, MedicationLogStatus) -> Unit,
    onLogAll: (MedicationLogStatus) -> Unit,
    onMoveDose: () -> Unit,
) {
    val occurrence = Instant.fromEpochMilliseconds(
        routine.occurrenceTimeMs,
    ).toLocalDateTime(TimeZone.currentSystemDefault())
    val actionableMeds = remember(routine.medications) {
        routine.medications.filter {
            it.status == MedicationLogStatus.PENDING ||
                it.status == MedicationLogStatus.MISSED ||
                it.status == MedicationLogStatus.SNOOZED
        }
    }
    val routineCompleted = actionableMeds.isEmpty()
    val showReviewToggle = routine.medications.size > 1 && actionableMeds.isNotEmpty()
    val reminderMeta = reminderMetaLabel(routine.reminderOffsetsMins)
    val metaLines = mutableListOf<String>()
    if (routine.isRescheduled) {
        val originalOccurrence = Instant.fromEpochMilliseconds(routine.originalOccurrenceTimeMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        metaLines += stringResource(
            Res.string.calendar_moved_from,
            DateFormatter.formatDayMonthYear(originalOccurrence.date),
            DateFormatter.formatTime(originalOccurrence.hour, originalOccurrence.minute),
        )
    }
    if (routineCompleted) {
        metaLines += routineCompletionSummary(routine.medications)
    }

    Card(
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        DateFormatter.formatTime(occurrence.hour, occurrence.minute),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        routine.routineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (routine.hasReminder && routine.reminderOffsetsMins.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.NotificationsW400Outlinedfill1,
                            contentDescription = stringResource(Res.string.calendar_reminders_enabled_desc),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        reminderMeta?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Text(
                routineMedicationSummary(routine.medications),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            metaLines.forEach { metaLine ->
                Text(
                    metaLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!routineCompleted) {
                if (routine.medications.size == 1) {
                    val medication = routine.medications.single()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.TAKEN) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(Res.string.calendar_taken_action)) }
                        OutlinedButton(
                            onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.SKIPPED) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(Res.string.calendar_skip_action)) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onLogAll(MedicationLogStatus.TAKEN) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.calendar_take_all_action))
                        }
                        OutlinedButton(onClick = {
                            onLogAll(MedicationLogStatus.SKIPPED)
                        }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.calendar_skip_all_action))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showReviewToggle) {
                            TextButton(onClick = onToggleExpansion) {
                                Text(
                                    if (isExpanded) {
                                        stringResource(Res.string.calendar_hide_medications)
                                    } else {
                                        stringResource(Res.string.calendar_review_individually)
                                    },
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        TextButton(onClick = onMoveDose) {
                            Text(
                                if (routine.isRescheduled) {
                                    stringResource(Res.string.calendar_move_again_action)
                                } else {
                                    stringResource(Res.string.calendar_move_action)
                                },
                            )
                        }
                    }
                }
                if (routine.medications.size == 1) {
                    TextButton(onClick = onMoveDose, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (routine.isRescheduled) {
                                stringResource(Res.string.calendar_move_again_action)
                            } else {
                                stringResource(Res.string.calendar_move_dose_action)
                            },
                        )
                    }
                }
            }

            if (routine.medications.size > 1 && isExpanded) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    routine.medications.forEach { medication ->
                        RoutineMedicationRow(medication = medication, onLogMedication = onLogMedication)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineMedicationRow(
    medication: RoutineMedicationAgenda,
    onLogMedication: (String, MedicationLogStatus) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(medication.medicationName, fontWeight = FontWeight.SemiBold)
                Text(
                    medication.dosage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                medication.status.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = medicationStatusColor(medication.status),
            )
        }
        if (
            medication.status == MedicationLogStatus.PENDING ||
            medication.status == MedicationLogStatus.MISSED ||
            medication.status == MedicationLogStatus.SNOOZED
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onLogMedication(medication.medicationId, MedicationLogStatus.TAKEN)
                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.calendar_taken_action))
                }
                OutlinedButton(onClick = {
                    onLogMedication(medication.medicationId, MedicationLogStatus.SKIPPED)
                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.calendar_skip_action))
                }
            }
        }
    }
}

@Composable
private fun medicationStatusColor(status: MedicationLogStatus): Color = when (status) {
    MedicationLogStatus.TAKEN -> Color(0xFF2E7D32)
    MedicationLogStatus.SKIPPED, MedicationLogStatus.MISSED -> MaterialTheme.colorScheme.error
    MedicationLogStatus.SNOOZED -> MaterialTheme.colorScheme.primary
    MedicationLogStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun routineMedicationSummary(medications: List<RoutineMedicationAgenda>): String = when (medications.size) {
    0 -> ""
    1 -> "${medications.single().medicationName} • ${medications.single().dosage}"
    2 -> medications.joinToString(" • ") { it.medicationName }
    else -> "${medications[0].medicationName}, ${medications[1].medicationName} +${medications.size - 2}"
}

@Composable
private fun routineCompletionSummary(medications: List<RoutineMedicationAgenda>): String {
    val taken = medications.count { it.status == MedicationLogStatus.TAKEN }
    val skipped = medications.count {
        it.status == MedicationLogStatus.SKIPPED ||
            it.status == MedicationLogStatus.MISSED
    }
    return when {
        taken == medications.size -> stringResource(Res.string.calendar_all_medications_taken)
        skipped == medications.size -> stringResource(Res.string.calendar_marked_skipped)
        taken > 0 && skipped > 0 -> stringResource(Res.string.calendar_taken_skipped_summary, taken, skipped)
        else -> stringResource(Res.string.calendar_completed)
    }
}

private fun reminderMetaLabel(offsets: List<Int>): String? {
    val unique = offsets.sorted().distinct()
    return when {
        unique.isEmpty() -> null
        unique == listOf(0) -> null
        unique.size == 1 -> "${unique.first()}m"
        else -> "${unique.size}x"
    }
}

@Composable
private fun ManualLogCard(log: MedicationLog) {
    val dateTime = Instant.fromEpochMilliseconds(log.medicationTime).toLocalDateTime(TimeZone.currentSystemDefault())
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    log.medicationName ?: stringResource(Res.string.calendar_medication_fallback),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        Res.string.calendar_log_status_at_time,
                        log.status.displayName,
                        DateFormatter.formatTime(dateTime.hour, dateTime.minute),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(Res.string.calendar_extra_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun CalendarCard(
    baseDate: LocalDate,
    overview: List<CalendarOverviewResponse>,
    selectedDate: CalendarDay?,
    onDayClick: (CalendarDay) -> Unit,
    onMonthChange: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val displayDate = remember(baseDate, monthOffset) { baseDate.plus(monthOffset, DateTimeUnit.MONTH) }
    val currentMonthChange by rememberUpdatedState(onMonthChange)
    LaunchedEffect(displayDate) {
        currentMonthChange(displayDate.year, displayDate.month.number)
    }
    val overviewMap = remember(overview) { overview.associate { it.date to it.status } }
    val calendarDays =
        remember(displayDate, overviewMap) { generateCalendarDays(displayDate.year, displayDate.month, overviewMap) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.calendar_prev_month_desc),
                    )
                }
                Text(
                    DateFormatter.formatMonthYear(displayDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = { monthOffset++ }) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.calendar_next_month_desc),
                        modifier = Modifier.rotate(180f),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                    stringResource(Res.string.day_initial_sun),
                    stringResource(Res.string.day_initial_mon),
                    stringResource(Res.string.day_initial_tue),
                    stringResource(Res.string.day_initial_wed),
                    stringResource(Res.string.day_initial_thu),
                    stringResource(Res.string.day_initial_fri),
                    stringResource(Res.string.day_initial_sat),
                ).forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            calendarDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(day = day, isSelected = selectedDate?.date == day.date, onClick = {
                                onDayClick(day)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(day: CalendarDay, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val alpha = if (day.isCurrentMonth) 1f else 0.3f
    val borderColor = when {
        isSelected -> Color.Transparent
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val statusDotColor = when (day.status) {
        AdherenceStatus.TAKEN -> Color(0xFF4CAF50)
        AdherenceStatus.MISSED -> MaterialTheme.colorScheme.error
        AdherenceStatus.APPOINTMENT -> Color(0xFFD4AF37)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.day.toString(),
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            )
            if (day.status != AdherenceStatus.NONE && day.isCurrentMonth) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(statusDotColor),
                )
            }
        }
    }
}

@Composable
private fun ManualMedicationLogForm(
    medications: List<Medication>,
    onSave: (String, MedicationLogStatus) -> Unit,
    onOpenMedicationCabinet: () -> Unit,
) {
    var selectedMedId by remember(medications) { mutableStateOf(medications.firstOrNull()?.id.orEmpty()) }
    var selectedStatus by remember { mutableStateOf(MedicationLogStatus.TAKEN) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (medications.isEmpty()) {
            EmptyStateMessage(stringResource(Res.string.calendar_no_medications_yet))
        } else {
            SimpleDropdown(
                label = stringResource(Res.string.form_medication_label),
                options = medications.map { it.id to "${it.name} • ${it.dosage}" },
                selectedKey = selectedMedId,
                onSelect = { selectedMedId = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MedicationLogStatus.TAKEN, MedicationLogStatus.SKIPPED).forEach { status ->
                    FilterChip(selected = selectedStatus == status, onClick = {
                        selectedStatus = status
                    }, label = { Text(status.displayName) })
                }
            }
            Button(onClick = {
                onSave(selectedMedId, selectedStatus)
            }, enabled = selectedMedId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.calendar_save_log))
            }
        }
        TextButton(onClick = onOpenMedicationCabinet, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_manage_medications))
        }
    }
}

@Composable
private fun ResolveMedicationLogSheet(
    medicationName: String,
    candidates: List<MedicationOccurrenceCandidate>,
    onBack: () -> Unit,
    onAttach: (MedicationOccurrenceCandidate) -> Unit,
    onLogExtraDose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = stringResource(Res.string.common_back_desc))
            }
            Text(
                stringResource(Res.string.calendar_match_scheduled_dose_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(Res.string.calendar_match_scheduled_dose_desc, medicationName),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        candidates.forEach { candidate ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(candidate.routineName, fontWeight = FontWeight.SemiBold)
                    val occurrence = Instant.fromEpochMilliseconds(candidate.occurrenceTimeMs)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        stringResource(
                            Res.string.calendar_occurrence_status,
                            DateFormatter.formatDayMonthYear(occurrence.date),
                            DateFormatter.formatTime(occurrence.hour, occurrence.minute),
                            candidate.status.displayName,
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Button(onClick = { onAttach(candidate) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.calendar_count_toward_schedule))
                    }
                }
            }
        }
        OutlinedButton(onClick = onLogExtraDose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_save_as_extra_log))
        }
    }
}

@Composable
private fun AddEntryMenu(selectedDate: LocalDate, onSelectType: (CalendarSheetState) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.calendar_add_entry_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            DateFormatter.formatDayMonthYear(selectedDate),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = {
            onSelectType(CalendarSheetState.FormMedication)
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.calendar_menu_log_med)) }
        OutlinedButton(onClick = {
            onSelectType(CalendarSheetState.FormAppointment)
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.calendar_menu_track_appt)) }
        OutlinedButton(onClick = {
            onSelectType(CalendarSheetState.FormMood)
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.calendar_menu_track_mood)) }
    }
}

@Composable
private fun WrapperFormLayout(
    title: String,
    entryDate: LocalDate,
    entryTime: Pair<Int, Int>,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    var activeDatePicker by remember { mutableStateOf(false) }
    var activeTimePicker by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = stringResource(Res.string.common_back_desc))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        DateValueField(
            label = stringResource(Res.string.form_date_label),
            value = entryDate,
            onClick = { activeDatePicker = true },
        )
        TimeValueField(
            label = stringResource(Res.string.form_time_label),
            value = ((entryTime.first * 60L) + entryTime.second) * 60_000L,
            onClick = {
                activeTimePicker =
                    true
            },
        )
        content()
    }
    if (activeDatePicker) {
        DatePickerSheet(
            initialDate = entryDate,
            onDismiss = { activeDatePicker = false },
            onConfirm = {
                onDateChange(it)
                activeDatePicker = false
            },
        )
    }
    if (activeTimePicker) {
        TimePickerSheet(
            initialTimeMs = ((entryTime.first * 60L) + entryTime.second) * 60_000L,
            onDismiss = { activeTimePicker = false },
            onConfirm = {
                val totalMinutes = (it / 60_000L).toInt()
                onTimeChange(totalMinutes / 60, totalMinutes % 60)
                activeTimePicker = false
            },
        )
    }
}

@Composable
private fun AppointmentForm(onSave: (String, String) -> Unit) {
    var doctorName by remember { mutableStateOf("") }
    var appointmentType by remember { mutableStateOf("CONSULTATION") }
    val appointmentOptions = listOf(
        "CONSULTATION" to stringResource(Res.string.appt_type_consultation),
        "LAB_TEST" to stringResource(Res.string.appt_type_labwork),
        "FOLLOW_UP" to stringResource(Res.string.appt_type_followup),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = doctorName, onValueChange = {
            doctorName = it
        }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.form_appt_doctor_label)) })
        SimpleDropdown(
            label = stringResource(Res.string.form_appt_type_label),
            options = appointmentOptions,
            selectedKey = appointmentType,
            onSelect = { appointmentType = it },
        )
        Button(onClick = {
            onSave(doctorName, appointmentType)
        }, enabled = doctorName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_save_appointment))
        }
    }
}

@Composable
private fun MoodEntryForm(onSave: (Int, String) -> Unit) {
    var moodScore by remember { mutableIntStateOf(3) }
    var notes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = FormConstants.moodLabels()[moodScore - 1],
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FormConstants.MoodEmojis.forEachIndexed { index, emoji ->
                    val score = index + 1
                    val isSelected = moodScore == score
                    val scale by animateFloatAsState(if (isSelected) 1.5f else 1.0f, label = "scale")

                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { moodScore = score },
                    )
                }
            }
            Slider(
                value = moodScore.toFloat(),
                onValueChange = { moodScore = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
            )
        }

        HorizontalDivider()

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.form_mood_journal_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Button(
            onClick = { onSave(moodScore, notes) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.form_mood_log_button))
        }
    }
}

@Composable
private fun AppointmentDetailSheetContent(appointment: Appointment, onClose: () -> Unit) {
    val dateTime = Instant.fromEpochMilliseconds(
        appointment.appointmentTime,
    ).toLocalDateTime(TimeZone.currentSystemDefault())
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(appointment.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "${DateFormatter.formatDayMonthYear(
                dateTime.date,
            )} at ${DateFormatter.formatTime(dateTime.hour, dateTime.minute)}",
        )
        Text(appointment.appointmentType, color = MaterialTheme.colorScheme.secondary)
        appointment.notes?.takeIf(String::isNotBlank)?.let { Text(it) }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_close_action))
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val dateTime = Instant.fromEpochMilliseconds(
        appointment.appointmentTime,
    ).toLocalDateTime(TimeZone.currentSystemDefault())
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(appointment.title, fontWeight = FontWeight.SemiBold)
            Text(
                DateFormatter.formatTime(dateTime.hour, dateTime.minute),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun entryDateTimeToEpochMs(entryDate: LocalDate, entryTime: Pair<Int, Int>, timeZone: TimeZone): Long {
    val logDateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
    return logDateTime.toInstant(timeZone).toEpochMilliseconds()
}

private fun routineLogRequest(
    routine: RoutineDayAgenda,
    medicationId: String,
    status: MedicationLogStatus,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
): MedicationLogRequest = MedicationLogRequest(
    medicationId = medicationId,
    status = status.name,
    timestampMs = if (status == MedicationLogStatus.TAKEN) nowMs else routine.occurrenceTimeMs,
    routineId = routine.routineId,
    occurrenceTimeMs = routine.occurrenceTimeMs,
    linkMode = MedicationLogLinkMode.ATTACH_TO_OCCURRENCE,
)

@Composable
private fun SimpleDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedValue = options.firstOrNull { it.first == selectedKey }?.second.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, value) ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onSelect(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DailyMoodSummaryCard(moods: List<Mood>) {
    val averageEmoji = moods.averageMoodEmoji()
    val summaryText = if (moods.size == 1) {
        stringResource(Res.string.calendar_mood_one_entry)
    } else {
        stringResource(Res.string.calendar_mood_entry_count, moods.size)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(averageEmoji, style = MaterialTheme.typography.headlineMedium)
                    Column {
                        Text(
                            stringResource(Res.string.calendar_mood_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            moods.sortedByDescending { it.timestamp }.forEach { mood ->
                MoodEntryRow(mood = mood)
            }
        }
    }
}

@Composable
private fun MoodEntryRow(mood: Mood) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val dateTime = remember(mood.timestamp) {
        Instant.fromEpochMilliseconds(mood.timestamp).toLocalDateTime(timeZone)
    }
    val emoji = mood.moodType.toEmoji()

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(emoji, style = MaterialTheme.typography.titleLarge)
                Column {
                    Text(
                        mood.moodType.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    mood.journal?.takeIf(String::isNotBlank)?.let { journal ->
                        Text(
                            journal.take(80) + if (journal.length > 80) "…" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                DateFormatter.formatTime(dateTime.hour, dateTime.minute),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthlyMoodChart(moodCounts: Map<MoodType, Int>) {
    if (moodCounts.isEmpty()) return

    val total = moodCounts.values.sum().toFloat()
    val moodTypes = MoodType.entries
    val moodColors = listOf(
        Color(0xFFE57373),
        Color(0xFFFFB74D),
        Color(0xFFFFF176),
        Color(0xFFAED581),
        Color(0xFF66BB6A),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.calendar_mood_monthly_trends),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Canvas(
            modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)),
        ) {
            var currentX = 0f
            moodTypes.forEachIndexed { index, type ->
                val count = moodCounts[type] ?: 0
                if (count > 0) {
                    val width = (count / total) * size.width
                    drawRect(
                        color = moodColors[index],
                        topLeft = Offset(currentX, 0f),
                        size = Size(width, size.height),
                    )
                    currentX += width
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            moodTypes.forEachIndexed { index, type ->
                val count = moodCounts[type] ?: 0
                if (count > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(moodColors[index]),
                        )
                        Text(
                            "${type.displayName}: $count",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun MoodType.toEmoji(): String = when (this) {
    MoodType.VERY_SAD -> "😢"
    MoodType.SAD -> "😕"
    MoodType.NEUTRAL -> "😐"
    MoodType.GOOD -> "🙂"
    MoodType.GREAT -> "🤩"
}

private fun List<Mood>.averageMoodEmoji(): String {
    if (isEmpty()) return "😐"
    val avg = map { it.moodType.toScore() }.average()
    return when {
        avg >= 4.5 -> "🤩"
        avg >= 3.5 -> "🙂"
        avg >= 2.5 -> "😐"
        avg >= 1.5 -> "😕"
        else -> "😢"
    }
}

private fun MoodType.toScore(): Int = when (this) {
    MoodType.VERY_SAD -> 1
    MoodType.SAD -> 2
    MoodType.NEUTRAL -> 3
    MoodType.GOOD -> 4
    MoodType.GREAT -> 5
}
