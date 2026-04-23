package com.group8.comp2300.feature.medical.shared.routines

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
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
fun ScheduleFormSheet(
    title: String,
    subtitle: String? = null,
    routineToEdit: Routine?,
    medications: List<Medication>,
    initialSelectedMedicationIds: Set<String> = routineToEdit?.medicationIds?.toSet() ?: emptySet(),
    isMutating: Boolean,
    onSave: (com.group8.comp2300.domain.model.medical.RoutineCreateRequest, String?) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onCancel: () -> Unit,
    showMedicationSection: Boolean = true,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var draft by remember(routineToEdit?.id, title, initialSelectedMedicationIds) {
        mutableStateOf(
            routineToEdit?.toDraft()
                ?: defaultRoutineDraft(today = today, medicationIds = initialSelectedMedicationIds),
        )
    }
    var showDeleteConfirmation by remember(routineToEdit?.id, title) { mutableStateOf(false) }
    val canSave = canSaveRoutineDraft(
        draft = draft,
        requireName = true,
        requireMedicationSelection = showMedicationSection,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        androidx.compose.foundation.layout.Row(
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
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (routineToEdit != null && onDelete != null) {
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

        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RoutineFieldsSection(
                draft = draft,
                onDraftChange = { draft = it },
                medications = medications,
                isMutating = isMutating,
                showNameField = true,
                showMedicationSection = showMedicationSection,
                showArchiveToggle = routineToEdit != null,
            )
        }

        HorizontalDivider()
        Button(
            onClick = { onSave(draft.toRequest(), routineToEdit?.id) },
            enabled = canSave && !isMutating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (routineToEdit != null) {
                    stringResource(Res.string.medical_routine_form_update_button)
                } else {
                    stringResource(Res.string.medical_routine_form_save_button)
                },
            )
        }
    }

    if (showDeleteConfirmation && routineToEdit != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(Res.string.medical_routine_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.medical_routine_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(routineToEdit.id)
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
fun scheduleWeekdayLabels(): List<String> = listOf(
    weekdaySummary(listOf(0)),
    weekdaySummary(listOf(1)),
    weekdaySummary(listOf(2)),
    weekdaySummary(listOf(3)),
    weekdaySummary(listOf(4)),
    weekdaySummary(listOf(5)),
    weekdaySummary(listOf(6)),
)
