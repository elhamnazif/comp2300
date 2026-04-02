package com.group8.comp2300.presentation.screens.medical.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.presentation.util.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val ScheduleReminderOffsets = listOf(0, 5, 10, 15, 30, 60)
private val ScheduleWeekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

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
                        contentDescription = "Delete schedule",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        MedicalFormTextField(
            label = "Schedule name",
            value = name,
            onValueChange = { name = it },
            placeholder = "Morning meds",
        )

        Text("Dose times", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                        Text("Remove")
                    }
                }
            }
            FilledTonalButton(onClick = {
                editingTimeIndex = null
                showTimePicker = true
            }) {
                Text("Add time")
            }
        }

        Text("Repeat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = repeatType == RoutineRepeatType.DAILY, onClick = {
                repeatType =
                    RoutineRepeatType.DAILY
            }, label = { Text("Every day") })
            FilterChip(selected = repeatType == RoutineRepeatType.WEEKLY, onClick = {
                repeatType =
                    RoutineRepeatType.WEEKLY
            }, label = { Text("Specific days") })
        }

        if (repeatType == RoutineRepeatType.WEEKLY) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleWeekdays.forEachIndexed { index, day ->
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

        DateValueField(label = "Start date", value = LocalDate.parse(startDate), onClick = {
            activeDatePicker = "start"
        })
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Ongoing", fontWeight = FontWeight.SemiBold)
            Switch(checked = ongoing, onCheckedChange = { ongoing = it })
        }
        if (!ongoing) {
            DateValueField(label = "End date", value = LocalDate.parse(endDate), onClick = { activeDatePicker = "end" })
            if (invalidEndDate) {
                Text("End date must be on or after the start date.", color = MaterialTheme.colorScheme.error)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
                        label = { Text(if (offset == 0) "At time" else "$offset min before") },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Included medications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (medications.size > 1) {
                TextButton(onClick = { showMedicationPicker = !showMedicationPicker }) {
                    Text(if (showMedicationPicker) "Done" else "Change")
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
                Text("Archive schedule", fontWeight = FontWeight.SemiBold)
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
            Text(if (routineToEdit == null) "Save schedule" else "Update schedule")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
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
                ?: 9 * 60 * 60 * 1000L,
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

fun scheduleSummary(routine: Routine): String {
    val timeSummary = formatTimesSummary(routine.timesOfDayMs)
    val repeat = when (routine.repeatType) {
        RoutineRepeatType.DAILY -> "Daily"
        RoutineRepeatType.WEEKLY -> "On ${routine.daysOfWeek.sorted().joinToString { ScheduleWeekdays[it] }}"
    }
    val reminder =
        if (!routine.hasReminder || routine.reminderOffsetsMins.isEmpty()) {
            "No reminders"
        } else {
            routine.reminderOffsetsMins.joinToString(prefix = "Reminder ", separator = ", ") { offset ->
                if (offset == 0) "at time" else "$offset min before"
            }
        }
    return "$timeSummary • $repeat • $reminder"
}

fun scheduleLinkSummary(routine: Routine): String {
    val repeatSummary = when (routine.repeatType) {
        RoutineRepeatType.DAILY -> "Daily"
        RoutineRepeatType.WEEKLY -> "On ${routine.daysOfWeek.sorted().joinToString { day -> ScheduleWeekdays[day] }}"
    }
    return "${formatTimesSummary(routine.timesOfDayMs)} • $repeatSummary"
}

fun formatTimesSummary(timesOfDayMs: List<Long>): String = timesOfDayMs.sorted().distinct()
    .joinToString { formatTimeOfDayMs(it) }

fun formatTimeOfDayMs(timeOfDayMs: Long): String {
    val totalMinutes = (timeOfDayMs / 60_000L).toInt()
    return DateFormatter.formatTime(totalMinutes / 60, totalMinutes % 60)
}
