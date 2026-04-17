package com.group8.comp2300.feature.calendar

import androidx.compose.ui.graphics.Color
import com.group8.comp2300.core.ui.accessibility.IndicatorPattern
import com.group8.comp2300.core.ui.accessibility.StatusIcon
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationOccurrenceCandidate
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda

internal sealed interface CalendarSheetState {
    data object Hidden : CalendarSheetState

    data object Menu : CalendarSheetState

    data object FormMedication : CalendarSheetState

    data class FormReschedule(val routine: RoutineDayAgenda) : CalendarSheetState

    data class ResolveMedication(
        val pendingLog: MedicationLogRequest,
        val candidates: List<MedicationOccurrenceCandidate>,
    ) : CalendarSheetState

    data object FormMood : CalendarSheetState

    data class AppointmentDetails(val appointment: Appointment) : CalendarSheetState
}

internal data class StatusVisual(
    val label: String,
    val color: Color,
    val pattern: IndicatorPattern,
    val icon: StatusIcon,
)
