package com.group8.comp2300.feature.medication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.ui.accessibility.IndicatorPattern
import com.group8.comp2300.core.ui.accessibility.PatternSwatch
import com.group8.comp2300.core.ui.components.ActionEmptyStateCard
import com.group8.comp2300.core.ui.components.EmptyStateMessage
import com.group8.comp2300.core.ui.components.SectionHeader
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.medical.shared.routines.formatTimeOfDayMs
import com.group8.comp2300.feature.medical.shared.routines.weekdaySummary
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.EditW400Outlinedfill1
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MedicationListContent(
    state: MedicationUiState,
    onAddMedication: () -> Unit,
    onEditMedication: (Medication) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeMedications = state.medications.filter { it.status == MedicationStatus.ACTIVE }
    val archivedMedications = state.medications.filter { it.status == MedicationStatus.ARCHIVED }

    fun linkedRoutinesFor(medication: Medication): List<Routine> = state.routines.filter { routine ->
        routine.status == RoutineStatus.ACTIVE && medication.id in routine.medicationIds
    }

    if (state.isLoading && state.medications.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.medications.isEmpty()) {
            item {
                ActionEmptyStateCard(
                    title = stringResource(Res.string.medical_medication_empty_title),
                    message = stringResource(Res.string.medical_medication_empty_desc),
                    actionLabel = stringResource(Res.string.medical_medication_empty_add_button),
                    onAction = onAddMedication,
                )
            }
        } else {
            if (activeMedications.isNotEmpty()) {
                item {
                    SectionHeader(stringResource(Res.string.medical_medication_section_active), activeMedications.size)
                }
                items(activeMedications, key = Medication::id) { medication ->
                    MedicationCard(
                        medication = medication,
                        linkedRoutines = linkedRoutinesFor(medication),
                        onClick = { onEditMedication(medication) },
                    )
                }
            }

            if (activeMedications.isEmpty() && archivedMedications.isNotEmpty()) {
                item { EmptyStateMessage(stringResource(Res.string.medical_medication_empty_active)) }
            }

            if (archivedMedications.isNotEmpty()) {
                item {
                    SectionHeader(
                        stringResource(Res.string.medical_medication_section_archived),
                        archivedMedications.size,
                    )
                }
                items(archivedMedications, key = Medication::id) { medication ->
                    MedicationCard(
                        medication = medication,
                        linkedRoutines = linkedRoutinesFor(medication),
                        isArchived = true,
                        onClick = { onEditMedication(medication) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    linkedRoutines: List<Routine>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
) {
    val colorOption = medicationColorOption(medication.colorHex)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
            if (isArchived) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(parseColorHex(medication.colorHex).copy(alpha = 0.18f), CircleShape)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PatternSwatch(
                        color = parseColorHex(medication.colorHex),
                        pattern = colorOption.pattern,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(medication.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${medication.dosage} • ${medication.stockLabel()}", color = MaterialTheme.colorScheme.secondary)
                Text(
                    medicationScheduleLabel(linkedRoutines),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                medication.instruction?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.EditW400Outlinedfill1,
                contentDescription = stringResource(Res.string.medical_medication_edit_desc),
            )
        }
    }
}

@Composable
private fun medicationScheduleLabel(linkedRoutines: List<Routine>): String {
    if (linkedRoutines.isEmpty()) return stringResource(Res.string.medical_medication_no_schedule)
    if (linkedRoutines.size > 1) {
        return stringResource(Res.string.medical_medication_schedule_count, linkedRoutines.size)
    }

    val routine = linkedRoutines.single()
    val times = routine.timesOfDayMs.sorted().distinct()
    val timeLabel = when (times.size) {
        0 -> ""
        1 -> stringResource(Res.string.medical_medication_schedule_at, formatTimeOfDayMs(times.single()))
        else -> stringResource(Res.string.medical_medication_schedule_times, times.size)
    }

    return when (routine.repeatType) {
        RoutineRepeatType.DAILY -> when (times.size) {
            1 -> stringResource(Res.string.medical_medication_schedule_daily_at, formatTimeOfDayMs(times.single()))

            else -> if (timeLabel.isBlank()) {
                stringResource(Res.string.medical_routine_repeat_daily)
            } else {
                stringResource(Res.string.medical_medication_schedule_daily_multi, times.size)
            }
        }

        RoutineRepeatType.WEEKLY -> {
            val days = weekdaySummary(routine.daysOfWeek)
            if (days.isBlank()) {
                stringResource(Res.string.medical_medication_schedule_weekly_default, timeLabel)
            } else {
                "$days$timeLabel"
            }
        }
    }
}

internal data class MedicationColorOption(val hex: String, val pattern: IndicatorPattern)

internal val medicationColorOptions = listOf(
    MedicationColorOption("#42A5F5", IndicatorPattern.DIAGONAL),
    MedicationColorOption("#EF5350", IndicatorPattern.GRID),
    MedicationColorOption("#66BB6A", IndicatorPattern.DOTS),
    MedicationColorOption("#FFA726", IndicatorPattern.HORIZONTAL),
    MedicationColorOption("#AB47BC", IndicatorPattern.VERTICAL),
    MedicationColorOption("#26C6DA", IndicatorPattern.DIAGONAL),
    MedicationColorOption("#78909C", IndicatorPattern.GRID),
)

internal fun medicationColorOption(hex: String?): MedicationColorOption {
    val normalized = normalizeColorHex(hex)
    return medicationColorOptions.firstOrNull { it.hex == normalized } ?: medicationColorOptions.first()
}

internal fun normalizeColorHex(hex: String?): String =
    "#${(hex ?: Medication.PRESET_COLORS.first()).removePrefix("#").uppercase()}"
