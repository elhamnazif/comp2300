package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.presentation.screens.medical.*
import com.group8.comp2300.presentation.util.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.form_mood_journal_label
import comp2300.i18n.generated.resources.form_mood_log_button
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Instant

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

    var showBottomSheet by remember { mutableStateOf(false) }
    var currentSheetView by remember { mutableStateOf(SheetView.MENU) }
    var selectedDateForEntry by remember { mutableStateOf<CalendarDay?>(null) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var entryDate by remember { mutableStateOf(today) }
    var entryTime by remember { mutableStateOf(9 to 0) }
    var pendingMedicationLog by remember { mutableStateOf<MedicationLogRequest?>(null) }
    var pendingOccurrenceCandidates by remember { mutableStateOf<List<MedicationOccurrenceCandidate>>(emptyList()) }
    var pendingRoutineReschedule by remember { mutableStateOf<RoutineDayAgenda?>(null) }

    LaunchedEffect(Unit) {
        selectedDateForEntry = CalendarDay(today.day, today, AdherenceStatus.NONE, isToday = true, isCurrentMonth = true)
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
                .toLocalDateTime(TimeZone.currentSystemDefault())
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
        topBar = { AppTopBar(title = { Text("Calendar", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedDateForEntry = CalendarDay(selectedDate.day, selectedDate, AdherenceStatus.NONE, isToday = selectedDate == today, isCurrentMonth = true)
                entryDate = selectedDate
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                entryTime = now.hour to now.minute
                currentSheetView = SheetView.MENU
                selectedAppointment = null
                pendingMedicationLog = null
                pendingOccurrenceCandidates = emptyList()
                pendingRoutineReschedule = null
                showBottomSheet = true
            }) {
                Icon(Icons.AddW400Outlinedfill1, contentDescription = "Add entry")
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
                    onMonthChanged = viewModel::loadOverviewForMonth,
                )
            }

            item {
                CalendarDayHeader(
                    date = selectedDate,
                    agenda = state.routineAgenda,
                    extraLogCount = state.manualLogs.size,
                    appointmentCount = selectedAppointments.size,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Scheduled doses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (state.routineAgenda.isEmpty()) {
                        EmptyStateMessage("No scheduled doses for this day.")
                    } else {
                        state.routineAgenda.forEach { routine ->
                            val routineKey = "${routine.routineId}:${routine.occurrenceTimeMs}"
                            RoutineAgendaCard(
                                routine = routine,
                                isExpanded = routineKey in expandedRoutineKeys,
                                onToggleExpanded = {
                                    expandedRoutineKeys = expandedRoutineKeys.toMutableSet().apply {
                                        if (!add(routineKey)) remove(routineKey)
                                    }
                                },
                                onLogMedication = { medicationId, status ->
                                    viewModel.logMedication(
                                        MedicationLogRequest(
                                            medicationId = medicationId,
                                            status = status.name,
                                            timestampMs = if (status == MedicationLogStatus.TAKEN) Clock.System.now().toEpochMilliseconds() else routine.occurrenceTimeMs,
                                            routineId = routine.routineId,
                                            occurrenceTimeMs = routine.occurrenceTimeMs,
                                            linkMode = MedicationLogLinkMode.ATTACH_TO_OCCURRENCE,
                                        ),
                                    )
                                },
                                onLogAll = { status ->
                                    routine.medications.forEach { medication ->
                                        viewModel.logMedication(
                                            MedicationLogRequest(
                                                medicationId = medication.medicationId,
                                                status = status.name,
                                                timestampMs = if (status == MedicationLogStatus.TAKEN) Clock.System.now().toEpochMilliseconds() else routine.occurrenceTimeMs,
                                                routineId = routine.routineId,
                                                occurrenceTimeMs = routine.occurrenceTimeMs,
                                                linkMode = MedicationLogLinkMode.ATTACH_TO_OCCURRENCE,
                                            ),
                                        )
                                    }
                                },
                                onMoveDose = {
                                    val occurrence = Instant.fromEpochMilliseconds(routine.occurrenceTimeMs)
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                    entryDate = occurrence.date
                                    entryTime = occurrence.hour to occurrence.minute
                                    pendingRoutineReschedule = routine
                                    currentSheetView = SheetView.FORM_RESCHEDULE
                                    showBottomSheet = true
                                },
                            )
                        }
                    }
                }
            }

            if (state.manualLogs.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Extra logs", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.manualLogs.forEach { log ->
                            ManualLogCard(log = log)
                        }
                    }
                }
            }

            if (selectedAppointments.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appointments", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        selectedAppointments.forEach { appointment ->
                            AppointmentCard(appointment = appointment, onClick = {
                                selectedAppointment = appointment
                                currentSheetView = SheetView.DETAILS_APPT
                                showBottomSheet = true
                            })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedAppointment = null
                pendingMedicationLog = null
                pendingOccurrenceCandidates = emptyList()
                pendingRoutineReschedule = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            when (currentSheetView) {
                SheetView.MENU -> AddEntryMenu(
                    selectedDate = selectedDate,
                    onSelectType = { currentSheetView = it },
                )

                SheetView.FORM_MED -> WrapperFormLayout(
                    title = "Log medication",
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = {
                        pendingMedicationLog = null
                        pendingOccurrenceCandidates = emptyList()
                        currentSheetView = SheetView.MENU
                    },
                ) {
                    ManualMedicationLogForm(
                        medications = activeMedications,
                        onSave = { medicationId, status ->
                            val logDateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
                            val timestampMs = logDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
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
                                    viewModel.logMedication(baseRequest.copy(linkMode = MedicationLogLinkMode.EXTRA_DOSE))
                                    showBottomSheet = false
                                } else {
                                    pendingMedicationLog = baseRequest
                                    pendingOccurrenceCandidates = candidates
                                    currentSheetView = SheetView.RESOLVE_MED
                                }
                            }
                        },
                        onOpenMedicationCabinet = {
                            showBottomSheet = false
                            onNavigateToMedication()
                        },
                    )
                }

                SheetView.FORM_RESCHEDULE -> pendingRoutineReschedule?.let { routine ->
                    WrapperFormLayout(
                        title = "Move scheduled dose",
                        entryDate = entryDate,
                        entryTime = entryTime,
                        onDateChange = { entryDate = it },
                        onTimeChange = { h, m -> entryTime = h to m },
                        onBack = {
                            pendingRoutineReschedule = null
                            currentSheetView = SheetView.MENU
                        },
                    ) {
                        val originalOccurrence = Instant.fromEpochMilliseconds(routine.originalOccurrenceTimeMs)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                        Text(
                            "This only changes the selected dose. Future doses will stay on their usual schedule.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (routine.isRescheduled) {
                            Text(
                                "Originally due ${DateFormatter.formatDayMonthYear(originalOccurrence.date)} at ${DateFormatter.formatTime(originalOccurrence.hour, originalOccurrence.minute)}",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Button(
                            onClick = {
                                val targetDateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
                                viewModel.rescheduleRoutineOccurrence(
                                    RoutineOccurrenceOverrideRequest(
                                        routineId = routine.routineId,
                                        originalOccurrenceTimeMs = routine.originalOccurrenceTimeMs,
                                        rescheduledOccurrenceTimeMs = targetDateTime
                                            .toInstant(TimeZone.currentSystemDefault())
                                            .toEpochMilliseconds(),
                                    ),
                                )
                                pendingRoutineReschedule = null
                                showBottomSheet = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save moved dose")
                        }
                    }
                }

                SheetView.RESOLVE_MED -> pendingMedicationLog?.let { pendingLog ->
                    ResolveMedicationLogSheet(
                        medicationName = state.medications.firstOrNull { it.id == pendingLog.medicationId }?.name.orEmpty(),
                        candidates = pendingOccurrenceCandidates,
                        onBack = {
                            pendingOccurrenceCandidates = emptyList()
                            currentSheetView = SheetView.FORM_MED
                        },
                        onAttach = { candidate ->
                            viewModel.logMedication(
                                pendingLog.copy(
                                    routineId = candidate.routineId,
                                    occurrenceTimeMs = candidate.occurrenceTimeMs,
                                    linkMode = MedicationLogLinkMode.ATTACH_TO_OCCURRENCE,
                                ),
                            )
                            pendingMedicationLog = null
                            pendingOccurrenceCandidates = emptyList()
                            showBottomSheet = false
                        },
                        onLogExtraDose = {
                            viewModel.logMedication(pendingLog.copy(linkMode = MedicationLogLinkMode.EXTRA_DOSE))
                            pendingMedicationLog = null
                            pendingOccurrenceCandidates = emptyList()
                            showBottomSheet = false
                        },
                    )
                }

                SheetView.FORM_APPT -> WrapperFormLayout(
                    title = "Add appointment",
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = { currentSheetView = SheetView.MENU },
                ) {
                    AppointmentForm { doctorName, appointmentType ->
                        val dateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
                        viewModel.scheduleAppointment(
                            doctorName,
                            appointmentType,
                            dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
                        )
                        showBottomSheet = false
                    }
                }

                SheetView.FORM_MOOD -> WrapperFormLayout(
                    title = "Log mood",
                    entryDate = entryDate,
                    entryTime = entryTime,
                    onDateChange = { entryDate = it },
                    onTimeChange = { h, m -> entryTime = h to m },
                    onBack = { currentSheetView = SheetView.MENU },
                ) {
                    MoodEntryForm { score, tags, symptoms, notes ->
                        viewModel.logMood(score, tags, symptoms, notes)
                        showBottomSheet = false
                    }
                }

                SheetView.DETAILS_APPT -> selectedAppointment?.let {
                    AppointmentDetailSheetContent(it, onDelete = { showBottomSheet = false })
                }
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
) {
    val allDoses = agenda.flatMap(RoutineDayAgenda::medications)
    val taken = allDoses.count { it.status == MedicationLogStatus.TAKEN }
    val unresolved = allDoses.count {
        it.status == MedicationLogStatus.PENDING ||
            it.status == MedicationLogStatus.MISSED ||
            it.status == MedicationLogStatus.SNOOZED
    }
    val summary = when {
        allDoses.isEmpty() -> "No scheduled doses"
        else -> "$taken of ${allDoses.size} taken"
    }
    val meta = buildList {
        if (allDoses.isNotEmpty() && unresolved > 0) add("$unresolved unresolved")
        if (extraLogCount > 0) add("$extraLogCount extra log${if (extraLogCount == 1) "" else "s"}")
        if (appointmentCount > 0) add("$appointmentCount appointment${if (appointmentCount == 1) "" else "s"}")
    }.joinToString(" • ")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            DateFormatter.formatDayMonthYear(date),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            summary,
            style = MaterialTheme.typography.titleMedium,
            color = if (allDoses.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
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
    onToggleExpanded: () -> Unit,
    onLogMedication: (String, MedicationLogStatus) -> Unit,
    onLogAll: (MedicationLogStatus) -> Unit,
    onMoveDose: () -> Unit,
) {
    val occurrence = Instant.fromEpochMilliseconds(routine.occurrenceTimeMs).toLocalDateTime(TimeZone.currentSystemDefault())
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

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Text(routine.routineName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (routine.hasReminder && routine.reminderOffsetsMins.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.NotificationsW400Outlinedfill1,
                            contentDescription = "Reminders enabled",
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

            buildList {
                if (routine.isRescheduled) {
                    val originalOccurrence = Instant.fromEpochMilliseconds(routine.originalOccurrenceTimeMs)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    add(
                        "Moved from ${DateFormatter.formatDayMonthYear(originalOccurrence.date)} at ${DateFormatter.formatTime(originalOccurrence.hour, originalOccurrence.minute)}",
                    )
                }
                if (routineCompleted) {
                    add(routineCompletionSummary(routine.medications))
                }
            }.forEach { metaLine ->
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
                        ) { Text("Taken") }
                        OutlinedButton(
                            onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.SKIPPED) },
                            modifier = Modifier.weight(1f),
                        ) { Text("Skip") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onLogAll(MedicationLogStatus.TAKEN) }, modifier = Modifier.weight(1f)) {
                            Text("Take all")
                        }
                        OutlinedButton(onClick = { onLogAll(MedicationLogStatus.SKIPPED) }, modifier = Modifier.weight(1f)) {
                            Text("Skip all")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showReviewToggle) {
                            TextButton(onClick = onToggleExpanded) {
                                Text(if (isExpanded) "Hide medications" else "Review individually")
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        TextButton(onClick = onMoveDose) {
                            Text(if (routine.isRescheduled) "Move again" else "Move")
                        }
                    }
                }
                if (routine.medications.size == 1) {
                    TextButton(onClick = onMoveDose, modifier = Modifier.fillMaxWidth()) {
                        Text(if (routine.isRescheduled) "Move again" else "Move dose")
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
                Text(medication.dosage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Button(onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.TAKEN) }, modifier = Modifier.weight(1f)) {
                    Text("Taken")
                }
                OutlinedButton(onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.SKIPPED) }, modifier = Modifier.weight(1f)) {
                    Text("Skip")
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

private fun routineCompletionSummary(medications: List<RoutineMedicationAgenda>): String {
    val taken = medications.count { it.status == MedicationLogStatus.TAKEN }
    val skipped = medications.count { it.status == MedicationLogStatus.SKIPPED || it.status == MedicationLogStatus.MISSED }
    return when {
        taken == medications.size -> "All medications taken"
        skipped == medications.size -> "Marked skipped"
        taken > 0 && skipped > 0 -> "$taken taken • $skipped skipped"
        else -> "Completed"
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
                Text(log.medicationName ?: "Medication", fontWeight = FontWeight.SemiBold)
                Text(
                    "${log.status.displayName} at ${DateFormatter.formatTime(dateTime.hour, dateTime.minute)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Extra",
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
    onMonthChanged: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val displayDate = remember(baseDate, monthOffset) { baseDate.plus(monthOffset, DateTimeUnit.MONTH) }
    LaunchedEffect(displayDate) {
        onMonthChanged(displayDate.year, displayDate.month.number)
    }
    val overviewMap = remember(overview) { overview.associate { it.date to it.status } }
    val calendarDays = remember(displayDate, overviewMap) { generateCalendarDays(displayDate.year, displayDate.month, overviewMap) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = "Previous month")
                }
                Text(DateFormatter.formatMonthYear(displayDate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { monthOffset++ }) {
                    Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = "Next month", modifier = Modifier.rotate(180f))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(8.dp))
            calendarDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(day = day, isSelected = selectedDate?.date == day.date, onClick = { onDayClick(day) })
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
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
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
            EmptyStateMessage("No medications yet.")
        } else {
            SimpleDropdown(
                label = "Medication",
                options = medications.map { it.id to "${it.name} • ${it.dosage}" },
                selectedKey = selectedMedId,
                onSelect = { selectedMedId = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MedicationLogStatus.TAKEN, MedicationLogStatus.SKIPPED).forEach { status ->
                    FilterChip(selected = selectedStatus == status, onClick = { selectedStatus = status }, label = { Text(status.displayName) })
                }
            }
            Button(onClick = { onSave(selectedMedId, selectedStatus) }, enabled = selectedMedId.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Save log")
            }
        }
        TextButton(onClick = onOpenMedicationCabinet, modifier = Modifier.fillMaxWidth()) {
            Text("Manage medications")
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
                Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = "Back")
            }
            Text("Match scheduled dose?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "$medicationName is close to a scheduled dose. Match it or keep it as an extra log.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        candidates.forEach { candidate ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(candidate.routineName, fontWeight = FontWeight.SemiBold)
                    val occurrence = Instant.fromEpochMilliseconds(candidate.occurrenceTimeMs)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        "${DateFormatter.formatDayMonthYear(occurrence.date)} at ${DateFormatter.formatTime(occurrence.hour, occurrence.minute)} • ${candidate.status.displayName}",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Button(onClick = { onAttach(candidate) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Count toward schedule")
                    }
                }
            }
        }
        OutlinedButton(onClick = onLogExtraDose, modifier = Modifier.fillMaxWidth()) {
            Text("Save as extra log")
        }
    }
}

@Composable
private fun AddEntryMenu(selectedDate: LocalDate, onSelectType: (SheetView) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add entry", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            DateFormatter.formatDayMonthYear(selectedDate),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { onSelectType(SheetView.FORM_MED) }, modifier = Modifier.fillMaxWidth()) { Text("Log medication") }
        OutlinedButton(onClick = { onSelectType(SheetView.FORM_APPT) }, modifier = Modifier.fillMaxWidth()) { Text("Add appointment") }
        OutlinedButton(onClick = { onSelectType(SheetView.FORM_MOOD) }, modifier = Modifier.fillMaxWidth()) { Text("Log mood") }
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
                Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = "Back")
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        DateValueField(label = "Date", value = entryDate, onClick = { activeDatePicker = true })
        TimeValueField(label = "Time", value = ((entryTime.first * 60L) + entryTime.second) * 60_000L, onClick = { activeTimePicker = true })
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
    var appointmentType by remember { mutableStateOf("Consultation") }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(value = doctorName, onValueChange = { doctorName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Doctor / clinic") })
        SimpleDropdown(
            label = "Appointment type",
            options = listOf("Consultation", "Lab work", "Follow-up").associateBy { it }.map { it.key to it.value },
            selectedKey = appointmentType,
            onSelect = { appointmentType = it },
        )
        Button(onClick = { onSave(doctorName, appointmentType) }, enabled = doctorName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("Save appointment")
        }
    }
}

@Composable
private fun MoodEntryForm(onSave: (Int, List<String>, List<String>, String) -> Unit) {
    var moodScore by remember { mutableIntStateOf(3) }
    val selectedTags = remember { mutableStateListOf<String>() }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
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
            onClick = { onSave(moodScore, selectedTags, selectedSymptoms, notes) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.form_mood_log_button))
        }
    }
}

@Composable
private fun AppointmentDetailSheetContent(appointment: Appointment, onDelete: () -> Unit) {
    val dateTime = Instant.fromEpochMilliseconds(appointment.appointmentTime).toLocalDateTime(TimeZone.currentSystemDefault())
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(appointment.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${DateFormatter.formatDayMonthYear(dateTime.date)} at ${DateFormatter.formatTime(dateTime.hour, dateTime.minute)}")
        Text(appointment.appointmentType, color = MaterialTheme.colorScheme.secondary)
        appointment.notes?.takeIf(String::isNotBlank)?.let { Text(it) }
        TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, onClick: () -> Unit = {}) {
    val dateTime = Instant.fromEpochMilliseconds(appointment.appointmentTime).toLocalDateTime(TimeZone.currentSystemDefault())
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(appointment.title, fontWeight = FontWeight.SemiBold)
            Text(DateFormatter.formatTime(dateTime.hour, dateTime.minute), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

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
