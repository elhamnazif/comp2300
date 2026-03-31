package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.presentation.notifications.NotificationPermissionResult
import com.group8.comp2300.presentation.notifications.rememberNotificationPermissionRequester
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.NotificationsW400Outlinedfill1
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoutineScreen(modifier: Modifier = Modifier, onBack: () -> Unit, viewModel: RoutineViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val activeRoutines = state.routines.filter { it.status == RoutineStatus.ACTIVE }
    val archivedRoutines = state.routines.filter { it.status == RoutineStatus.ARCHIVED }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text("Schedules", fontWeight = FontWeight.Bold) },
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRoutine = null
                showSheet = true
            }) {
                Icon(Icons.AddW400Outlinedfill1, contentDescription = "Add schedule")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (activeRoutines.isEmpty() && archivedRoutines.isEmpty()) {
                item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "No schedules yet",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Create reminder schedules like Morning meds or Bedtime meds.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = {
                                editingRoutine = null
                                showSheet = true
                            }) {
                                Text("Create schedule")
                            }
                        }
                    }
                }
            } else {
                if (activeRoutines.isNotEmpty()) {
                    item { SectionHeader("Active", activeRoutines.size) }
                }
                items(activeRoutines, key = Routine::id) { routine ->
                    RoutineCard(
                        routine = routine,
                        medications = state.medications,
                        onClick = {
                            editingRoutine = routine
                            showSheet = true
                        },
                    )
                }
            }
            if (archivedRoutines.isNotEmpty()) {
                item { SectionHeader("Archived", archivedRoutines.size) }
                items(archivedRoutines, key = Routine::id) { routine ->
                    RoutineCard(
                        routine = routine,
                        medications = state.medications,
                        isArchived = true,
                        onClick = {
                            editingRoutine = routine
                            showSheet = true
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            ScheduleFormSheet(
                title = if (editingRoutine == null) "Add schedule" else "Edit schedule",
                routineToEdit = editingRoutine,
                medications = state.medications.filter { it.status == MedicationStatus.ACTIVE },
                onSave = { request, id ->
                    coroutineScope.launch {
                        val permissionResult = if (request.hasReminder && request.reminderOffsetsMins.isNotEmpty()) {
                            requestNotificationPermission()
                        } else {
                            NotificationPermissionResult.GRANTED
                        }
                        viewModel.saveRoutine(request, id)
                        if (permissionResult != NotificationPermissionResult.GRANTED) {
                            snackbarHostState.showSnackbar(
                                "Schedule saved, but notifications are disabled in system settings.",
                            )
                        }
                    }
                    showSheet = false
                },
                onDelete = { routineId ->
                    viewModel.deleteRoutine(routineId)
                    showSheet = false
                },
                onCancel = { showSheet = false },
            )
        }
    }
}

@Composable
private fun RoutineCard(
    routine: Routine,
    medications: List<Medication>,
    onClick: () -> Unit,
    isArchived: Boolean = false,
) {
    val linked = medications.filter { it.id in routine.medicationIds }
    val reminderMeta = reminderMetaLabel(routine.reminderOffsetsMins)
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor =
            if (isArchived) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (routine.hasReminder && routine.reminderOffsetsMins.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.NotificationsW400Outlinedfill1,
                            contentDescription = "Reminders enabled",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        reminderMeta?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            Text(
                "${formatTimesSummary(routine.timesOfDayMs)} • ${routineRepeatSummary(routine)}",
                color = MaterialTheme.colorScheme.secondary,
            )
            if (linked.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    linked.take(3).forEach { medication ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                medication.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    if (linked.size > 3) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                "+${linked.size - 3}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun routineRepeatSummary(routine: Routine): String = when (routine.repeatType) {
    com.group8.comp2300.domain.model.medical.RoutineRepeatType.DAILY -> "Every day"

    com.group8.comp2300.domain.model.medical.RoutineRepeatType.WEEKLY -> {
        routine.daysOfWeek.sorted().joinToString { day ->
            when (day) {
                0 -> "Sun"
                1 -> "Mon"
                2 -> "Tue"
                3 -> "Wed"
                4 -> "Thu"
                5 -> "Fri"
                6 -> "Sat"
                else -> ""
            }
        }
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
