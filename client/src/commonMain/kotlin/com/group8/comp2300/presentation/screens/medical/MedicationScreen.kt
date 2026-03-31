package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EditW400Outlinedfill1
import org.koin.compose.viewmodel.koinViewModel

private enum class MedicationSheetStep {
    FORM,
    SCHEDULE_LINKS,
    SCHEDULE_FORM,
}

@Composable
fun MedicationScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onNavigateToRoutines: () -> Unit = {},
    viewModel: MedicationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var editingMedication by remember { mutableStateOf<Medication?>(null) }
    var editingRoutine by remember { mutableStateOf<Routine?>(null) }
    var sheetStep by remember { mutableStateOf(MedicationSheetStep.FORM) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val activeMedications = state.medications.filter { it.status == MedicationStatus.ACTIVE }
    val archivedMedications = state.medications.filter { it.status == MedicationStatus.ARCHIVED }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text("Medication cabinet", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.AddW400Outlinedfill1, contentDescription = "Add medication")
            }
        },
    ) { innerPadding ->
        if (state.isLoading && state.medications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
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
                if (state.medications.isEmpty()) {
                    item {
                        MedicationEmptyState(
                            onAddMedication = {
                                editingMedication = null
                                editingRoutine = null
                                sheetStep = MedicationSheetStep.FORM
                                showSheet = true
                            },
                        )
                    }
                } else {
                    if (activeMedications.isNotEmpty()) {
                        item { SectionHeader("Active", activeMedications.size) }
                        items(activeMedications, key = Medication::id) { medication ->
                            MedicationCard(
                                medication = medication,
                                linkedScheduleCount = state.linkedRoutineCounts[medication.id] ?: 0,
                                onClick = {
                                    editingMedication = medication
                                    editingRoutine = null
                                    sheetStep = MedicationSheetStep.FORM
                                    showSheet = true
                                },
                            )
                        }
                    }

                    if (activeMedications.isEmpty() && archivedMedications.isNotEmpty()) {
                        item { EmptyStateMessage("No active medications right now.") }
                    }

                    if (archivedMedications.isNotEmpty()) {
                        item { SectionHeader("Archived", archivedMedications.size) }
                        items(archivedMedications, key = Medication::id) { medication ->
                            MedicationCard(
                                medication = medication,
                                linkedScheduleCount = state.linkedRoutineCounts[medication.id] ?: 0,
                                isArchived = true,
                                onClick = {
                                    editingMedication = medication
                                    editingRoutine = null
                                    sheetStep = MedicationSheetStep.FORM
                                    showSheet = true
                                },
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
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
                    val linkedSchedules = editingMedication?.let { medication ->
                        state.routines.filter { routine ->
                            routine.status == RoutineStatus.ACTIVE && medication.id in routine.medicationIds
                        }
                    }.orEmpty()

                    MedicationFormSheet(
                        medicationToEdit = editingMedication,
                        linkedSchedules = linkedSchedules,
                        onSave = { request, id ->
                            viewModel.saveMedication(request, id) { savedMedication ->
                                editingMedication = savedMedication
                                if (id != null) {
                                    showSheet = false
                                    sheetStep = MedicationSheetStep.FORM
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
                        onLinkExistingSchedules = editingMedication
                            ?.takeIf { it.status == MedicationStatus.ACTIVE }
                            ?.let {
                                {
                                    editingRoutine = null
                                    sheetStep = MedicationSheetStep.SCHEDULE_LINKS
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

                MedicationSheetStep.SCHEDULE_LINKS -> editingMedication?.let { medication ->
                    MedicationScheduleLinkSheet(
                        medication = medication,
                        routines = state.routines.filter { it.status == RoutineStatus.ACTIVE },
                        onBack = { sheetStep = MedicationSheetStep.FORM },
                        onCreateSchedule = {
                            editingRoutine = null
                            sheetStep = MedicationSheetStep.SCHEDULE_FORM
                        },
                        onOpenSchedulesScreen = {
                            showSheet = false
                            editingRoutine = null
                            sheetStep = MedicationSheetStep.FORM
                            onNavigateToRoutines()
                        },
                        onSaveLinks = { selectedRoutineIds ->
                            viewModel.updateRoutineLinks(medication.id, selectedRoutineIds)
                            sheetStep = MedicationSheetStep.FORM
                        },
                    )
                }

                MedicationSheetStep.SCHEDULE_FORM -> editingMedication?.let { medication ->
                    ScheduleFormSheet(
                        title = if (editingRoutine == null) "Add schedule" else "Edit schedule",
                        subtitle = if (editingRoutine == null) {
                            "This schedule will include ${medication.name}. You can add more medications if needed."
                        } else {
                            null
                        },
                        routineToEdit = editingRoutine,
                        medications = state.medications.filter { it.status == MedicationStatus.ACTIVE },
                        initialSelectedMedicationIds = setOf(medication.id),
                        onSave = { request, id ->
                            viewModel.saveRoutine(request, id) {
                                editingRoutine = null
                                sheetStep = MedicationSheetStep.FORM
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

@Composable
fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Badge { Text(count.toString()) }
    }
}

@Composable
fun EmptyStateMessage(msg: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MedicationEmptyState(onAddMedication: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("No medications yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Add your first medication to start tracking doses and schedules.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddMedication) {
                Text("Add medication")
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    linkedScheduleCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
) {
    val quantityLabel = medication.quantity.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(parseColorHex(medication.colorHex).copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(18.dp).background(parseColorHex(medication.colorHex), CircleShape))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(medication.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${medication.dosage}$quantityLabel", color = MaterialTheme.colorScheme.secondary)
                Text(
                    medicationScheduleLabel(linkedScheduleCount),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                medication.instruction?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.EditW400Outlinedfill1, contentDescription = "Edit medication")
        }
    }
}

private fun medicationScheduleLabel(linkedScheduleCount: Int): String = when (linkedScheduleCount) {
    0 -> "No schedules"
    1 -> "1 schedule"
    else -> "$linkedScheduleCount schedules"
}

@Composable
fun MedicationFormSheet(
    medicationToEdit: Medication?,
    linkedSchedules: List<Routine>,
    onSave: (MedicationCreateRequest, String?) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    onAddSchedule: (() -> Unit)? = null,
    onLinkExistingSchedules: (() -> Unit)? = null,
    onEditSchedule: (Routine) -> Unit = {},
    onRemoveSchedule: (Routine) -> Unit = {},
) {
    var name by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.name ?: "") }
    var dosage by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.dosage ?: "") }
    var quantity by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.quantity ?: "") }
    var instruction by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.instruction ?: "") }
    var frequency by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.frequency ?: MedicationFrequency.DAILY) }
    var status by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.status ?: MedicationStatus.ACTIVE) }
    var selectedColor by remember(medicationToEdit?.id) { mutableStateOf(parseColorHex(medicationToEdit?.colorHex)) }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (medicationToEdit == null) "Add medication" else "Edit medication",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (medicationToEdit != null) {
                IconButton(onClick = { onDelete(medicationToEdit.id) }) {
                    Icon(Icons.DeleteW400Outlined, contentDescription = "Delete medication", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Medication name") },
        )
        OutlinedTextField(
            value = dosage,
            onValueChange = { dosage = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dose") },
            placeholder = { Text("1 tablet") },
        )
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Strength") },
            placeholder = { Text("500 mg") },
        )
        OutlinedTextField(
            value = instruction,
            onValueChange = { instruction = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("Instructions") },
        )

        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Schedules", fontWeight = FontWeight.SemiBold)
                if (medicationToEdit == null) {
                    Text(
                        "Save this medication first. Then you can add schedules or keep it unscheduled and log it whenever you take it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    if (linkedSchedules.isEmpty()) {
                        Text("No schedules yet. You can still log this medication anytime from Calendar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        linkedSchedules.forEach { routine ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(routine.name, fontWeight = FontWeight.SemiBold)
                                    Text(scheduleLinkSummary(routine), color = MaterialTheme.colorScheme.secondary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { onEditSchedule(routine) }) {
                                            Text("Edit")
                                        }
                                        TextButton(onClick = { onRemoveSchedule(routine) }) {
                                            Text("Remove from medication")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onAddSchedule?.invoke() }, enabled = onAddSchedule != null) {
                            Text(if (linkedSchedules.isEmpty()) "Add schedule" else "Add another schedule")
                        }
                        OutlinedButton(onClick = { onLinkExistingSchedules?.invoke() }, enabled = onLinkExistingSchedules != null) {
                            Text("Link existing")
                        }
                    }
                }
            }
        }

        Text("Color tag", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Medication.PRESET_COLORS.forEach { swatch ->
                val color = parseColorHex(swatch)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (selectedColor == color) 3.dp else 0.dp,
                            color = if (selectedColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape,
                        )
                        .clickable { selectedColor = color },
                )
            }
        }

        if (medicationToEdit != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Archive medication", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Archived medications stay in the cabinet and disappear from active schedules.",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Switch(
                    checked = status == MedicationStatus.ARCHIVED,
                    onCheckedChange = { isArchived ->
                        status = if (isArchived) MedicationStatus.ARCHIVED else MedicationStatus.ACTIVE
                    },
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    MedicationCreateRequest(
                        name = name,
                        dosage = dosage,
                        quantity = quantity,
                        frequency = frequency.name,
                        instruction = instruction.takeIf(String::isNotBlank),
                        colorHex = selectedColor.toHexString(),
                        status = status.name,
                    ),
                    medicationToEdit?.id,
                )
            },
            enabled = name.isNotBlank() && dosage.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (medicationToEdit == null) "Save medication" else "Update medication")
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun MedicationScheduleLinkSheet(
    medication: Medication,
    routines: List<Routine>,
    onBack: () -> Unit,
    onCreateSchedule: () -> Unit,
    onOpenSchedulesScreen: () -> Unit,
    onSaveLinks: (Set<String>) -> Unit,
) {
    var selectedRoutineIds by remember(medication.id, routines) {
        mutableStateOf(routines.filter { medication.id in it.medicationIds }.map(Routine::id).toSet())
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Schedules for ${medication.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Choose which schedules should include this medication.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (routines.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("No schedules yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Create a schedule like Morning meds or Bedtime meds, then link this medication to it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onCreateSchedule) {
                        Text("Create schedule")
                    }
                }
            }
        } else {
            routines.forEach { routine ->
                val checked = routine.id in selectedRoutineIds
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedRoutineIds = selectedRoutineIds.toMutableSet().apply {
                            if (!add(routine.id)) remove(routine.id)
                        }
                    },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(routine.name, fontWeight = FontWeight.SemiBold)
                            Text(scheduleLinkSummary(routine), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        androidx.compose.material3.Checkbox(
                            checked = checked,
                            onCheckedChange = { shouldCheck ->
                                selectedRoutineIds = selectedRoutineIds.toMutableSet().apply {
                                    if (shouldCheck) add(routine.id) else remove(routine.id)
                                }
                            },
                        )
                    }
                }
            }
        }

        FilledTonalButton(onClick = onCreateSchedule, modifier = Modifier.fillMaxWidth()) {
            Text("Create schedule")
        }
        OutlinedButton(onClick = onOpenSchedulesScreen, modifier = Modifier.fillMaxWidth()) {
            Text("Open schedules screen")
        }
        Button(onClick = { onSaveLinks(selectedRoutineIds) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save schedule links")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
