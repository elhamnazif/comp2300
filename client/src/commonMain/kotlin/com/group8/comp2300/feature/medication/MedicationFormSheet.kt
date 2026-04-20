package com.group8.comp2300.feature.medication

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.accessibility.PatternSwatch
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.medical.shared.forms.MedicalFormTextField
import com.group8.comp2300.feature.medical.shared.routines.scheduleLinkSummary
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private enum class MedicationFormStep {
    BASICS,
    STOCK,
    REVIEW,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFormSheet(
    medicationToEdit: Medication?,
    linkedSchedules: List<Routine>,
    isMutating: Boolean,
    onSave: (MedicationCreateRequest, String?, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: () -> Unit,
    onAddSchedule: (() -> Unit)? = null,
    onEditSchedule: (Routine) -> Unit = {},
    onRemoveSchedule: (Routine) -> Unit = {},
) {
    var currentStep by remember(medicationToEdit?.id) { mutableStateOf(MedicationFormStep.BASICS) }
    var name by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.name ?: "") }
    var doseAmount by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.doseAmount ?: "") }
    var doseUnit by remember(medicationToEdit?.id) {
        mutableStateOf(medicationToEdit?.doseUnit ?: MedicationUnit.TABLET)
    }
    var customDoseUnit by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.customDoseUnit ?: "") }
    var stockAmount by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.stockAmount ?: "0") }
    var stockUnit by remember(medicationToEdit?.id) {
        mutableStateOf(medicationToEdit?.stockUnit ?: MedicationUnit.TABLET)
    }
    var customStockUnit by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.customStockUnit ?: "") }
    var instruction by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.instruction ?: "") }
    var status by remember(medicationToEdit?.id) { mutableStateOf(medicationToEdit?.status ?: MedicationStatus.ACTIVE) }
    var selectedColor by remember(medicationToEdit?.id) { mutableStateOf(parseColorHex(medicationToEdit?.colorHex)) }
    var showDeleteConfirmation by remember(medicationToEdit?.id) { mutableStateOf(false) }
    var scheduleToRemove by remember(medicationToEdit?.id) { mutableStateOf<Routine?>(null) }

    val canContinueBasics = name.isNotBlank() &&
        doseAmount.toDoubleOrNull()?.let { it > 0.0 } == true &&
        (doseUnit != MedicationUnit.OTHER || customDoseUnit.isNotBlank())
    val canContinueStock = stockAmount.toDoubleOrNull()?.let { it >= 0.0 } == true &&
        (stockUnit != MedicationUnit.OTHER || customStockUnit.isNotBlank())
    val canSave = canContinueBasics && canContinueStock
    val resolvedCustomDoseUnit = customDoseUnit.trim().takeIf(String::isNotBlank)
    val resolvedCustomUnit = customStockUnit.trim().takeIf(String::isNotBlank)
    val sheetTitle = when (currentStep) {
        MedicationFormStep.BASICS -> if (medicationToEdit == null) {
            stringResource(Res.string.medical_medication_form_add_title)
        } else {
            stringResource(Res.string.medical_medication_form_edit_title)
        }

        MedicationFormStep.STOCK -> stringResource(Res.string.medical_medication_form_stock_title)

        MedicationFormStep.REVIEW -> if (medicationToEdit == null) {
            stringResource(Res.string.medical_medication_form_summary_title)
        } else {
            stringResource(Res.string.medical_medication_form_edit_finish_title)
        }
    }

    fun save(addScheduleAfterSave: Boolean) {
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
            medicationToEdit?.id,
            addScheduleAfterSave,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = !isMutating,
                onClick = {
                    currentStep = when (currentStep) {
                        MedicationFormStep.BASICS -> {
                            onCancel()
                            MedicationFormStep.BASICS
                        }

                        MedicationFormStep.STOCK -> MedicationFormStep.BASICS

                        MedicationFormStep.REVIEW -> MedicationFormStep.STOCK
                    }
                },
            ) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (currentStep) {
                MedicationFormStep.BASICS -> {
                    MedicalFormTextField(
                        label = stringResource(Res.string.medical_medication_form_name_label),
                        value = name,
                        onValueChange = { name = it },
                    )
                    MedicationAmountRow(
                        amountLabel = stringResource(Res.string.medical_medication_form_dose_label),
                        amountValue = doseAmount,
                        onAmountChange = { doseAmount = it },
                        amountPlaceholder = stringResource(Res.string.medical_medication_form_dose_amount_placeholder),
                        selectedUnit = doseUnit,
                        unitLabel = stringResource(Res.string.medical_medication_form_dose_unit_label),
                        onUnitSelect = { doseUnit = it },
                    )
                    if (doseUnit == MedicationUnit.OTHER) {
                        MedicalFormTextField(
                            label = stringResource(Res.string.medical_medication_form_custom_dose_unit_label),
                            value = customDoseUnit,
                            onValueChange = { customDoseUnit = it },
                        )
                    }
                    MedicalFormTextField(
                        label = stringResource(Res.string.medical_medication_form_instructions_label),
                        value = instruction,
                        onValueChange = { instruction = it },
                        minLines = 2,
                    )
                    MedicationColorPicker(
                        selectedColor = selectedColor,
                        onColorSelected = { selectedColor = it },
                    )
                }

                MedicationFormStep.STOCK -> {
                    MedicationAmountRow(
                        amountLabel = stringResource(Res.string.medical_medication_form_stock_amount_label),
                        amountValue = stockAmount,
                        onAmountChange = { stockAmount = it },
                        amountPlaceholder = stringResource(Res.string.medical_medication_form_stock_amount_placeholder),
                        selectedUnit = stockUnit,
                        unitLabel = stringResource(Res.string.medical_medication_form_stock_unit_label),
                        onUnitSelect = { stockUnit = it },
                    )
                    if (stockUnit == MedicationUnit.OTHER) {
                        MedicalFormTextField(
                            label = stringResource(Res.string.medical_medication_form_custom_unit_label),
                            value = customStockUnit,
                            onValueChange = { customStockUnit = it },
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Text(
                            stringResource(Res.string.medical_medication_form_stock_amount_hint),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                MedicationFormStep.REVIEW -> {
                    if (medicationToEdit != null) {
                        Text(
                            stringResource(Res.string.medical_medication_form_edit_finish_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val colorOption = medicationColorOption(selectedColor.toHexString())
                    SummaryPanel(
                        name = name,
                        dosage = formatMedicationAmount(doseAmount, doseUnit, resolvedCustomDoseUnit),
                        supply = formatMedicationStock(stockAmount, stockUnit, resolvedCustomUnit),
                        instruction = instruction.takeIf(String::isNotBlank),
                        color = selectedColor,
                        colorOption = colorOption,
                    )

                    if (medicationToEdit != null) {
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
                                stringResource(Res.string.medical_medication_section_archived),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Switch(
                                checked = status == MedicationStatus.ARCHIVED,
                                enabled = !isMutating,
                                onCheckedChange = { isArchived ->
                                    status = if (isArchived) MedicationStatus.ARCHIVED else MedicationStatus.ACTIVE
                                },
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        MedicationFormActions(
            step = currentStep,
            isEditing = medicationToEdit != null,
            canContinueBasics = canContinueBasics,
            canContinueStock = canContinueStock,
            canSave = canSave,
            isMutating = isMutating,
            onContinueBasics = { currentStep = MedicationFormStep.STOCK },
            onContinueStock = { currentStep = MedicationFormStep.REVIEW },
            onSaveMedication = { save(addScheduleAfterSave = false) },
            onSaveAndAddSchedule = { save(addScheduleAfterSave = true) },
        )
    }

    if (showDeleteConfirmation && medicationToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.medical_medication_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.medical_medication_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(medicationToEdit.id)
                    },
                ) {
                    Text(stringResource(Res.string.medical_medication_delete_desc))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }

    scheduleToRemove?.let { routine ->
        AlertDialog(
            onDismissRequest = { scheduleToRemove = null },
            title = { Text(stringResource(Res.string.medical_medication_schedule_remove_confirm_title)) },
            text = { Text(stringResource(Res.string.medical_medication_schedule_remove_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scheduleToRemove = null
                        onRemoveSchedule(routine)
                    },
                ) {
                    Text(stringResource(Res.string.medical_medication_form_schedule_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToRemove = null }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            },
        )
    }
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
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 320.dp
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MedicalFormTextField(
                    label = amountLabel,
                    value = amountValue,
                    onValueChange = onAmountChange,
                    placeholder = amountPlaceholder,
                )
                MedicationUnitField(
                    selectedUnit = selectedUnit,
                    unitLabel = unitLabel,
                    onSelect = onUnitSelect,
                )
            }
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
                    placeholder = amountPlaceholder,
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

@Composable
private fun MedicationFormActions(
    step: MedicationFormStep,
    isEditing: Boolean,
    canContinueBasics: Boolean,
    canContinueStock: Boolean,
    canSave: Boolean,
    isMutating: Boolean,
    onContinueBasics: () -> Unit,
    onContinueStock: () -> Unit,
    onSaveMedication: () -> Unit,
    onSaveAndAddSchedule: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (step) {
            MedicationFormStep.BASICS -> {
                Button(
                    onClick = onContinueBasics,
                    enabled = canContinueBasics && !isMutating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.medical_medication_form_continue))
                }
            }

            MedicationFormStep.STOCK -> {
                Button(
                    onClick = onContinueStock,
                    enabled = canContinueStock && !isMutating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.medical_medication_form_continue))
                }
            }

            MedicationFormStep.REVIEW -> {
                if (isEditing) {
                    Button(
                        onClick = onSaveMedication,
                        enabled = canSave && !isMutating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.medical_medication_form_update_medication))
                    }
                } else {
                    Button(
                        onClick = onSaveAndAddSchedule,
                        enabled = canSave && !isMutating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.medical_medication_form_save_add_schedule))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            onClick = onSaveMedication,
                            enabled = canSave && !isMutating,
                        ) {
                            Text(stringResource(Res.string.medical_medication_form_save_medication))
                        }
                    }
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
private fun SummaryPanel(
    name: String,
    dosage: String,
    supply: String,
    instruction: String?,
    color: Color,
    colorOption: MedicationColorOption,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.18f), CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PatternSwatch(
                        color = color,
                        pattern = colorOption.pattern,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(Res.string.medical_medication_form_name_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryMetric(
                    label = stringResource(Res.string.medical_medication_form_dose_label),
                    value = dosage,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                )
                SummaryMetric(
                    label = stringResource(Res.string.medical_medication_form_supply_label),
                    value = supply,
                    modifier = Modifier.weight(1f),
                )
            }
            instruction?.let {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                SummaryMetric(
                    label = stringResource(Res.string.medical_medication_form_instructions_label),
                    value = it,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    Row {
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
