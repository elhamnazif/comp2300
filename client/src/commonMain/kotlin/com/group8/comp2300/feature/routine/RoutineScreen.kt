package com.group8.comp2300.feature.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.ActionEmptyStateCard
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.core.ui.components.ConsumeSnackbarMessage
import com.group8.comp2300.core.ui.components.SectionHeader
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.feature.medical.shared.routines.ReminderIndicator
import com.group8.comp2300.feature.medical.shared.routines.ScheduleFormSheet
import com.group8.comp2300.feature.medical.shared.routines.scheduleLinkSummary
import com.group8.comp2300.platform.notifications.NotificationPermissionResult
import com.group8.comp2300.platform.notifications.rememberNotificationPermissionRequester
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
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
    val notificationDisabledMessage = stringResource(Res.string.medical_routine_notification_disabled)

    ConsumeSnackbarMessage(
        message = state.error,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::dismissError,
    )

    val activeRoutines = state.routines.filter { it.status == RoutineStatus.ACTIVE }
    val archivedRoutines = state.routines.filter { it.status == RoutineStatus.ARCHIVED }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.medical_routine_title), fontWeight = FontWeight.Bold) },
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingRoutine = null
                showSheet = true
            }) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_routine_add_desc),
                )
            }
        },
    ) { innerPadding ->
        if (state.isLoading && activeRoutines.isEmpty() && archivedRoutines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading schedules",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
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
                        ActionEmptyStateCard(
                            title = stringResource(Res.string.medical_routine_empty_title),
                            message = stringResource(Res.string.medical_routine_empty_desc),
                            actionLabel = stringResource(Res.string.medical_routine_empty_button),
                            onAction = {
                                editingRoutine = null
                                showSheet = true
                            },
                        )
                    }
                } else {
                    if (activeRoutines.isNotEmpty()) {
                        item {
                            SectionHeader(
                                stringResource(Res.string.medical_medication_section_active),
                                activeRoutines.size,
                            )
                        }
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
                    item {
                        SectionHeader(
                            stringResource(Res.string.medical_medication_section_archived),
                            archivedRoutines.size,
                        )
                    }
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
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!state.isMutating) {
                    showSheet = false
                }
            },
            sheetState = sheetState,
        ) {
            ScheduleFormSheet(
                title = if (editingRoutine == null) {
                    stringResource(Res.string.medical_routine_form_add_title)
                } else {
                    stringResource(Res.string.medical_routine_form_edit_title)
                },
                routineToEdit = editingRoutine,
                medications = state.medications.filter { it.status == MedicationStatus.ACTIVE },
                isMutating = state.isMutating,
                onSave = { request, id ->
                    coroutineScope.launch {
                        val permissionResult = if (request.hasReminder && request.reminderOffsetsMins.isNotEmpty()) {
                            requestNotificationPermission()
                        } else {
                            NotificationPermissionResult.GRANTED
                        }
                        viewModel.saveRoutine(request, id) {
                            showSheet = false
                        }
                        if (permissionResult != NotificationPermissionResult.GRANTED) {
                            snackbarHostState.showSnackbar(notificationDisabledMessage)
                        }
                    }
                },
                onDelete = { routineId ->
                    viewModel.deleteRoutine(routineId) {
                        showSheet = false
                    }
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
    val secondaryMeta = rememberRoutineMeta(routine = routine, linkedCount = linked.size, isArchived = isArchived)
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
                    ReminderIndicator(
                        reminderOffsetsMins = routine.reminderOffsetsMins,
                        contentDescription = stringResource(Res.string.calendar_reminders_enabled_desc),
                        textFontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                scheduleLinkSummary(routine),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                secondaryMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun rememberRoutineMeta(routine: Routine, linkedCount: Int, isArchived: Boolean): String {
    val medicationSummary = when (linkedCount) {
        0 -> stringResource(Res.string.medical_routine_card_no_medications)
        1 -> stringResource(Res.string.medical_routine_card_one_medication)
        else -> stringResource(Res.string.medical_routine_card_many_medications, linkedCount)
    }
    val endDate = routine.endDate
    val statusSummary = when {
        isArchived -> stringResource(Res.string.medical_routine_card_archived)

        endDate.isNullOrBlank() -> stringResource(Res.string.medical_routine_card_ongoing)

        else -> stringResource(
            Res.string.medical_routine_card_ends_on,
            LocalDate.parse(endDate).toString(),
        )
    }
    return "$medicationSummary • $statusSummary"
}
