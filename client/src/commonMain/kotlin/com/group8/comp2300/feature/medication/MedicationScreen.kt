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
import com.group8.comp2300.feature.medical.shared.routines.rememberMedicalSheetChrome
import com.group8.comp2300.platform.notifications.NotificationPermissionResult
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
    val sheetChrome = rememberMedicalSheetChrome()
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var sheetStep by remember { mutableStateOf(MedicationSheetStep.FORM) }
    var pendingCreatedMedicationId by remember { mutableStateOf<String?>(null) }
    val notificationDisabledMessage = stringResource(Res.string.medical_medication_notification_disabled)

    ConsumeSnackbarMessage(
        message = state.error,
        snackbarHostState = sheetChrome.snackbarHostState,
        onConsumed = viewModel::dismissError,
    )

    fun linkedRoutinesFor(medication: Medication): List<Routine> = state.routines.filter { routine ->
        routine.status == RoutineStatus.ACTIVE && medication.id in routine.medicationIds
    }

    fun resetSheetState() {
        sheetChrome.showSheet = false
        editingMedication = null
        editingRoutine = null
        sheetStep = MedicationSheetStep.FORM
        pendingCreatedMedicationId = null
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(sheetChrome.snackbarHostState) },
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
                    pendingCreatedMedicationId = null
                    sheetChrome.showSheet = true
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
                pendingCreatedMedicationId = null
                sheetChrome.showSheet = true
            },
            onEditMedication = { medication ->
                editingMedication = medication
                editingRoutine = null
                sheetStep = MedicationSheetStep.FORM
                pendingCreatedMedicationId = null
                sheetChrome.showSheet = true
            },
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (sheetChrome.showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!state.isMutating) {
                    resetSheetState()
                }
            },
            sheetState = sheetChrome.sheetState,
        ) {
            when (sheetStep) {
                MedicationSheetStep.FORM -> {
                    val linkedSchedules = editingMedication?.let(::linkedRoutinesFor).orEmpty()

                    MedicationFormSheet(
                        medicationToEdit = editingMedication,
                        linkedSchedules = linkedSchedules,
                        isMutating = state.isMutating,
                        onSave = { medicationRequest, routineRequest ->
                            val targetMedicationId = editingMedication?.id ?: pendingCreatedMedicationId
                            viewModel.saveMedication(medicationRequest, targetMedicationId) { savedMedication ->
                                if (routineRequest == null) {
                                    resetSheetState()
                                    return@saveMedication
                                }

                                pendingCreatedMedicationId = savedMedication.id
                                sheetChrome.coroutineScope.launch {
                                    val requestWithMedication = routineRequest.copy(
                                        medicationIds = listOf(savedMedication.id),
                                    )
                                    val permissionResult =
                                        if (requestWithMedication.hasReminder &&
                                            requestWithMedication.reminderOffsetsMins.isNotEmpty()
                                        ) {
                                            sheetChrome.requestNotificationPermission()
                                        } else {
                                            NotificationPermissionResult.GRANTED
                                        }
                                    viewModel.saveRoutine(requestWithMedication) {
                                        resetSheetState()
                                    }
                                    if (permissionResult != NotificationPermissionResult.GRANTED) {
                                        sheetChrome.snackbarHostState.showSnackbar(notificationDisabledMessage)
                                    }
                                }
                            }
                        },
                        onDelete = {
                            viewModel.deleteMedication(it) {
                                resetSheetState()
                            }
                        },
                        onCancel = ::resetSheetState,
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
                        subtitle = medication.name,
                        routineToEdit = editingRoutine,
                        medications = state.medications.filter { it.status == MedicationStatus.ACTIVE },
                        initialSelectedMedicationIds = setOf(medication.id),
                        isMutating = state.isMutating,
                        showMedicationSection = false,
                        onSave = { request, id ->
                            sheetChrome.coroutineScope.launch {
                                val permissionResult =
                                    if (request.hasReminder && request.reminderOffsetsMins.isNotEmpty()) {
                                        sheetChrome.requestNotificationPermission()
                                    } else {
                                        NotificationPermissionResult.GRANTED
                                    }
                                viewModel.saveRoutine(request, id) {
                                    editingRoutine = null
                                    sheetStep = MedicationSheetStep.FORM
                                }
                                if (permissionResult != NotificationPermissionResult.GRANTED) {
                                    sheetChrome.snackbarHostState.showSnackbar(notificationDisabledMessage)
                                }
                            }
                        },
                        onDelete = { routineId ->
                            viewModel.deleteRoutine(routineId) {
                                editingRoutine = null
                                sheetStep = MedicationSheetStep.FORM
                            }
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
