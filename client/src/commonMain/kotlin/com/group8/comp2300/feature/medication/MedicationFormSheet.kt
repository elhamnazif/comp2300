package com.group8.comp2300.feature.medication

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.accessibility.PatternSwatch
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.feature.medical.shared.forms.MedicalFormTextField
import com.group8.comp2300.feature.medical.shared.routines.scheduleLinkSummary
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.common_cancel
import comp2300.i18n.generated.resources.medical_medication_delete_desc
import comp2300.i18n.generated.resources.medical_medication_edit_desc
import comp2300.i18n.generated.resources.medical_medication_form_add_title
import comp2300.i18n.generated.resources.medical_medication_form_color_tag_label
import comp2300.i18n.generated.resources.medical_medication_form_dose_label
import comp2300.i18n.generated.resources.medical_medication_form_dose_placeholder
import comp2300.i18n.generated.resources.medical_medication_form_edit_title
import comp2300.i18n.generated.resources.medical_medication_form_instructions_label
import comp2300.i18n.generated.resources.medical_medication_form_name_label
import comp2300.i18n.generated.resources.medical_medication_form_save_add_schedule
import comp2300.i18n.generated.resources.medical_medication_form_save_medication
import comp2300.i18n.generated.resources.medical_medication_form_schedule_add
import comp2300.i18n.generated.resources.medical_medication_form_schedule_none
import comp2300.i18n.generated.resources.medical_medication_form_schedule_remove
import comp2300.i18n.generated.resources.medical_medication_form_schedule_section
import comp2300.i18n.generated.resources.medical_medication_form_selected_color
import comp2300.i18n.generated.resources.medical_medication_form_strength_label
import comp2300.i18n.generated.resources.medical_medication_form_strength_placeholder
import comp2300.i18n.generated.resources.medical_medication_form_update_medication
import comp2300.i18n.generated.resources.medical_medication_section_archived
import org.jetbrains.compose.resources.stringResource

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
        modifier = Modifier.fillMaxWidth().padding(24.dp),
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

        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MedicalFormTextField(
                label = stringResource(
                    Res.string.medical_medication_form_name_label,
                ),
                value = name,
                onValueChange = {
                    name =
                        it
                },
            )
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
                    stringResource(
                        Res.string.medical_medication_form_selected_color,
                        medicationColorOption(selectedColor.toHexString()).label,
                    ),
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
                    Text(
                        stringResource(Res.string.medical_medication_form_schedule_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { onAddSchedule?.invoke() },
                        enabled =
                        onAddSchedule != null,
                    ) {
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
                                androidx.compose.material3.TextButton(onClick = { onEditSchedule(routine) }) {
                                    Text(stringResource(Res.string.medical_medication_edit_desc))
                                }
                                androidx.compose.material3.TextButton(onClick = { onRemoveSchedule(routine) }) {
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
                    Text(
                        stringResource(Res.string.medical_medication_section_archived),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = status == MedicationStatus.ARCHIVED,
                        onCheckedChange = { isArchived ->
                            status = if (isArchived) MedicationStatus.ARCHIVED else MedicationStatus.ACTIVE
                        },
                    )
                }
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

        androidx.compose.material3.TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.common_cancel))
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
