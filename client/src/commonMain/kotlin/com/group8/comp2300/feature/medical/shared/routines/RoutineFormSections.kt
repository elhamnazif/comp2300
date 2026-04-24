package com.group8.comp2300.feature.medical.shared.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.components.DatePickerSheet
import com.group8.comp2300.core.ui.components.DateValueField
import com.group8.comp2300.core.ui.components.TimePickerSheet
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.medical.shared.forms.MedicalFormTextField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CloseW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

internal val scheduleReminderOffsets = listOf(0, 5, 10, 15, 30, 60)
private const val DefaultRoutineTimeMs = 9 * 60 * 60 * 1000L

data class RoutineDraft(
    val name: String = "",
    val timesOfDayMs: List<Long> = listOf(DefaultRoutineTimeMs),
    val repeatType: RoutineRepeatType = RoutineRepeatType.DAILY,
    val daysOfWeek: Set<Int> = emptySet(),
    val startDate: String,
    val endDate: String? = null,
    val hasReminder: Boolean = false,
    val reminderOffsetsMins: Set<Int> = emptySet(),
    val medicationIds: Set<String> = emptySet(),
    val status: RoutineStatus = RoutineStatus.ACTIVE,
)

fun defaultRoutineDraft(today: LocalDate, medicationIds: Set<String> = emptySet(), name: String = ""): RoutineDraft =
    RoutineDraft(
        name = name,
        startDate = today.toString(),
        medicationIds = medicationIds,
    )

fun Routine.toDraft(): RoutineDraft = RoutineDraft(
    name = name,
    timesOfDayMs = timesOfDayMs.sorted().distinct().ifEmpty { listOf(DefaultRoutineTimeMs) },
    repeatType = repeatType,
    daysOfWeek = daysOfWeek.toSet(),
    startDate = startDate,
    endDate = endDate,
    hasReminder = hasReminder,
    reminderOffsetsMins = reminderOffsetsMins.toSet(),
    medicationIds = medicationIds.toSet(),
    status = status,
)

fun canSaveRoutineDraft(draft: RoutineDraft, requireName: Boolean, requireMedicationSelection: Boolean): Boolean {
    val hasName = !requireName || draft.name.isNotBlank()
    val hasTimes = draft.timesOfDayMs.isNotEmpty()
    val hasDays = draft.repeatType != RoutineRepeatType.WEEKLY || draft.daysOfWeek.isNotEmpty()
    val hasMedications = !requireMedicationSelection || draft.medicationIds.isNotEmpty()
    val hasValidEndDate = draft.endDate == null || LocalDate.parse(draft.endDate) >= LocalDate.parse(draft.startDate)
    return hasName && hasTimes && hasDays && hasMedications && hasValidEndDate
}

fun RoutineDraft.toRequest(
    nameOverride: String? = null,
    medicationIdsOverride: List<String>? = null,
): RoutineCreateRequest = RoutineCreateRequest(
    name = nameOverride?.trim()?.takeIf(String::isNotBlank) ?: name.trim(),
    timesOfDayMs = timesOfDayMs.sorted().distinct(),
    repeatType = repeatType.name,
    daysOfWeek = if (repeatType == RoutineRepeatType.WEEKLY) daysOfWeek.sorted() else emptyList(),
    startDate = startDate,
    endDate = endDate,
    hasReminder = hasReminder,
    reminderOffsetsMins = if (hasReminder) reminderOffsetsMins.sorted() else emptyList(),
    status = status.name,
    medicationIds = medicationIdsOverride ?: medicationIds.sorted(),
)

@Composable
fun RoutineFieldsSection(
    draft: RoutineDraft,
    onDraftChange: (RoutineDraft) -> Unit,
    medications: List<Medication>,
    isMutating: Boolean,
    showNameField: Boolean,
    showMedicationSection: Boolean,
    showArchiveToggle: Boolean,
    modifier: Modifier = Modifier,
    nameTextFieldModifier: Modifier = Modifier,
    startDateModifier: Modifier = Modifier,
    nameKeyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    nameKeyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var activeDatePicker by remember { mutableStateOf<String?>(null) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMedicationPicker by remember(showMedicationSection, draft.medicationIds) {
        mutableStateOf(showMedicationSection && draft.medicationIds.isEmpty())
    }
    val invalidEndDate = draft.endDate != null && LocalDate.parse(draft.endDate) < LocalDate.parse(draft.startDate)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (showNameField) {
            MedicalFormTextField(
                label = stringResource(Res.string.medical_routine_form_name_label),
                value = draft.name,
                onValueChange = { onDraftChange(draft.copy(name = it)) },
                placeholder = stringResource(Res.string.medical_routine_form_name_placeholder),
                textFieldModifier = nameTextFieldModifier,
                keyboardOptions = nameKeyboardOptions,
                keyboardActions = nameKeyboardActions,
            )
        }

        RoutineSectionHeading(stringResource(Res.string.medical_routine_form_dose_times))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            draft.timesOfDayMs.sorted().distinct().forEachIndexed { index, timeOfDayMs ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = true,
                            enabled = !isMutating,
                            onClick = {
                                editingTimeIndex = index
                                showTimePicker = true
                            },
                            label = { Text(formatTimeOfDayMs(timeOfDayMs)) },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                onDraftChange(
                                    draft.copy(
                                        timesOfDayMs = draft.timesOfDayMs.toMutableList().apply {
                                            removeAt(index)
                                        }.sorted(),
                                    ),
                                )
                            },
                            enabled = draft.timesOfDayMs.size > 1 && !isMutating,
                        ) {
                            Icon(
                                Icons.CloseW400Outlinedfill1,
                                contentDescription = stringResource(Res.string.medical_routine_form_remove_time),
                            )
                        }
                    }
                }
            }
            FilledTonalButton(
                enabled = !isMutating,
                onClick = {
                    editingTimeIndex = null
                    showTimePicker = true
                },
            ) {
                Text(stringResource(Res.string.medical_routine_form_add_time))
            }
        }

        RoutineSectionHeading(stringResource(Res.string.medical_routine_form_repeat))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = draft.repeatType == RoutineRepeatType.DAILY,
                enabled = !isMutating,
                onClick = { onDraftChange(draft.copy(repeatType = RoutineRepeatType.DAILY)) },
                label = { Text(stringResource(Res.string.medical_routine_repeat_daily)) },
            )
            FilterChip(
                selected = draft.repeatType == RoutineRepeatType.WEEKLY,
                enabled = !isMutating,
                onClick = { onDraftChange(draft.copy(repeatType = RoutineRepeatType.WEEKLY)) },
                label = { Text(stringResource(Res.string.medical_routine_repeat_specific_days)) },
            )
        }

        if (draft.repeatType == RoutineRepeatType.WEEKLY) {
            val weekdayLabels = scheduleWeekdayLabels()
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weekdayLabels.forEachIndexed { index, day ->
                    FilterChip(
                        selected = index in draft.daysOfWeek,
                        enabled = !isMutating,
                        onClick = {
                            val nextDays = draft.daysOfWeek.toMutableSet().apply {
                                if (!add(index)) remove(index)
                            }
                            onDraftChange(draft.copy(daysOfWeek = nextDays))
                        },
                        label = { Text(day) },
                    )
                }
            }
        }

        HorizontalDivider()
        DateValueField(
            label = stringResource(Res.string.medical_routine_form_start_date),
            value = LocalDate.parse(draft.startDate),
            onClick = { activeDatePicker = "start" },
            modifier = startDateModifier,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(Res.string.medical_routine_form_ongoing), fontWeight = FontWeight.SemiBold)
            Switch(
                checked = draft.endDate == null,
                enabled = !isMutating,
                onCheckedChange = { ongoing ->
                    onDraftChange(
                        if (ongoing) {
                            draft.copy(endDate = null)
                        } else {
                            draft.copy(endDate = draft.endDate ?: draft.startDate)
                        },
                    )
                },
            )
        }
        if (draft.endDate != null) {
            DateValueField(
                label = stringResource(Res.string.medical_routine_form_end_date),
                value = LocalDate.parse(draft.endDate),
                onClick = { activeDatePicker = "end" },
            )
            if (invalidEndDate) {
                Text(
                    stringResource(Res.string.medical_routine_form_end_date_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(Res.string.medical_routine_form_reminders),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = draft.hasReminder,
                enabled = !isMutating,
                onCheckedChange = { enabled ->
                    onDraftChange(
                        if (enabled) {
                            draft.copy(
                                hasReminder = true,
                                reminderOffsetsMins = draft.reminderOffsetsMins.ifEmpty {
                                    setOf(0)
                                },
                            )
                        } else {
                            draft.copy(hasReminder = false, reminderOffsetsMins = emptySet())
                        },
                    )
                },
            )
        }
        if (draft.hasReminder) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                scheduleReminderOffsets.forEach { offset ->
                    FilterChip(
                        selected = offset in draft.reminderOffsetsMins,
                        enabled = !isMutating,
                        onClick = {
                            val nextOffsets = draft.reminderOffsetsMins.toMutableSet().apply {
                                if (!add(offset)) remove(offset)
                            }
                            onDraftChange(draft.copy(reminderOffsetsMins = nextOffsets))
                        },
                        label = {
                            Text(
                                if (offset == 0) {
                                    stringResource(Res.string.medical_routine_form_reminder_at_time)
                                } else {
                                    stringResource(Res.string.medical_routine_form_reminder_before, offset)
                                },
                            )
                        },
                    )
                }
            }
        }

        if (showMedicationSection) {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.medical_routine_form_included_medications),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (medications.size > 1) {
                    TextButton(
                        onClick = { showMedicationPicker = !showMedicationPicker },
                        enabled = !isMutating,
                    ) {
                        Text(
                            if (showMedicationPicker) {
                                stringResource(Res.string.medical_routine_form_done)
                            } else {
                                stringResource(Res.string.medical_routine_form_change)
                            },
                        )
                    }
                }
            }
            when {
                medications.isEmpty() -> Text(
                    stringResource(Res.string.medical_routine_form_no_medications),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                !showMedicationPicker && draft.medicationIds.isNotEmpty() -> {
                    val selectedMedications = medications.filter { it.id in draft.medicationIds }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        selectedMedications.forEach { medication ->
                            SelectionPill(medication.name)
                        }
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        medications.forEach { medication ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = medication.id in draft.medicationIds,
                                        enabled = !isMutating,
                                        onCheckedChange = { checked ->
                                            val nextIds = draft.medicationIds.toMutableSet().apply {
                                                if (checked) add(medication.id) else remove(medication.id)
                                            }
                                            onDraftChange(draft.copy(medicationIds = nextIds))
                                        },
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(medication.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${medication.dosage} • ${medication.stockLabel()}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showArchiveToggle) {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.medical_routine_form_archive),
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = draft.status == RoutineStatus.ARCHIVED,
                    enabled = !isMutating,
                    onCheckedChange = { archived ->
                        onDraftChange(
                            draft.copy(
                                status = if (archived) RoutineStatus.ARCHIVED else RoutineStatus.ACTIVE,
                            ),
                        )
                    },
                )
            }
        }
    }

    activeDatePicker?.let { target ->
        val currentDate = if (target ==
            "start"
        ) {
            LocalDate.parse(draft.startDate)
        } else {
            LocalDate.parse(draft.endDate ?: draft.startDate)
        }
        DatePickerSheet(
            initialDate = currentDate,
            onDismiss = { activeDatePicker = null },
            onConfirm = { selectedDate ->
                onDraftChange(
                    if (target == "start") {
                        draft.copy(
                            startDate = selectedDate.toString(),
                            endDate = draft.endDate?.takeIf { LocalDate.parse(it) >= selectedDate },
                        )
                    } else {
                        draft.copy(endDate = selectedDate.toString())
                    },
                )
                activeDatePicker = null
            },
        )
    }

    if (showTimePicker) {
        TimePickerSheet(
            initialTimeMs =
            editingTimeIndex?.let { index -> draft.timesOfDayMs.getOrElse(index) { DefaultRoutineTimeMs } }
                ?: draft.timesOfDayMs.lastOrNull()
                ?: DefaultRoutineTimeMs,
            onDismiss = { showTimePicker = false },
            onConfirm = { selectedTime ->
                val updatedTimes = draft.timesOfDayMs.toMutableList()
                val existingIndex = editingTimeIndex
                if (existingIndex == null) {
                    updatedTimes += selectedTime
                } else {
                    updatedTimes[existingIndex] = selectedTime
                }
                onDraftChange(draft.copy(timesOfDayMs = updatedTimes.sorted().distinct()))
                editingTimeIndex = null
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun RoutineSectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SelectionPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
