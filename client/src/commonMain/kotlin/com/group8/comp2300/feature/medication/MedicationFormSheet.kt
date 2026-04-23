package com.group8.comp2300.feature.medication

import androidx.compose.foundation.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.accessibility.PatternSwatch
import com.group8.comp2300.core.ui.components.ConfirmActionDialog
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.medical.shared.forms.MedicalFormTextField
import com.group8.comp2300.feature.medical.shared.routines.*
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFormSheet(
    medicationToEdit: Medication?,
    linkedSchedules: List<Routine>,
    isMutating: Boolean,
    onSave: (MedicationCreateRequest, RoutineCreateRequest?) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    onAddSchedule: (() -> Unit)? = null,
    onEditSchedule: (Routine) -> Unit = {},
    onRemoveSchedule: (Routine) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val nameFocusRequester = remember { FocusRequester() }
    val doseAmountFocusRequester = remember { FocusRequester() }
    val customDoseUnitFocusRequester = remember { FocusRequester() }
    val stockAmountFocusRequester = remember { FocusRequester() }
    val customStockUnitFocusRequester = remember { FocusRequester() }
    val instructionFocusRequester = remember { FocusRequester() }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var name by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.name ?: "") }
    var doseAmount by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.doseAmount ?: "") }
    var doseUnit by remember(medicationToEdit?.id) {
        mutableStateOf(medicationToEdit?.doseUnit ?: MedicationUnit.TABLET)
    }
    var customDoseUnit by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.customDoseUnit ?: "") }
    var stockAmount by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.stockAmount ?: "") }
    var stockUnit by remember(medicationToEdit?.id) {
        mutableStateOf(medicationToEdit?.stockUnit ?: MedicationUnit.TABLET)
    }
    var customStockUnit by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.customStockUnit ?: "") }
    var instruction by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.instruction ?: "") }
    var status by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.status ?: MedicationStatus.ACTIVE) }
    var selectedColor by remember(medicationToEdit?.id) { mutableStateOf(parseColorHex(medicationToEdit?.colorHex)) }
    var addScheduleNow by remember(medicationToEdit?.id) { mutableStateOf(false) }
    var scheduleDraft by remember(medicationToEdit?.id) { mutableStateOf(defaultRoutineDraft(today = today)) }
    var showDeleteConfirmation by remember(medicationToEdit?.id) { mutableStateOf(false) }
    var scheduleToRemove by remember(medicationToEdit?.id) { mutableStateOf<Routine?>(null) }

    val canSaveMedication = name.isNotBlank() &&
        doseAmount.toDoubleOrNull()?.let { it > 0.0 } == true &&
        stockAmount.toDoubleOrNull()?.let { it >= 0.0 } == true &&
        (doseUnit != MedicationUnit.OTHER || customDoseUnit.isNotBlank()) &&
        (stockUnit != MedicationUnit.OTHER || customStockUnit.isNotBlank())
    val canSaveSchedule = !addScheduleNow || canSaveRoutineDraft(
        draft = scheduleDraft,
        requireName = false,
        requireMedicationSelection = false,
    )
    val canSave = canSaveMedication && canSaveSchedule
    val resolvedCustomDoseUnit = customDoseUnit.trim().takeIf(String::isNotBlank)
    val resolvedCustomUnit = customStockUnit.trim().takeIf(String::isNotBlank)
    val sheetTitle = if (medicationToEdit == null) {
        stringResource(Res.string.medical_medication_form_add_title)
    } else {
        stringResource(Res.string.medical_medication_form_edit_title)
    }
    val defaultScheduleName = stringResource(Res.string.medical_medication_form_schedule_default_name)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(enabled = !isMutating, onClick = onCancel) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.common_back_desc),
                )
            }
            Text(
                sheetTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (medicationToEdit != null) {
                IconButton(
                    enabled = !isMutating,
                    onClick = { showDeleteConfirmation = true },
                ) {
                    Icon(
                        Icons.DeleteW400Outlined,
                        contentDescription = stringResource(Res.string.medical_medication_delete_desc),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MedicalFormTextField(
                label = stringResource(Res.string.medical_medication_form_name_label),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(Res.string.medical_medication_form_name_placeholder),
                textFieldModifier = Modifier.focusRequester(nameFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { doseAmountFocusRequester.requestFocus() }),
            )
            MedicationAmountRow(
                amountLabel = stringResource(Res.string.medical_medication_form_dose_label),
                amountValue = doseAmount,
                onAmountChange = { doseAmount = it },
                amountPlaceholder = stringResource(Res.string.medical_medication_form_dose_amount_placeholder),
                selectedUnit = doseUnit,
                unitLabel = stringResource(Res.string.medical_medication_form_dose_unit_label),
                onUnitSelect = { doseUnit = it },
                amountTextFieldModifier = Modifier.focusRequester(doseAmountFocusRequester),
                amountKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                amountKeyboardActions = KeyboardActions(
                    onNext = {
                        if (doseUnit == MedicationUnit.OTHER) {
                            customDoseUnitFocusRequester.requestFocus()
                        } else {
                            stockAmountFocusRequester.requestFocus()
                        }
                    },
                ),
            )
            OtherUnitField(
                visible = doseUnit == MedicationUnit.OTHER,
                label = stringResource(Res.string.medical_medication_form_custom_dose_unit_label),
                value = customDoseUnit,
                onValueChange = { customDoseUnit = it },
                textFieldModifier = Modifier.focusRequester(customDoseUnitFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { stockAmountFocusRequester.requestFocus() }),
            )
            MedicationAmountRow(
                amountLabel = stringResource(Res.string.medical_medication_form_stock_amount_label),
                amountValue = stockAmount,
                onAmountChange = { stockAmount = it },
                amountPlaceholder = stringResource(Res.string.medical_medication_form_stock_amount_placeholder),
                selectedUnit = stockUnit,
                unitLabel = stringResource(Res.string.medical_medication_form_stock_unit_label),
                onUnitSelect = { stockUnit = it },
                amountTextFieldModifier = Modifier.focusRequester(stockAmountFocusRequester),
                amountKeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                amountKeyboardActions = KeyboardActions(
                    onNext = {
                        if (stockUnit == MedicationUnit.OTHER) {
                            customStockUnitFocusRequester.requestFocus()
                        } else {
                            instructionFocusRequester.requestFocus()
                        }
                    },
                ),
            )
            OtherUnitField(
                visible = stockUnit == MedicationUnit.OTHER,
                label = stringResource(Res.string.medical_medication_form_custom_unit_label),
                value = customStockUnit,
                onValueChange = { customStockUnit = it },
                textFieldModifier = Modifier.focusRequester(customStockUnitFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { instructionFocusRequester.requestFocus() }),
            )
            MedicalFormTextField(
                label = stringResource(Res.string.medical_medication_form_instructions_label),
                value = instruction,
                onValueChange = { instruction = it },
                minLines = 2,
                placeholder = stringResource(Res.string.medical_medication_form_instructions_placeholder),
                textFieldModifier = Modifier.focusRequester(instructionFocusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            MedicationColorPicker(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
            )

            if (medicationToEdit == null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(Res.string.medical_medication_form_schedule_toggle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = addScheduleNow,
                        enabled = !isMutating,
                        onCheckedChange = { addScheduleNow = it },
                    )
                }
                if (addScheduleNow) {
                    RoutineFieldsSection(
                        draft = scheduleDraft,
                        onDraftChange = { scheduleDraft = it },
                        medications = emptyList(),
                        isMutating = isMutating,
                        showNameField = false,
                        showMedicationSection = false,
                        showArchiveToggle = false,
                    )
                }
            } else {
                HorizontalDivider()
                ScheduleSection(
                    linkedSchedules = linkedSchedules,
                    isMutating = isMutating,
                    onAddSchedule = onAddSchedule,
                    onEditSchedule = onEditSchedule,
                    onRemoveSchedule = { routine -> scheduleToRemove = routine },
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(Res.string.medical_medication_form_archive_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = status == MedicationStatus.ARCHIVED,
                        enabled = !isMutating,
                        onCheckedChange = { archived ->
                            status = if (archived) MedicationStatus.ARCHIVED else MedicationStatus.ACTIVE
                        },
                    )
                }
            }
        }

        HorizontalDivider()
        Button(
            onClick = {
                onSave(
                    MedicationCreateRequest(
                        name = name,
                        doseAmount = doseAmount,
                        doseUnit = doseUnit.name,
                        customDoseUnit = resolvedCustomDoseUnit.takeIf { doseUnit == MedicationUnit.OTHER },
                        stockAmount = stockAmount,
                        stockUnit = stockUnit.name,
                        customStockUnit = resolvedCustomUnit.takeIf { stockUnit == MedicationUnit.OTHER },
                        instruction = instruction.takeIf(String::isNotBlank),
                        colorHex = selectedColor.toHexString(),
                        status = status.name,
                    ),
                    if (addScheduleNow) {
                        scheduleDraft.toRequest(
                            nameOverride = name.trim().ifBlank { defaultScheduleName },
                            medicationIdsOverride = emptyList(),
                        )
                    } else {
                        null
                    },
                )
            },
            enabled = canSave && !isMutating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    medicationToEdit != null -> stringResource(Res.string.medical_medication_form_update_medication)
                    addScheduleNow -> stringResource(Res.string.medical_medication_form_save_with_schedule)
                    else -> stringResource(Res.string.medical_medication_form_save_medication)
                },
            )
        }
    }

    if (showDeleteConfirmation && medicationToEdit != null) {
        ConfirmActionDialog(
            title = stringResource(Res.string.medical_medication_delete_confirm_title),
            message = stringResource(Res.string.medical_medication_delete_confirm_message),
            confirmLabel = stringResource(Res.string.medical_medication_delete_desc),
            onConfirm = {
                showDeleteConfirmation = false
                onDelete(medicationToEdit.id)
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }

    scheduleToRemove?.let { routine ->
        ConfirmActionDialog(
            title = stringResource(Res.string.medical_medication_schedule_remove_confirm_title),
            message = stringResource(Res.string.medical_medication_schedule_remove_confirm_message),
            confirmLabel = stringResource(Res.string.medical_medication_form_schedule_remove),
            onConfirm = {
                scheduleToRemove = null
                onRemoveSchedule(routine)
            },
            onDismiss = { scheduleToRemove = null },
        )
    }
}

@Composable
private fun OtherUnitField(
    visible: Boolean,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textFieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    if (!visible) return
    MedicalFormTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        textFieldModifier = textFieldModifier,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun MedicationAmountRow(
    amountLabel: String,
    amountValue: String,
    onAmountChange: (String) -> Unit,
    amountPlaceholder: String,
    selectedUnit: MedicationUnit,
    unitLabel: String,
    onUnitSelect: (MedicationUnit) -> Unit,
    amountTextFieldModifier: Modifier = Modifier,
    amountKeyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    amountKeyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 320.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MedicalFormTextField(
                    label = amountLabel,
                    value = amountValue,
                    onValueChange = onAmountChange,
                    placeholder = amountPlaceholder.takeIf(String::isNotBlank),
                    textFieldModifier = amountTextFieldModifier,
                    keyboardOptions = amountKeyboardOptions,
                    keyboardActions = amountKeyboardActions,
                )
                if (unitLabel.isNotBlank()) {
                    MedicationUnitField(
                        selectedUnit = selectedUnit,
                        unitLabel = unitLabel,
                        onSelect = onUnitSelect,
                    )
                }
            }
        } else {
            if (unitLabel.isBlank()) {
                MedicalFormTextField(
                    label = amountLabel,
                    value = amountValue,
                    onValueChange = onAmountChange,
                    placeholder = amountPlaceholder.takeIf(String::isNotBlank),
                    textFieldModifier = amountTextFieldModifier,
                    keyboardOptions = amountKeyboardOptions,
                    keyboardActions = amountKeyboardActions,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    MedicalFormTextField(
                        label = amountLabel,
                        value = amountValue,
                        onValueChange = onAmountChange,
                        modifier = Modifier.weight(1.15f),
                        placeholder = amountPlaceholder.takeIf(String::isNotBlank),
                        textFieldModifier = amountTextFieldModifier,
                        keyboardOptions = amountKeyboardOptions,
                        keyboardActions = amountKeyboardActions,
                    )
                    MedicationUnitField(
                        selectedUnit = selectedUnit,
                        unitLabel = unitLabel,
                        onSelect = onUnitSelect,
                        modifier = Modifier.weight(0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicationColorPicker(selectedColor: Color, onColorSelected: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.medical_medication_form_color_tag_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            medicationColorOptions.forEach { option ->
                val color = parseColorHex(option.hex)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .border(
                            width = if (selectedColor == color) 2.dp else 0.dp,
                            color = if (selectedColor ==
                                color
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape,
                        )
                        .padding(4.dp)
                        .clickable { onColorSelected(color) },
                ) {
                    PatternSwatch(
                        color = color,
                        pattern = option.pattern,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationUnitField(
    selectedUnit: MedicationUnit,
    unitLabel: String,
    onSelect: (MedicationUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(selectedUnit) { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            unitLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextField(
                value = selectedUnit.displayName,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                ),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                MedicationUnit.entries.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit.displayName) },
                        onClick = {
                            onSelect(unit)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleSection(
    linkedSchedules: List<Routine>,
    isMutating: Boolean,
    onAddSchedule: (() -> Unit)?,
    onEditSchedule: (Routine) -> Unit,
    onRemoveSchedule: (Routine) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.medical_medication_form_schedule_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onAddSchedule?.invoke() }, enabled = onAddSchedule != null && !isMutating) {
                Text(stringResource(Res.string.medical_medication_form_schedule_add))
            }
        }
        if (linkedSchedules.isEmpty()) {
            Text(
                stringResource(Res.string.medical_medication_form_schedule_none),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            linkedSchedules.forEachIndexed { index, routine ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(routine.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        scheduleLinkSummary(routine),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onEditSchedule(routine) }, enabled = !isMutating) {
                            Text(stringResource(Res.string.medical_medication_edit_desc))
                        }
                        TextButton(onClick = { onRemoveSchedule(routine) }, enabled = !isMutating) {
                            Text(stringResource(Res.string.medical_medication_form_schedule_remove))
                        }
                    }
                }
                if (index != linkedSchedules.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

internal fun parseColorHex(hex: String?): Color = try {
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
