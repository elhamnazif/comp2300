package com.group8.comp2300.feature.medical.shared.routines

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.DatePickerSheet
import com.group8.comp2300.core.ui.components.DateValueField
import com.group8.comp2300.core.ui.components.TimePickerSheet
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.feature.medical.shared.forms.MedicalFormTextField
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CloseW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DeleteW400Outlined
import comp2300.i18n.generated.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

private val ScheduleReminderOffsets = listOf(0, 5, 10, 15, 30, 60)
private enum class ScheduleFormStep { TIMING, DURATION, REMINDERS }

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
    val nameFocusRequester = remember { FocusRequester() }
    val startDateFocusRequester = remember { FocusRequester() }
    val reminderFocusRequester = remember { FocusRequester() }
    var currentStep by remember(routineToEdit?.id, title) { mutableStateOf(ScheduleFormStep.TIMING) }
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
    val canContinueTiming = name.isNotBlank() &&
        timesOfDayMs.isNotEmpty() &&
        (repeatType != RoutineRepeatType.WEEKLY || daysOfWeek.isNotEmpty())
    val canContinueDuration = !invalidEndDate
    val canSave = canContinueTiming && canContinueDuration && selectedMedicationIds.isNotEmpty()
    val sheetTitle = when (currentStep) {
        ScheduleFormStep.TIMING -> stringResource(Res.string.medical_routine_form_step_timing_title)
        ScheduleFormStep.DURATION -> stringResource(Res.string.medical_routine_form_step_duration_title)
        ScheduleFormStep.REMINDERS -> stringResource(Res.string.medical_routine_form_step_reminders_title)
    }
    val selectedMedicationNames = medications.filter { it.id in selectedMedicationIds }.joinToString { it.name }

    LaunchedEffect(currentStep) {
        when (currentStep) {
            ScheduleFormStep.TIMING -> nameFocusRequester.requestFocus()
            ScheduleFormStep.DURATION -> startDateFocusRequester.requestFocus()
            ScheduleFormStep.REMINDERS -> reminderFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    currentStep = when (currentStep) {
                        ScheduleFormStep.TIMING -> {
                            onCancel()
                            ScheduleFormStep.TIMING
                        }

                        ScheduleFormStep.DURATION -> ScheduleFormStep.TIMING

                        ScheduleFormStep.REMINDERS -> ScheduleFormStep.DURATION
                    }
                },
            ) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.common_back_desc),
                )
            }
            Text(
                sheetTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (routineToEdit != null && onDelete != null && currentStep == ScheduleFormStep.REMINDERS) {
                IconButton(onClick = { onDelete(routineToEdit.id) }) {
                    Icon(
                        Icons.DeleteW400Outlined,
                        contentDescription = stringResource(Res.string.medical_routine_form_archive),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        ScheduleStepHeader(
            step = currentStep,
            subtitle = subtitle,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (currentStep) {
                ScheduleFormStep.TIMING -> {
                    MedicalFormTextField(
                        label = stringResource(Res.string.medical_routine_form_name_label),
                        value = name,
                        onValueChange = { name = it },
                        textFieldModifier = Modifier.focusRequester(nameFocusRequester),
                        placeholder = stringResource(Res.string.medical_routine_form_name_placeholder),
                    )

                    SectionHeading(stringResource(Res.string.medical_routine_form_dose_times))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        timesOfDayMs.forEachIndexed { index, timeOfDayMs ->
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
                                    AssistChip(
                                        onClick = {
                                            editingTimeIndex = index
                                            showTimePicker = true
                                        },
                                        label = { Text(formatTimeOfDayMs(timeOfDayMs)) },
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            timesOfDayMs =
                                                timesOfDayMs.toMutableList().apply { removeAt(index) }.sorted()
                                        },
                                        enabled = timesOfDayMs.size > 1,
                                    ) {
                                        Icon(
                                            Icons.CloseW400Outlinedfill1,
                                            contentDescription = stringResource(
                                                Res.string.medical_routine_form_remove_time,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                editingTimeIndex = null
                                showTimePicker = true
                            },
                        ) {
                            Text(stringResource(Res.string.medical_routine_form_add_time))
                        }
                    }

                    SectionHeading(stringResource(Res.string.medical_routine_form_repeat))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = repeatType == RoutineRepeatType.DAILY,
                            onClick = { repeatType = RoutineRepeatType.DAILY },
                            label = { Text(stringResource(Res.string.medical_routine_repeat_daily)) },
                        )
                        FilterChip(
                            selected = repeatType == RoutineRepeatType.WEEKLY,
                            onClick = { repeatType = RoutineRepeatType.WEEKLY },
                            label = { Text(stringResource(Res.string.medical_routine_repeat_specific_days)) },
                        )
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
                }

                ScheduleFormStep.DURATION -> {
                    DateValueField(
                        label = stringResource(Res.string.medical_routine_form_start_date),
                        value = LocalDate.parse(startDate),
                        modifier = Modifier.focusRequester(startDateFocusRequester),
                        onClick = { activeDatePicker = "start" },
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
                            label = stringResource(Res.string.medical_routine_form_end_date),
                            value = LocalDate.parse(endDate),
                            onClick = { activeDatePicker = "end" },
                        )
                        if (invalidEndDate) {
                            Text(
                                stringResource(Res.string.medical_routine_form_end_date_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                ScheduleFormStep.REMINDERS -> {
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
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            modifier = Modifier.focusRequester(reminderFocusRequester),
                        )
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
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                selectedMedicationNames,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
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
                                            checked = medication.id in selectedMedicationIds,
                                            onCheckedChange = { checked ->
                                                selectedMedicationIds = selectedMedicationIds.toMutableSet().apply {
                                                    if (checked) add(medication.id) else remove(medication.id)
                                                }
                                            },
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(medication.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${medication.dosage} • ${medication.stockLabel()}",
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (routineToEdit != null) {
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
                                checked = status == RoutineStatus.ARCHIVED,
                                onCheckedChange = { status = if (it) RoutineStatus.ARCHIVED else RoutineStatus.ACTIVE },
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        ScheduleFormActions(
            step = currentStep,
            isEditing = routineToEdit != null,
            canContinueTiming = canContinueTiming,
            canContinueDuration = canContinueDuration,
            canSave = canSave,
            onContinueTiming = { currentStep = ScheduleFormStep.DURATION },
            onContinueDuration = { currentStep = ScheduleFormStep.REMINDERS },
            onSave = {
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
        )
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
private fun ScheduleStepHeader(step: ScheduleFormStep, subtitle: String?) {
    val stepNumber = when (step) {
        ScheduleFormStep.TIMING -> 1
        ScheduleFormStep.DURATION -> 2
        ScheduleFormStep.REMINDERS -> 3
    }
    val progress = stepNumber / 3f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(Res.string.medical_routine_form_step_counter, stepNumber),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        )
        if (step == ScheduleFormStep.TIMING && !subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ScheduleFormActions(
    step: ScheduleFormStep,
    isEditing: Boolean,
    canContinueTiming: Boolean,
    canContinueDuration: Boolean,
    canSave: Boolean,
    onContinueTiming: () -> Unit,
    onContinueDuration: () -> Unit,
    onSave: () -> Unit,
) {
    when (step) {
        ScheduleFormStep.TIMING -> {
            Button(
                onClick = onContinueTiming,
                enabled = canContinueTiming,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.medical_medication_form_continue))
            }
        }

        ScheduleFormStep.DURATION -> {
            Button(
                onClick = onContinueDuration,
                enabled = canContinueDuration,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.medical_medication_form_continue))
            }
        }

        ScheduleFormStep.REMINDERS -> {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (isEditing) {
                        stringResource(Res.string.medical_routine_form_update_button)
                    } else {
                        stringResource(Res.string.medical_routine_form_save_button)
                    },
                )
            }
        }
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
