@file:Suppress("ktlint:standard:max-line-length")

package com.group8.comp2300.feature.calendar.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.*
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.calendar.CalendarSheetState
import com.group8.comp2300.feature.calendar.FormConstants
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
internal fun ManualMedicationLogForm(
    medications: List<Medication>,
    isSaving: Boolean,
    onSave: (String, MedicationLogStatus) -> Unit,
    onOpenMedicationCabinet: () -> Unit,
) {
    var selectedMedId by remember(medications) { mutableStateOf(medications.firstOrNull()?.id.orEmpty()) }
    var selectedStatus by remember { mutableStateOf(MedicationLogStatus.TAKEN) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (medications.isEmpty()) {
            EmptyStateMessage(stringResource(Res.string.calendar_no_medications_yet))
        } else {
            SimpleDropdown(
                label = stringResource(Res.string.form_medication_label),
                options = medications.map { it.id to "${it.name} • ${it.dosage}" },
                selectedKey = selectedMedId,
                onSelect = { selectedMedId = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(MedicationLogStatus.TAKEN, MedicationLogStatus.SKIPPED).forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        enabled = !isSaving,
                        onClick = { selectedStatus = status },
                        label = { Text(status.displayName) },
                    )
                }
            }
            Button(
                onClick = { onSave(selectedMedId, selectedStatus) },
                enabled = selectedMedId.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.calendar_save_log))
            }
        }
        TextButton(
            onClick = onOpenMedicationCabinet,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
        ) {
            Text(stringResource(Res.string.calendar_manage_medications))
        }
    }
}

@Composable
internal fun ResolveMedicationLogSheet(
    medicationName: String,
    candidates: List<MedicationOccurrenceCandidate>,
    onBack: () -> Unit,
    onAttach: (MedicationOccurrenceCandidate) -> Unit,
    onLogExtraDose: () -> Unit,
    isSaving: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !isSaving) {
                Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = stringResource(Res.string.common_back_desc))
            }
            Text(
                stringResource(Res.string.calendar_match_scheduled_dose_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(Res.string.calendar_match_scheduled_dose_desc, medicationName),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        candidates.forEach { candidate ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(candidate.routineName, fontWeight = FontWeight.SemiBold)
                    val occurrence = Instant.fromEpochMilliseconds(candidate.occurrenceTimeMs)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        stringResource(
                            Res.string.calendar_occurrence_status,
                            DateFormatter.formatDayMonthYear(occurrence.date),
                            DateFormatter.formatTime(occurrence.hour, occurrence.minute),
                            candidate.status.displayName,
                        ),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Button(
                        onClick = { onAttach(candidate) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                    ) {
                        Text(stringResource(Res.string.calendar_count_toward_schedule))
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onLogExtraDose,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
        ) {
            Text(stringResource(Res.string.calendar_save_as_extra_log))
        }
    }
}

@Composable
internal fun AddEntryMenu(selectedDate: LocalDate, onSelectType: (CalendarSheetState) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.calendar_add_entry_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            DateFormatter.formatDayMonthYear(selectedDate),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = { onSelectType(CalendarSheetState.FormMedication) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_menu_log_med))
        }
        OutlinedButton(onClick = { onSelectType(CalendarSheetState.FormMood) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_menu_track_mood))
        }
    }
}

@Composable
internal fun WrapperFormLayout(
    title: String,
    entryDate: LocalDate,
    entryTime: Pair<Int, Int>,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    var activeDatePicker by remember { mutableStateOf(false) }
    var activeTimePicker by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.ArrowBackW400Outlinedfill1, contentDescription = stringResource(Res.string.common_back_desc))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        DateValueField(
            label = stringResource(Res.string.form_date_label),
            value = entryDate,
            onClick = { activeDatePicker = true },
        )
        TimeValueField(
            label = stringResource(Res.string.form_time_label),
            value = ((entryTime.first * 60L) + entryTime.second) * 60_000L,
            onClick = { activeTimePicker = true },
        )
        content()
    }
    if (activeDatePicker) {
        DatePickerSheet(
            initialDate = entryDate,
            onDismiss = { activeDatePicker = false },
            onConfirm = {
                onDateChange(it)
                activeDatePicker = false
            },
        )
    }
    if (activeTimePicker) {
        TimePickerSheet(
            initialTimeMs = ((entryTime.first * 60L) + entryTime.second) * 60_000L,
            onDismiss = { activeTimePicker = false },
            onConfirm = {
                val totalMinutes = (it / 60_000L).toInt()
                onTimeChange(totalMinutes / 60, totalMinutes % 60)
                activeTimePicker = false
            },
        )
    }
}

@Composable
internal fun MoodEntryForm(isSaving: Boolean, onSave: (Int, String) -> Unit) {
    var moodScore by remember { mutableIntStateOf(3) }
    var notes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = FormConstants.moodLabels()[moodScore - 1],
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FormConstants.MoodEmojis.forEachIndexed { index, emoji ->
                    val score = index + 1
                    val isSelected = moodScore == score
                    val scale by animateFloatAsState(if (isSelected) 1.5f else 1.0f, label = "scale")

                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { moodScore = score },
                    )
                }
            }
            Slider(
                value = moodScore.toFloat(),
                onValueChange = { moodScore = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
            )
        }

        HorizontalDivider()

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.form_mood_journal_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Button(
            onClick = { onSave(moodScore, notes) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
        ) {
            Text(stringResource(Res.string.form_mood_log_button))
        }
    }
}

@Composable
internal fun AppointmentDetailSheetContent(appointment: Appointment, onManageInCare: () -> Unit, onClose: () -> Unit) {
    val dateTime = Instant.fromEpochMilliseconds(appointment.appointmentTime)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(appointment.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "${DateFormatter.formatDayMonthYear(
                dateTime.date,
            )} at ${DateFormatter.formatTime(dateTime.hour, dateTime.minute)}",
        )
        Text(appointment.appointmentType, color = MaterialTheme.colorScheme.secondary)
        appointment.notes?.takeIf(String::isNotBlank)?.let { Text(it) }
        TextButton(onClick = onManageInCare, modifier = Modifier.fillMaxWidth()) {
            Text("Manage in care")
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.calendar_close_action))
        }
    }
}

@Composable
internal fun AppointmentCard(appointment: Appointment, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val dateTime = Instant.fromEpochMilliseconds(appointment.appointmentTime)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    Surface(
        modifier = modifier,
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(appointment.title, fontWeight = FontWeight.SemiBold)
            Text(
                DateFormatter.formatTime(dateTime.hour, dateTime.minute),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun entryDateTimeToEpochMs(entryDate: LocalDate, entryTime: Pair<Int, Int>, timeZone: TimeZone): Long {
    val logDateTime = LocalDateTime(entryDate, LocalTime(entryTime.first, entryTime.second))
    return logDateTime.toInstant(timeZone).toEpochMilliseconds()
}

internal fun routineLogRequest(
    routine: RoutineDayAgenda,
    medicationId: String,
    status: MedicationLogStatus,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
): MedicationLogRequest = MedicationLogRequest(
    medicationId = medicationId,
    status = status.name,
    timestampMs = if (status == MedicationLogStatus.TAKEN) nowMs else routine.occurrenceTimeMs,
    routineId = routine.routineId,
    occurrenceTimeMs = routine.occurrenceTimeMs,
    linkMode = MedicationLogLinkMode.ATTACH_TO_OCCURRENCE,
)

@Composable
private fun SimpleDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedValue = options.firstOrNull { it.first == selectedKey }?.second.orEmpty()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, value) ->
                DropdownMenuItem(
                    text = { Text(value) },
                    onClick = {
                        onSelect(key)
                        expanded = false
                    },
                )
            }
        }
    }
}
