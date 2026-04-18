package com.group8.comp2300.feature.medical.shared.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.DatePickerSheet
import com.group8.comp2300.core.ui.components.DateValueField
import com.group8.comp2300.core.ui.components.TimePickerSheet
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.medical.shared.forms.MedicalFormTextField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private val ScheduleReminderOffsets = listOf(0, 5, 10, 15, 30, 60)

@Composable
fun ScheduleFormSheet(
    title: String,
    subtitle: String? = null,
    routineToEdit: Routine?,
    medications: List<Medication>,
    initialSelectedMedicationIds: Set<String> = routineToEdit?.medicationIds?.toSet() ?: emptySet(),
    onSave: (RoutineCreateRequest, String?) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onCancel: () -> Unit,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val weekdayLabels = scheduleWeekdayLabels()
    var name by remember(routineToEdit?.id, title) { mutableStateOf(routineToEdit?.name ?: "") }
    var timesOfDayMs by remember(routineToEdit?.id, title) {
        mutableStateOf(
            routineToEdit?.timesOfDayMs?.sorted()?.distinct()?.ifEmpty { listOf(9 * 60 * 60 * 1000L) }
                ?: listOf(9 * 60 * 60 * 1000L),
        )
    }
    var repeatType by remember(routineToEdit?.id, title) {
        mutableStateOf(routineToEdit?.repeatType ?: RoutineRepeatType.DAILY)
    }
    var daysOfWeek by remember(routineToEdit?.id, title) {
        mutableStateOf(routineToEdit?.daysOfWeek?.toSet() ?: setOf(1, 2, 3, 4, 5))
    }
    var startDate by remember(routineToEdit?.id, title) { mutableStateOf(routineToEdit?.startDate ?: today.toString()) }
    var endDate by remember(routineToEdit?.id, title) { mutableStateOf(routineToEdit?.endDate ?: today.toString()) }
    var ongoing by remember(routineToEdit?.id, title) { mutableStateOf(routineToEdit?.endDate == null) }
    var hasReminder by remember(routineToEdit?.id, title) { mutableStateOf(routineToEdit?.hasReminder ?: true) }
    var offsets by remember(routineToEdit?.id, title) {
        mutableStateOf(routineToEdit?.reminderOffsetsMins?.toSet() ?: setOf(0))
    }
    var status by remember(routineToEdit?.id, title) { mutableStateOf(routineToEdit?.status ?: RoutineStatus.ACTIVE) }
    var selectedMedicationIds by remember(routineToEdit?.id, title, initialSelectedMedicationIds) {
        mutableStateOf((routineToEdit?.medicationIds?.toSet() ?: initialSelectedMedicationIds).toSet())
    }
    var activeDatePicker by remember { mutableStateOf<String?>(null) }
    var editingTimeIndex by remember { mutableStateOf<Int?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMedicationPicker by remember(routineToEdit?.id, title, initialSelectedMedicationIds) {
        mutableStateOf(routineToEdit != null || initialSelectedMedicationIds.isEmpty())
    }
    val invalidEndDate = !ongoing && LocalDate.parse(endDate) < LocalDate.parse(startDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (routineToEdit != null && onDelete != null) {
                IconButton(onClick = { onDelete(routineToEdit.id) }) {
                    Icon(
                        Icons.DeleteW400Outlined,
                        contentDescription = stringResource(Res.string.medical_routine_form_archive),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        MedicalFormTextField(
            label = stringResource(Res.string.medical_routine_form_name_label),
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(Res.string.medical_routine_form_name_placeholder),
        )

        Text(
            stringResource(Res.string.medical_routine_form_dose_times),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            timesOfDayMs.forEachIndexed { index, timeOfDayMs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AssistChip(
                        onClick = {
                            editingTimeIndex = index
                            showTimePicker = true
                        },
                        label = { Text(formatTimeOfDayMs(timeOfDayMs)) },
                    )
                    TextButton(
                        onClick = {
                            timesOfDayMs = timesOfDayMs.toMutableList().apply { removeAt(index) }.sorted()
                        },
                        enabled = timesOfDayMs.size > 1,
                    ) {
                        Text(stringResource(Res.string.medical_routine_form_remove_time))
                    }
                }
            }
            FilledTonalButton(onClick = {
                editingTimeIndex = null
                showTimePicker = true
            }) {
                Text(stringResource(Res.string.medical_routine_form_add_time))
            }
        }

        Text(
            stringResource(Res.string.medical_routine_form_repeat),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = repeatType == RoutineRepeatType.DAILY, onClick = {
                repeatType =
                    RoutineRepeatType.DAILY
            }, label = { Text(stringResource(Res.string.medical_routine_repeat_daily)) })
            FilterChip(selected = repeatType == RoutineRepeatType.WEEKLY, onClick = {
                repeatType =
                    RoutineRepeatType.WEEKLY
            }, label = { Text(stringResource(Res.string.medical_routine_repeat_specific_days)) })
        }

        if (repeatType == RoutineRepeatType.WEEKLY) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weekdayLabels.forEachIndexed { index, day ->
                    FilterChip(
                        selected = index in daysOfWeek,
                        onClick = {
                            daysOfWeek = daysOfWeek.toMutableSet().apply {
                                if (!add(index)) remove(index)
                            }
                        },
                        label = { Text(day) },
                    )
                }
            }
        }

        DateValueField(
            label = stringResource(
                Res.string.medical_routine_form_start_date,
            ),
            value = LocalDate.parse(startDate),
            onClick = {
                activeDatePicker = "start"
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(Res.string.medical_routine_form_ongoing), fontWeight = FontWeight.SemiBold)
            Switch(checked = ongoing, onCheckedChange = { ongoing = it })
        }
        if (!ongoing) {
            DateValueField(
                label = stringResource(
                    Res.string.medical_routine_form_end_date,
                ),
                value = LocalDate.parse(endDate),
                onClick = {
                    activeDatePicker =
                        "end"
                },
            )
            if (invalidEndDate) {
                Text(
                    stringResource(Res.string.medical_routine_form_end_date_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

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
            Switch(checked = hasReminder, onCheckedChange = { hasReminder = it })
        }
        if (hasReminder) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleReminderOffsets.forEach { offset ->
                    FilterChip(
                        selected = offset in offsets,
                        onClick = {
                            offsets = offsets.toMutableSet().apply {
                                if (!add(offset)) remove(offset)
                            }
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
                TextButton(onClick = { showMedicationPicker = !showMedicationPicker }) {
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

        if (!showMedicationPicker && selectedMedicationIds.isNotEmpty()) {
            val selectedNames = medications.filter { it.id in selectedMedicationIds }.joinToString { it.name }
            Text(
                selectedNames,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            medications.forEach { medication ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = medication.id in selectedMedicationIds,
                        onCheckedChange = { checked ->
                            selectedMedicationIds = selectedMedicationIds.toMutableSet().apply {
                                if (checked) add(medication.id) else remove(medication.id)
                            }
                        },
                    )
                    Column {
                        Text(medication.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            medication.quantity.takeIf(String::isNotBlank)?.let { "${medication.dosage} • $it" }
                                ?: medication.dosage,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        if (routineToEdit != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(Res.string.medical_routine_form_archive), fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = status == RoutineStatus.ARCHIVED,
                    onCheckedChange = { status = if (it) RoutineStatus.ARCHIVED else RoutineStatus.ACTIVE },
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    RoutineCreateRequest(
                        name = name,
                        timesOfDayMs = timesOfDayMs.sorted(),
                        repeatType = repeatType.name,
                        daysOfWeek = if (repeatType == RoutineRepeatType.WEEKLY) daysOfWeek.sorted() else emptyList(),
                        startDate = startDate,
                        endDate = if (ongoing) null else endDate,
                        hasReminder = hasReminder,
                        reminderOffsetsMins = if (hasReminder) offsets.sorted() else emptyList(),
                        status = status.name,
                        medicationIds = selectedMedicationIds.sorted(),
                    ),
                    routineToEdit?.id,
                )
            },
            enabled = name.isNotBlank() &&
                timesOfDayMs.isNotEmpty() &&
                selectedMedicationIds.isNotEmpty() &&
                !invalidEndDate &&
                (repeatType != RoutineRepeatType.WEEKLY || daysOfWeek.isNotEmpty()),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (routineToEdit == null) {
                    stringResource(Res.string.medical_routine_form_save_button)
                } else {
                    stringResource(Res.string.medical_routine_form_update_button)
                },
            )
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.common_cancel))
        }
    }

    activeDatePicker?.let { target ->
        val currentDate = if (target == "start") LocalDate.parse(startDate) else LocalDate.parse(endDate)
        DatePickerSheet(
            initialDate = currentDate,
            onDismiss = { activeDatePicker = null },
            onConfirm = {
                if (target == "start") startDate = it.toString() else endDate = it.toString()
                activeDatePicker = null
            },
        )
    }
    if (showTimePicker) {
        TimePickerSheet(
            initialTimeMs = editingTimeIndex?.let { index -> timesOfDayMs.getOrElse(index) { 9 * 60 * 60 * 1000L } }
                ?: timesOfDayMs.lastOrNull()
                ?: (9 * 60 * 60 * 1000L),
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val updatedTimes = timesOfDayMs.toMutableList()
                val existingIndex = editingTimeIndex
                if (existingIndex == null) {
                    updatedTimes += it
                } else {
                    updatedTimes[existingIndex] = it
                }
                timesOfDayMs = updatedTimes.sorted().distinct()
                editingTimeIndex = null
                showTimePicker = false
            },
        )
    }
}

@Composable
fun scheduleSummary(routine: Routine): String {
    val weekdayLabels = scheduleWeekdayLabels()
    val timeSummary = formatTimesSummary(routine.timesOfDayMs)
    val repeat = when (routine.repeatType) {
        RoutineRepeatType.DAILY -> stringResource(Res.string.medical_routine_repeat_daily)

        RoutineRepeatType.WEEKLY -> stringResource(
            Res.string.medical_routine_summary_on_days,
            routine.daysOfWeek.sorted().joinToString { weekdayLabels[it] },
        )
    }
    val reminder =
        if (!routine.hasReminder || routine.reminderOffsetsMins.isEmpty()) {
            stringResource(Res.string.medical_routine_summary_no_reminders)
        } else {
            val reminderParts = buildList {
                for (offset in routine.reminderOffsetsMins) {
                    add(
                        if (offset == 0) {
                            stringResource(Res.string.medical_routine_form_reminder_at_time)
                        } else {
                            stringResource(Res.string.medical_routine_form_reminder_before, offset)
                        },
                    )
                }
            }
            stringResource(
                Res.string.medical_routine_summary_reminder_prefix,
                reminderParts.joinToString(separator = ", "),
            )
        }
    return "$timeSummary • $repeat • $reminder"
}

@Composable
fun scheduleLinkSummary(routine: Routine): String {
    val weekdayLabels = scheduleWeekdayLabels()
    val repeatSummary = when (routine.repeatType) {
        RoutineRepeatType.DAILY -> stringResource(Res.string.medical_routine_repeat_daily)

        RoutineRepeatType.WEEKLY -> stringResource(
            Res.string.medical_routine_summary_on_days,
            routine.daysOfWeek.sorted().joinToString { day -> weekdayLabels[day] },
        )
    }
    return "${formatTimesSummary(routine.timesOfDayMs)} • $repeatSummary"
}

fun formatTimesSummary(timesOfDayMs: List<Long>): String = timesOfDayMs.sorted().distinct()
    .joinToString { formatTimeOfDayMs(it) }

fun formatTimeOfDayMs(timeOfDayMs: Long): String {
    val totalMinutes = (timeOfDayMs / 60_000L).toInt()
    return DateFormatter.formatTime(totalMinutes / 60, totalMinutes % 60)
}

@Composable
private fun scheduleWeekdayLabels(): List<String> = listOf(
    stringResource(Res.string.common_day_sun_short),
    stringResource(Res.string.common_day_mon_short),
    stringResource(Res.string.common_day_tue_short),
    stringResource(Res.string.common_day_wed_short),
    stringResource(Res.string.common_day_thu_short),
    stringResource(Res.string.common_day_fri_short),
    stringResource(Res.string.common_day_sat_short),
)
