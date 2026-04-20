package com.group8.comp2300.feature.medication

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.core.ui.components.ConsumeSnackbarMessage
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.feature.medical.shared.routines.ScheduleFormSheet
import com.group8.comp2300.platform.notifications.NotificationPermissionResult
import com.group8.comp2300.platform.notifications.rememberNotificationPermissionRequester
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class MedicationSheetStep { FORM, SCHEDULE_FORM }

@Composable
fun MedicationScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    viewModel: MedicationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var sheetStep by remember { mutableStateOf(MedicationSheetStep.FORM) }
    val notificationDisabledMessage = stringResource(Res.string.medical_medication_notification_disabled)

    ConsumeSnackbarMessage(
        message = state.error,
        snackbarHostState = snackbarHostState,
        onConsumed = viewModel::dismissError,
    )

    fun linkedRoutinesFor(medication: Medication): List<Routine> = state.routines.filter { routine ->
        routine.status == RoutineStatus.ACTIVE && medication.id in routine.medicationIds
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text(stringResource(Res.string.medical_medication_title), fontWeight = FontWeight.Bold) },
                onBackClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMedication = null
                    editingRoutine = null
                    sheetStep = MedicationSheetStep.FORM
                    showSheet = true
                },
            ) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_medication_add_desc),
                )
            }
        },
    ) { innerPadding ->
        MedicationListContent(
            state = state,
            onAddMedication = {
                editingMedication = null
                editingRoutine = null
                sheetStep = MedicationSheetStep.FORM
                showSheet = true
            },
            onEditMedication = { medication ->
                editingMedication = medication
                editingRoutine = null
                sheetStep = MedicationSheetStep.FORM
                showSheet = true
            },
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                editingRoutine = null
                sheetStep = MedicationSheetStep.FORM
            },
            sheetState = sheetState,
        ) {
            when (sheetStep) {
                MedicationSheetStep.FORM -> {
                    val linkedSchedules = editingMedication?.let(::linkedRoutinesFor).orEmpty()

                    MedicationFormSheet(
                        medicationToEdit = editingMedication,
                        linkedSchedules = linkedSchedules,
                        onSave = { request, id, addScheduleAfterSave ->
                            viewModel.saveMedication(request, id) { savedMedication ->
                                editingMedication = savedMedication
                                when {
                                    id != null -> {
                                        showSheet = false
                                        sheetStep = MedicationSheetStep.FORM
                                    }

                                    addScheduleAfterSave -> {
                                        editingRoutine = null
                                        sheetStep = MedicationSheetStep.SCHEDULE_FORM
                                    }

                                    else -> {
                                        showSheet = false
                                        sheetStep = MedicationSheetStep.FORM
                                    }
                                }
                            }
                        },
                        onDelete = {
                            viewModel.deleteMedication(it)
                            showSheet = false
                            editingRoutine = null
                            sheetStep = MedicationSheetStep.FORM
                        },
                        onCancel = {
                            showSheet = false
                            editingRoutine = null
                            sheetStep = MedicationSheetStep.FORM
                        },
                        onAddSchedule = editingMedication
                            ?.takeIf { it.status == MedicationStatus.ACTIVE }
                            ?.let {
                                {
                                    editingRoutine = null
                                    sheetStep = MedicationSheetStep.SCHEDULE_FORM
                                }
                            },
                        onEditSchedule = { routine ->
                            editingRoutine = routine
                            sheetStep = MedicationSheetStep.SCHEDULE_FORM
                        },
                        onRemoveSchedule = { routine ->
                            editingMedication?.let { medication ->
                                viewModel.unlinkMedicationFromRoutine(medication.id, routine.id)
                            }
                        },
                    )
                }

                MedicationSheetStep.SCHEDULE_FORM -> editingMedication?.let { medication ->
                    ScheduleFormSheet(
                        title = if (editingRoutine == null) {
                            stringResource(Res.string.medical_routine_form_add_title)
                        } else {
                            stringResource(Res.string.medical_routine_form_edit_title)
                        },
                        subtitle = null,
                        routineToEdit = editingRoutine,
                        medications = state.medications.filter { it.status == MedicationStatus.ACTIVE },
                        initialSelectedMedicationIds = setOf(medication.id),
                        onSave = { request, id ->
                            coroutineScope.launch {
                                val permissionResult =
                                    if (request.hasReminder && request.reminderOffsetsMins.isNotEmpty()) {
                                        requestNotificationPermission()
                                    } else {
                                        NotificationPermissionResult.GRANTED
                                    }
                                viewModel.saveRoutine(request, id) {
                                    editingRoutine = null
                                    sheetStep = MedicationSheetStep.FORM
                                }
                                if (permissionResult != NotificationPermissionResult.GRANTED) {
                                    snackbarHostState.showSnackbar(notificationDisabledMessage)
                                }
                            }
                        },
                        onDelete = { routineId ->
                            viewModel.deleteRoutine(routineId)
                            editingRoutine = null
                            sheetStep = MedicationSheetStep.FORM
                        },
                        onCancel = {
                            editingRoutine = null
                            sheetStep = MedicationSheetStep.FORM
                        },
                    )
                }
            }
        }
    }
}
