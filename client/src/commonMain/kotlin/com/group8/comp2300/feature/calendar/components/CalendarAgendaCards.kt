@file:Suppress("ktlint:standard:max-line-length")

package com.group8.comp2300.feature.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.feature.medical.shared.routines.ReminderIndicator
import com.group8.comp2300.core.ui.accessibility.AccessibleStatusChip
import com.group8.comp2300.core.ui.accessibility.IndicatorPattern
import com.group8.comp2300.core.ui.accessibility.StatusIcon
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineMedicationAgenda
import com.group8.comp2300.feature.calendar.StatusVisual
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun CalendarDayHeader(
    date: LocalDate,
    agenda: List<RoutineDayAgenda>,
    extraLogCount: Int,
    appointmentCount: Int,
    moodCount: Int = 0,
) {
    val allDoses = agenda.flatMap(RoutineDayAgenda::medications)
    val taken = allDoses.count { it.status == MedicationLogStatus.TAKEN }
    val unresolved = allDoses.count {
        it.status == MedicationLogStatus.PENDING ||
            it.status == MedicationLogStatus.MISSED ||
            it.status == MedicationLogStatus.SNOOZED
    }
    val summary =
        if (allDoses.isEmpty()) {
            stringResource(Res.string.calendar_no_scheduled_doses)
        } else {
            stringResource(Res.string.calendar_taken_summary, taken, allDoses.size)
        }
    val metaItems = mutableListOf<String>()
    if (allDoses.isNotEmpty() && unresolved > 0) {
        metaItems +=
            if (unresolved == 1) {
                stringResource(Res.string.calendar_unresolved_one)
            } else {
                stringResource(Res.string.calendar_unresolved_many, unresolved)
            }
    }
    if (extraLogCount > 0) {
        metaItems +=
            if (extraLogCount == 1) {
                stringResource(Res.string.calendar_extra_log_one)
            } else {
                stringResource(Res.string.calendar_extra_log_many, extraLogCount)
            }
    }
    if (appointmentCount > 0) {
        metaItems +=
            if (appointmentCount == 1) {
                stringResource(Res.string.calendar_appointment_one)
            } else {
                stringResource(Res.string.calendar_appointment_many, appointmentCount)
            }
    }
    if (moodCount > 0) {
        metaItems +=
            if (moodCount == 1) {
                stringResource(Res.string.calendar_mood_one)
            } else {
                stringResource(Res.string.calendar_mood_many, moodCount)
            }
    }
    val meta = metaItems.joinToString(" • ")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            DateFormatter.formatDayMonthYear(date),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            summary,
            style = MaterialTheme.typography.titleMedium,
            color = if (allDoses.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun RoutineAgendaCard(
    routine: RoutineDayAgenda,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onLogMedication: (String, MedicationLogStatus) -> Unit,
    onLogAll: (MedicationLogStatus) -> Unit,
    onMoveDose: () -> Unit,
) {
    val occurrence = Instant.fromEpochMilliseconds(routine.occurrenceTimeMs).toLocalDateTime(TimeZone.currentSystemDefault())
    val actionableMeds = remember(routine.medications) {
        routine.medications.filter {
            it.status == MedicationLogStatus.PENDING ||
                it.status == MedicationLogStatus.MISSED ||
                it.status == MedicationLogStatus.SNOOZED
        }
    }
    val routineCompleted = actionableMeds.isEmpty()
    val showReviewToggle = routine.medications.size > 1 && actionableMeds.isNotEmpty()
    val metaLines = mutableListOf<String>()
    if (routine.isRescheduled) {
        val originalOccurrence = Instant.fromEpochMilliseconds(routine.originalOccurrenceTimeMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        metaLines += stringResource(
            Res.string.calendar_moved_from,
            DateFormatter.formatDayMonthYear(originalOccurrence.date),
            DateFormatter.formatTime(originalOccurrence.hour, originalOccurrence.minute),
        )
    }
    if (routineCompleted) {
        metaLines += routineCompletionSummary(routine.medications)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        DateFormatter.formatTime(occurrence.hour, occurrence.minute),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        routine.routineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (routine.hasReminder && routine.reminderOffsetsMins.isNotEmpty()) {
                    ReminderIndicator(
                        reminderOffsetsMins = routine.reminderOffsetsMins,
                        contentDescription = stringResource(Res.string.calendar_reminders_enabled_desc),
                        iconModifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                routineMedicationSummary(routine.medications),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            metaLines.forEach { metaLine ->
                Text(
                    metaLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!routineCompleted) {
                if (routine.medications.size == 1) {
                    val medication = routine.medications.single()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.TAKEN) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(Res.string.calendar_taken_action)) }
                        OutlinedButton(
                            onClick = { onLogMedication(medication.medicationId, MedicationLogStatus.SKIPPED) },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(Res.string.calendar_skip_action)) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onLogAll(MedicationLogStatus.TAKEN) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.calendar_take_all_action))
                        }
                        OutlinedButton(onClick = {
                            onLogAll(MedicationLogStatus.SKIPPED)
                        }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.calendar_skip_all_action))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showReviewToggle) {
                            TextButton(onClick = onToggleExpansion) {
                                Text(
                                    if (isExpanded) {
                                        stringResource(Res.string.calendar_hide_medications)
                                    } else {
                                        stringResource(Res.string.calendar_review_individually)
                                    },
                                )
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        TextButton(onClick = onMoveDose) {
                            Text(
                                if (routine.isRescheduled) {
                                    stringResource(Res.string.calendar_move_again_action)
                                } else {
                                    stringResource(Res.string.calendar_move_action)
                                },
                            )
                        }
                    }
                }
                if (routine.medications.size == 1) {
                    TextButton(onClick = onMoveDose, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (routine.isRescheduled) {
                                stringResource(Res.string.calendar_move_again_action)
                            } else {
                                stringResource(Res.string.calendar_move_dose_action)
                            },
                        )
                    }
                }
            }

            if (routine.medications.size > 1 && isExpanded) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    routine.medications.forEach { medication ->
                        RoutineMedicationRow(medication = medication, onLogMedication = onLogMedication)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineMedicationRow(
    medication: RoutineMedicationAgenda,
    onLogMedication: (String, MedicationLogStatus) -> Unit,
) {
    val statusVisual = medicationStatusVisual(medication.status)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(medication.medicationName, fontWeight = FontWeight.SemiBold)
                Text(
                    medication.dosage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AccessibleStatusChip(
                label = statusVisual.label,
                icon = statusVisual.icon,
                containerColor = statusVisual.color.copy(alpha = 0.16f),
                contentColor = statusVisual.color,
            )
        }
        if (
            medication.status == MedicationLogStatus.PENDING ||
            medication.status == MedicationLogStatus.MISSED ||
            medication.status == MedicationLogStatus.SNOOZED
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onLogMedication(medication.medicationId, MedicationLogStatus.TAKEN)
                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.calendar_taken_action))
                }
                OutlinedButton(onClick = {
                    onLogMedication(medication.medicationId, MedicationLogStatus.SKIPPED)
                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.calendar_skip_action))
                }
            }
        }
    }
}

@Composable
private fun medicationStatusColor(status: MedicationLogStatus): Color = when (status) {
    MedicationLogStatus.TAKEN -> Color(0xFF2E7D32)
    MedicationLogStatus.SKIPPED, MedicationLogStatus.MISSED -> MaterialTheme.colorScheme.error
    MedicationLogStatus.SNOOZED -> MaterialTheme.colorScheme.primary
    MedicationLogStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun medicationStatusVisual(status: MedicationLogStatus): StatusVisual = StatusVisual(
    label = status.displayName,
    color = medicationStatusColor(status),
    pattern = when (status) {
        MedicationLogStatus.TAKEN -> IndicatorPattern.DIAGONAL
        MedicationLogStatus.SKIPPED, MedicationLogStatus.MISSED -> IndicatorPattern.GRID
        MedicationLogStatus.SNOOZED -> IndicatorPattern.HORIZONTAL
        MedicationLogStatus.PENDING -> IndicatorPattern.DOTS
    },
    icon = when (status) {
        MedicationLogStatus.TAKEN -> StatusIcon.SUCCESS
        MedicationLogStatus.SKIPPED, MedicationLogStatus.MISSED -> StatusIcon.DANGER
        MedicationLogStatus.SNOOZED -> StatusIcon.WARNING
        MedicationLogStatus.PENDING -> StatusIcon.DATE
    },
)

private fun routineMedicationSummary(medications: List<RoutineMedicationAgenda>): String = when (medications.size) {
    0 -> ""
    1 -> "${medications.single().medicationName} • ${medications.single().dosage}"
    2 -> medications.joinToString(" • ") { it.medicationName }
    else -> "${medications[0].medicationName}, ${medications[1].medicationName} +${medications.size - 2}"
}

@Composable
private fun routineCompletionSummary(medications: List<RoutineMedicationAgenda>): String {
    val taken = medications.count { it.status == MedicationLogStatus.TAKEN }
    val skipped = medications.count {
        it.status == MedicationLogStatus.SKIPPED || it.status == MedicationLogStatus.MISSED
    }
    return when {
        taken == medications.size -> stringResource(Res.string.calendar_all_medications_taken)
        skipped == medications.size -> stringResource(Res.string.calendar_marked_skipped)
        taken > 0 && skipped > 0 -> stringResource(Res.string.calendar_taken_skipped_summary, taken, skipped)
        else -> stringResource(Res.string.calendar_completed)
    }
}

@Composable
internal fun ManualLogCard(log: MedicationLog) {
    val dateTime = Instant.fromEpochMilliseconds(log.medicationTime).toLocalDateTime(TimeZone.currentSystemDefault())
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    log.medicationName ?: stringResource(Res.string.calendar_medication_fallback),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(
                        Res.string.calendar_log_status_at_time,
                        log.status.displayName,
                        DateFormatter.formatTime(dateTime.hour, dateTime.minute),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(Res.string.calendar_extra_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
