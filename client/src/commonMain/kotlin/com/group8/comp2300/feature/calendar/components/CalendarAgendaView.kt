package com.group8.comp2300.feature.calendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.feature.calendar.CalendarAgendaDay
import com.group8.comp2300.feature.calendar.CalendarViewMode
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CalendarMonthW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.FormatListBulletedW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarViewModeSwitch(
    viewMode: CalendarViewMode,
    onSelect: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = viewMode == CalendarViewMode.CALENDAR,
            onClick = { onSelect(CalendarViewMode.CALENDAR) },
            label = { Text(stringResource(Res.string.calendar_view_calendar)) },
            leadingIcon = {
                Icon(Icons.CalendarMonthW400Outlinedfill1, contentDescription = null)
            },
        )
        FilterChip(
            selected = viewMode == CalendarViewMode.AGENDA,
            onClick = { onSelect(CalendarViewMode.AGENDA) },
            label = { Text(stringResource(Res.string.calendar_view_agenda)) },
            leadingIcon = {
                Icon(Icons.FormatListBulletedW400Outlinedfill1, contentDescription = null)
            },
        )
    }
}

@Composable
internal fun AgendaRangeHeader(
    startDate: LocalDate,
    endDate: LocalDate,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.calendar_agenda_window_previous_desc),
                )
            }
            Text(
                "${DateFormatter.formatDayMonthYear(startDate)} - ${DateFormatter.formatDayMonthYear(endDate)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.calendar_agenda_window_forward_desc),
                    modifier = Modifier.rotate(180f),
                )
            }
        }
        FilterChip(
            selected = false,
            onClick = onToday,
            label = { Text(stringResource(Res.string.calendar_today_button)) },
        )
    }
}

@Composable
internal fun AgendaDaySection(
    day: CalendarAgendaDay,
    appointments: List<Appointment>,
    expandedRoutineKeys: Set<String>,
    onToggleRoutineExpansion: (String) -> Unit,
    onLogMedication: (RoutineDayAgenda, String, MedicationLogStatus) -> Unit,
    onLogAll: (RoutineDayAgenda, MedicationLogStatus) -> Unit,
    onMoveDose: (RoutineDayAgenda) -> Unit,
    onAppointmentClick: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val routineDoseCount = day.routineAgenda.sumOf { it.medications.size }
        val summaryParts = mutableListOf<String>()
        if (routineDoseCount > 0) {
            summaryParts += if (routineDoseCount == 1) {
                stringResource(Res.string.calendar_agenda_scheduled_one)
            } else {
                stringResource(Res.string.calendar_agenda_scheduled_many, routineDoseCount)
            }
        }
        if (day.manualLogs.isNotEmpty()) {
            summaryParts += if (day.manualLogs.size == 1) {
                stringResource(Res.string.calendar_extra_log_one)
            } else {
                stringResource(Res.string.calendar_extra_log_many, day.manualLogs.size)
            }
        }
        if (appointments.isNotEmpty()) {
            summaryParts += if (appointments.size == 1) {
                stringResource(Res.string.calendar_appointment_one)
            } else {
                stringResource(Res.string.calendar_appointment_many, appointments.size)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                DateFormatter.formatDayMonthYear(day.date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (summaryParts.isNotEmpty()) {
                Text(
                    summaryParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        day.routineAgenda.forEach { routine ->
            val routineKey = "${routine.routineId}:${routine.occurrenceTimeMs}"
            RoutineAgendaCard(
                routine = routine,
                isExpanded = routineKey in expandedRoutineKeys,
                onToggleExpansion = { onToggleRoutineExpansion(routineKey) },
                onLogMedication = { medicationId, status -> onLogMedication(routine, medicationId, status) },
                onLogAll = { status -> onLogAll(routine, status) },
                onMoveDose = { onMoveDose(routine) },
            )
        }

        day.manualLogs.forEach { log ->
            ManualLogCard(log)
        }

        appointments.forEach { appointment ->
            AppointmentCard(
                appointment = appointment,
                onClick = { onAppointmentClick(appointment) },
            )
        }
    }
}

internal fun hasAgendaContent(day: CalendarAgendaDay, appointments: List<Appointment>): Boolean =
    day.routineAgenda.isNotEmpty() || day.manualLogs.isNotEmpty() || appointments.isNotEmpty()

@Composable
internal fun AgendaEmptyState(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.calendar_agenda_empty),
        modifier = modifier.padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
