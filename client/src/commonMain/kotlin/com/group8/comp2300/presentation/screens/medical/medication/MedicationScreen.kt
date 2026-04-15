package com.group8.comp2300.presentation.screens.medical.medication

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
import com.group8.comp2300.presentation.accessibility.IndicatorPattern
import com.group8.comp2300.presentation.accessibility.PatternSwatch
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.presentation.notifications.NotificationPermissionResult
import com.group8.comp2300.presentation.notifications.rememberNotificationPermissionRequester
import com.group8.comp2300.presentation.screens.medical.components.EmptyStateMessage
import com.group8.comp2300.presentation.screens.medical.components.MedicalFormTextField
import com.group8.comp2300.presentation.screens.medical.components.ScheduleFormSheet
import com.group8.comp2300.presentation.screens.medical.components.SectionHeader
import com.group8.comp2300.presentation.screens.medical.components.formatTimeOfDayMs
import com.group8.comp2300.presentation.screens.medical.components.scheduleLinkSummary
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EditW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private enum class MedicationSheetStep {
    FORM,
    SCHEDULE_FORM,
}

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

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    val activeMedications = state.medications.filter { it.status == MedicationStatus.ACTIVE }
    val archivedMedications = state.medications.filter { it.status == MedicationStatus.ARCHIVED }

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
                Icon(Icons.AddW400Outlinedfill1, contentDescription = stringResource(Res.string.medical_medication_add_desc))
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
                        item { SectionHeader(stringResource(Res.string.medical_medication_section_active), activeMedications.size) }
                        items(activeMedications, key = Medication::id) { medication ->
                            MedicationCard(
                                medication = medication,
                                linkedRoutines = linkedRoutinesFor(medication),
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
                        item { EmptyStateMessage(stringResource(Res.string.medical_medication_empty_active)) }
                    }

                    if (archivedMedications.isNotEmpty()) {
                        item { SectionHeader(stringResource(Res.string.medical_medication_section_archived), archivedMedications.size) }
                        items(archivedMedications, key = Medication::id) { medication ->
                            MedicationCard(
                                medication = medication,
                                linkedRoutines = linkedRoutinesFor(medication),
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
            Text(
                stringResource(Res.string.medical_medication_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.medical_medication_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddMedication) {
                Text(stringResource(Res.string.medical_medication_empty_add_button))
            }
        }
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    linkedRoutines: List<Routine>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
) {
    val quantityLabel = medication.quantity.takeIf(String::isNotBlank)?.let { " • $it" }.orEmpty()
    val colorOption = medicationColorOption(medication.colorHex)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
            if (isArchived) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(parseColorHex(medication.colorHex).copy(alpha = 0.18f), CircleShape)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PatternSwatch(
                        color = parseColorHex(medication.colorHex),
                        pattern = colorOption.pattern,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    colorOption.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(medication.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${medication.dosage}$quantityLabel", color = MaterialTheme.colorScheme.secondary)
                Text(medicationScheduleLabel(linkedRoutines), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                medication.instruction?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.EditW400Outlinedfill1, contentDescription = stringResource(Res.string.medical_medication_edit_desc))
        }
    }
}

@Composable
private fun medicationScheduleLabel(linkedRoutines: List<Routine>): String {
    val weekdayLabels = mapOf(
        0 to stringResource(Res.string.common_day_sun_short),
        1 to stringResource(Res.string.common_day_mon_short),
        2 to stringResource(Res.string.common_day_tue_short),
        3 to stringResource(Res.string.common_day_wed_short),
        4 to stringResource(Res.string.common_day_thu_short),
        5 to stringResource(Res.string.common_day_fri_short),
        6 to stringResource(Res.string.common_day_sat_short),
    )

    if (linkedRoutines.isEmpty()) return stringResource(Res.string.medical_medication_no_schedule)
    if (linkedRoutines.size > 1) {
        return stringResource(Res.string.medical_medication_schedule_count, linkedRoutines.size)
    }

    val routine = linkedRoutines.single()
    val times = routine.timesOfDayMs.sorted().distinct()
    val timeLabel = when (times.size) {
        0 -> ""
        1 -> stringResource(Res.string.medical_medication_schedule_at, formatTimeOfDayMs(times.single()))
        else -> stringResource(Res.string.medical_medication_schedule_times, times.size)
    }

    return when (routine.repeatType) {
        RoutineRepeatType.DAILY -> when (times.size) {
            1 -> stringResource(Res.string.medical_medication_schedule_daily_at, formatTimeOfDayMs(times.single()))
            else -> if (timeLabel.isBlank()) {
                stringResource(Res.string.medical_routine_repeat_daily)
            } else {
                stringResource(Res.string.medical_medication_schedule_daily_multi, times.size)
            }
        }

        RoutineRepeatType.WEEKLY -> {
            val days = routine.daysOfWeek.sorted().joinToString { day -> weekdayLabels[day].orEmpty() }
            if (days.isBlank()) {
                stringResource(Res.string.medical_medication_schedule_weekly_default, timeLabel)
            } else {
                "$days$timeLabel"
            }
        }
    }
}

@Composable
fun MedicationFormSheet(
    medicationToEdit: Medication?,
    linkedSchedules: List<Routine>,
    onSave: (MedicationCreateRequest, String?, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    onAddSchedule: (() -> Unit)? = null,
    onEditSchedule: (Routine) -> Unit = {},
    onRemoveSchedule: (Routine) -> Unit = {},
) {
    var name by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.name ?: "") }
    var dosage by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.dosage ?: "") }
    var quantity by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.quantity ?: "") }
    var instruction by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.instruction ?: "") }
    val frequency = medicationToEdit?.frequency ?: MedicationFrequency.DAILY
    var status by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.status ?: MedicationStatus.ACTIVE) }
    var selectedColor by remember(medicationToEdit?.id) { mutableStateOf(parseColorHex(medicationToEdit?.colorHex)) }
    val canSave = name.isNotBlank() && dosage.isNotBlank()

    fun save(addScheduleAfterSave: Boolean) {
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
            addScheduleAfterSave,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (medicationToEdit == null) {
                    stringResource(Res.string.medical_medication_form_add_title)
                } else {
                    stringResource(Res.string.medical_medication_form_edit_title)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (medicationToEdit != null) {
                IconButton(onClick = { onDelete(medicationToEdit.id) }) {
                    Icon(
                        Icons.DeleteW400Outlined,
                        contentDescription = stringResource(Res.string.medical_medication_delete_desc),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        MedicalFormTextField(label = stringResource(Res.string.medical_medication_form_name_label), value = name, onValueChange = { name = it })
        MedicalFormTextField(
            label = stringResource(Res.string.medical_medication_form_dose_label),
            value = dosage,
            onValueChange = { dosage = it },
            placeholder = stringResource(Res.string.medical_medication_form_dose_placeholder),
        )
        MedicalFormTextField(
            label = stringResource(Res.string.medical_medication_form_strength_label),
            value = quantity,
            onValueChange = { quantity = it },
            placeholder = stringResource(Res.string.medical_medication_form_strength_placeholder),
        )
        MedicalFormTextField(
            label = stringResource(Res.string.medical_medication_form_instructions_label),
            value = instruction,
            onValueChange = { instruction = it },
            minLines = 2,
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(Res.string.medical_medication_form_color_tag_label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                medicationColorOptions.forEach { option ->
                    val color = parseColorHex(option.hex)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                                .border(
                                    width = if (selectedColor == color) 2.dp else 0.dp,
                                    color = if (selectedColor == color) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape,
                                )
                                .padding(4.dp)
                                .clickable { selectedColor = color },
                        ) {
                            PatternSwatch(
                                color = color,
                                pattern = option.pattern,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Text(
                            option.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                stringResource(Res.string.medical_medication_form_selected_color, medicationColorOption(selectedColor.toHexString()).label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (medicationToEdit != null) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.medical_medication_form_schedule_section), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { onAddSchedule?.invoke() }, enabled = onAddSchedule != null) {
                    Text(stringResource(Res.string.medical_medication_form_schedule_add))
                }
            }
            if (linkedSchedules.isEmpty()) {
                Text(stringResource(Res.string.medical_medication_form_schedule_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                linkedSchedules.forEachIndexed { index, routine ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(routine.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                scheduleLinkSummary(routine),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onEditSchedule(routine) }) {
                                Text(stringResource(Res.string.medical_medication_edit_desc))
                            }
                            TextButton(onClick = { onRemoveSchedule(routine) }) {
                                Text(stringResource(Res.string.medical_medication_form_schedule_remove))
                            }
                        }
                    }
                    if (index != linkedSchedules.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.medical_medication_section_archived), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = status == MedicationStatus.ARCHIVED,
                    onCheckedChange = { isArchived ->
                        status = if (isArchived) MedicationStatus.ARCHIVED else MedicationStatus.ACTIVE
                    },
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        if (medicationToEdit == null) {
            Button(
                onClick = { save(addScheduleAfterSave = true) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.medical_medication_form_save_add_schedule))
            }
            OutlinedButton(
                onClick = { save(addScheduleAfterSave = false) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.medical_medication_form_save_medication))
            }
        } else {
            Button(
                onClick = { save(addScheduleAfterSave = false) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.medical_medication_form_update_medication))
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.common_cancel))
        }
    }
}

private data class MedicationColorOption(
    val hex: String,
    val label: String,
    val shortLabel: String,
    val pattern: IndicatorPattern,
)

private val medicationColorOptions = listOf(
    MedicationColorOption("#42A5F5", "Blue tag", "Blue", IndicatorPattern.DIAGONAL),
    MedicationColorOption("#EF5350", "Red tag", "Red", IndicatorPattern.GRID),
    MedicationColorOption("#66BB6A", "Green tag", "Green", IndicatorPattern.DOTS),
    MedicationColorOption("#FFA726", "Orange tag", "Orange", IndicatorPattern.HORIZONTAL),
    MedicationColorOption("#AB47BC", "Purple tag", "Purple", IndicatorPattern.VERTICAL),
    MedicationColorOption("#26C6DA", "Cyan tag", "Cyan", IndicatorPattern.DIAGONAL),
    MedicationColorOption("#78909C", "Blue grey tag", "Grey", IndicatorPattern.GRID),
)

private fun medicationColorOption(hex: String?): MedicationColorOption {
    val normalized = normalizeColorHex(hex)
    return medicationColorOptions.firstOrNull { it.hex == normalized } ?: medicationColorOptions.first()
}

private fun normalizeColorHex(hex: String?): String = "#${(hex ?: Medication.PRESET_COLORS.first()).removePrefix("#").uppercase()}"

private fun parseColorHex(hex: String?): Color = try {
    val raw = normalizeColorHex(hex).removePrefix("#")
    Color((raw.toLong(16) or 0xFF000000L).toInt())
} catch (_: Exception) {
    Color(0xFF42A5F5)
}

private fun Color.toHexString(): String {
    val red = (this.red * 255).toInt().coerceIn(0, 255)
    val green = (this.green * 255).toInt().coerceIn(0, 255)
    val blue = (this.blue * 255).toInt().coerceIn(0, 255)
    return "#${red.hex2()}${green.hex2()}${blue.hex2()}"
}

private fun Int.hex2(): String = toString(16).uppercase().padStart(2, '0')
