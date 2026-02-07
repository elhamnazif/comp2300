@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.medical

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.presentation.util.DateFormatter
import comp2300.i18n.generated.resources.*
import comp2300.i18n.generated.resources.Res
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Instant

// --- Mock Data Models (Replacing external imports for self-contained file) ---

enum class AdherenceStatus {
    TAKEN,
    MISSED,
    NONE,
    APPOINTMENT,
}

data class CalendarDay(
    val dayOfMonth: Int,
    val date: LocalDate,
    val status: AdherenceStatus,
    val isToday: Boolean,
    /** Added to visually dim days from prev/next months */
    val isCurrentMonth: Boolean,
)

data class Appointment(val id: String, val title: String, val type: String, val date: String, val time: String)

data class Doctor(val name: String)

// --- Mock Data Generators ---

// --- Mock Data Generators ---
// Imported from shared/mock/CalendarViewData.kt
val sampleAppointments =
    com.group8.comp2300.mock.sampleCalendarAppointments.map {
        Appointment(it.id, it.title, it.type, it.date, it.time)
    }

val sampleDoctors = com.group8.comp2300.mock.sampleCalendarDoctors.map { Doctor(it.name) }

// --- Helper Functions ---

/**
 * Generates a 6-week (42 day) grid for the given month/year. Includes padding days from previous
 * and next months.
 */
fun generateCalendarDays(year: Int, month: Month): List<CalendarDay> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val firstOfMonth = LocalDate(year, month, 1)

    // Calculate start offset (Sunday start)
    // ISO: Mon=1 ... Sun=7. We want Sun=0, Mon=1.
    val startOffset = firstOfMonth.dayOfWeek.isoDayNumber % 7

    val firstDayOfGrid = firstOfMonth.minus(startOffset, DateTimeUnit.DAY)

    // Generate 42 days (6 weeks) to ensure consistent grid size
    return (0 until 42).map { offset ->
        val date = firstDayOfGrid.plus(offset, DateTimeUnit.DAY)
        val isToday = date == today
        val isCurrentMonth = date.month == month

        // Mocking status logic - in a real app, check DB here
        val status =
            when {
                date == today -> AdherenceStatus.NONE
                !isCurrentMonth -> AdherenceStatus.NONE
                date.day % 5 == 0 -> AdherenceStatus.TAKEN
                date.day % 7 == 0 -> AdherenceStatus.MISSED
                else -> AdherenceStatus.NONE
            }

        CalendarDay(
            dayOfMonth = date.day,
            date = date,
            status = status,
            isToday = isToday,
            isCurrentMonth = isCurrentMonth,
        )
    }
}

// --- Constants & Enums ---

private enum class SheetView {
    MENU,
    FORM_MED,
    FORM_APPT,
    FORM_MOOD,
    DETAILS_APPT,
}

private object FormConstants {
    @Composable
    fun commonMeds() = listOf(
        stringResource(Res.string.medication_prep),
        stringResource(Res.string.medication_truvada),
        stringResource(Res.string.medication_descovy),
        stringResource(Res.string.medication_doxypep),
        stringResource(Res.string.medication_multivitamin),
    )

    @Composable
    fun apptTypes() = listOf(
        stringResource(Res.string.appt_type_consultation),
        stringResource(Res.string.appt_type_labwork),
        stringResource(Res.string.appt_type_followup),
    )

    val Dosages = listOf(1, 2, 3)
    val MoodEmojis = listOf("😢", "😕", "😐", "🙂", "🤩")

    @Composable
    fun moodLabels() = listOf(
        stringResource(Res.string.form_mood_very_sad),
        stringResource(Res.string.form_mood_sad),
        stringResource(Res.string.form_mood_neutral),
        stringResource(Res.string.form_mood_happy),
        stringResource(Res.string.form_mood_great),
    )

    @Composable
    fun moodTags() = listOf(
        stringResource(Res.string.form_mood_tag_anxious),
        stringResource(Res.string.form_mood_tag_calm),
        stringResource(Res.string.form_mood_tag_irritable),
        stringResource(Res.string.form_mood_tag_energetic),
        stringResource(Res.string.form_mood_tag_tired),
        stringResource(Res.string.form_mood_tag_stressed),
        stringResource(Res.string.form_mood_tag_focused),
    )
}

// --- Main Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    // UI State
    var isMedicationTaken by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentSheetView by remember { mutableStateOf(SheetView.MENU) }

    // Selection State
    var selectedDateForEntry by remember { mutableStateOf<CalendarDay?>(null) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }

    // Editable Form State
    var entryDate by remember { mutableStateOf(today) }
    var entryTime by remember { mutableStateOf(9 to 0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val closeSheet = {
        showBottomSheet = false
        selectedAppointment = null
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Default to today if nothing selected
                    val defaultDay =
                        CalendarDay(today.day, today, AdherenceStatus.NONE, isToday = true, isCurrentMonth = true)
                    val dateToUse = selectedDateForEntry ?: defaultDay

                    selectedDateForEntry = dateToUse
                    entryDate = dateToUse.date
                    val now =
                        Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    entryTime = now.hour to now.minute

                    currentSheetView = SheetView.MENU
                    selectedAppointment = null
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.AddW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.calendar_add_entry_desc),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.calendar_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Updated Calendar Card with Navigation Logic
            item {
                CalendarCard(
                    baseDate = today,
                    isMedicationTaken = isMedicationTaken,
                    selectedDate = selectedDateForEntry,
                    onDayClick = { day ->
                        selectedDateForEntry = day
                        entryDate = day.date
                        val now =
                            Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                        entryTime = now.hour to now.minute

                        currentSheetView = SheetView.MENU
                        selectedAppointment = null
                        showBottomSheet = true
                    },
                )
            }

            item {
                Column {
                    Text(
                        text = stringResource(Res.string.calendar_today_action),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    DailyActionCard(
                        isTaken = isMedicationTaken,
                        onToggle = { isMedicationTaken = !isMedicationTaken },
                    )
                }
            }

            item {
                Text(
                    stringResource(Res.string.calendar_upcoming),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            items(sampleAppointments) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onClick = {
                        selectedAppointment = appointment
                        currentSheetView = SheetView.DETAILS_APPT
                        showBottomSheet = true
                    },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = closeSheet,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                AnimatedContent(targetState = currentSheetView, label = "SheetTransition") { view ->
                    when (view) {
                        SheetView.MENU -> AddEntryMenu(onSelectType = { currentSheetView = it })

                        SheetView.FORM_MED ->
                            WrapperFormLayout(
                                title = stringResource(Res.string.form_medication_title),
                                entryDate = entryDate,
                                entryTime = entryTime,
                                onDateChange = { entryDate = it },
                                onTimeChange = { h, m -> entryTime = h to m },
                                onBack = { currentSheetView = SheetView.MENU },
                                content = {
                                    MedicationForm({ name, extras ->
                                        println("Saved: $name, $extras")
                                        closeSheet()
                                    })
                                },
                            )

                        SheetView.FORM_APPT ->
                            WrapperFormLayout(
                                title = stringResource(Res.string.form_appt_title),
                                entryDate = entryDate,
                                entryTime = entryTime,
                                onDateChange = { entryDate = it },
                                onTimeChange = { h, m -> entryTime = h to m },
                                onBack = { currentSheetView = SheetView.MENU },
                                content = {
                                    AppointmentForm({ doc, extras ->
                                        println("Saved: $doc, $extras")
                                        closeSheet()
                                    })
                                },
                            )

                        SheetView.FORM_MOOD ->
                            WrapperFormLayout(
                                title = stringResource(Res.string.form_mood_title),
                                entryDate = entryDate,
                                entryTime = entryTime,
                                onDateChange = { entryDate = it },
                                onTimeChange = { h, m -> entryTime = h to m },
                                onBack = { currentSheetView = SheetView.MENU },
                                content = {
                                    MoodEntryForm({ score, tags, symptoms, notes ->
                                        println("Mood: $score, $tags, $symptoms, $notes")
                                        closeSheet()
                                    })
                                },
                            )

                        SheetView.DETAILS_APPT ->
                            selectedAppointment?.let { appt ->
                                AppointmentDetailSheetContent(
                                    appt,
                                    onEdit = {},
                                    onDelete = { closeSheet() },
                                )
                            }
                    }
                }
            }
        }
    }
}

// --- UPDATED CALENDAR COMPONENT ---

@Composable
fun CalendarCard(
    baseDate: LocalDate,
    isMedicationTaken: Boolean,
    selectedDate: CalendarDay?,
    onDayClick: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    // State for navigation
    var monthOffset by remember { mutableIntStateOf(0) }

    // Calculate the month to display based on offset
    val displayDate =
        remember(baseDate, monthOffset) { baseDate.plus(monthOffset, DateTimeUnit.MONTH) }

    // Generate grid data dynamically for the displayed month
    val calendarDays =
        remember(displayDate) { generateCalendarDays(displayDate.year, displayDate.month) }

    Card(
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header with Navigation
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.calendar_prev_month_desc),
                    )
                }

                Text(
                    text = DateFormatter.formatMonthYear(displayDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(onClick = { monthOffset++ }) {
                    Icon(
                        Icons.ArrowBackW400Outlinedfill1,
                        contentDescription = stringResource(Res.string.calendar_next_month_desc),
                        modifier = Modifier.rotate(180f),
                    )
                }
            }

            // Days of Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(
                    stringResource(Res.string.day_initial_sun),
                    stringResource(Res.string.day_initial_mon),
                    stringResource(Res.string.day_initial_tue),
                    stringResource(Res.string.day_initial_wed),
                    stringResource(Res.string.day_initial_thu),
                    stringResource(Res.string.day_initial_fri),
                    stringResource(Res.string.day_initial_sat),
                ).forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Calendar Grid
            val weeks = calendarDays.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(
                                day = day,
                                todayTaken = isMedicationTaken,
                                isSelected = selectedDate?.date == day.date,
                                onClick = { onDayClick(day) },
                            )
                        }
                    }
                    if (week.size < 7) {
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: CalendarDay,
    todayTaken: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actualStatus = if (day.isToday && todayTaken) AdherenceStatus.TAKEN else day.status

    // Dim styling for days not in the current month
    val alpha = if (day.isCurrentMonth) 1f else 0.3f

    val borderColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primary
            day.isToday -> MaterialTheme.colorScheme.primary.copy(0.5f)
            else -> Color.Transparent
        }

    val backgroundColor =
        if (day.isToday) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent

    val statusDotColor =
        when (actualStatus) {
            AdherenceStatus.TAKEN -> Color(0xFF4CAF50)
            AdherenceStatus.MISSED -> MaterialTheme.colorScheme.error
            AdherenceStatus.APPOINTMENT -> Color(0xFFD4AF37)
            else -> Color.Transparent
        }

    Box(
        modifier =
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                if (isSelected) 2.dp else 1.dp,
                borderColor,
                RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .alpha(alpha),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight =
                if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            if (actualStatus != AdherenceStatus.NONE && day.isCurrentMonth) {
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusDotColor))
            }
        }
    }
}

// --- REUSABLE FORM COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelectOption: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier =
            Modifier.fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectOption(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormChipGroup(
    title: String,
    options: List<String>,
    selectedOptions: List<String>,
    onOptionToggle: (String) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                FilterChip(
                    selected = isSelected,
                    onClick = { onOptionToggle(option) },
                    label = { Text(option) },
                    leadingIcon =
                    if (isSelected) {
                        { Icon(Icons.CheckW400Outlinedfill1, null, Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}

// --- FORM IMPLEMENTATIONS ---

@Composable
fun MedicationForm(onSave: (String, Map<String, Any>) -> Unit, modifier: Modifier = Modifier) {
    val meds = FormConstants.commonMeds()
    var name by remember(meds) { mutableStateOf(meds.firstOrNull() ?: "") }
    var dosage by remember { mutableIntStateOf(1) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormDropdown(
            label = stringResource(Res.string.form_medication_label),
            options = FormConstants.commonMeds(),
            selectedOption = name,
            onSelectOption = { name = it },
        )

        Column {
            Text(stringResource(Res.string.form_dosage_label), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormConstants.Dosages.forEach { count ->
                    FilterChip(
                        selected = dosage == count,
                        onClick = { dosage = count },
                        label = {
                            Text(
                                pluralStringResource(
                                    Res.plurals.form_dosage_pill,
                                    count,
                                    count,
                                ),
                            )
                        },
                        leadingIcon = {
                            if (dosage == count) {
                                Icon(Icons.CheckW400Outlinedfill1, null, Modifier.size(16.dp))
                            }
                        },
                    )
                }
            }
        }

        Button(
            onClick = { onSave(name, mapOf("dosage" to dosage)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotEmpty(),
        ) { Text(stringResource(Res.string.form_medication_title)) }
    }
}

@Composable
fun AppointmentForm(onSave: (String, Map<String, Any>) -> Unit, modifier: Modifier = Modifier) {
    var doctorName by remember { mutableStateOf("") }
    val apptTypes = FormConstants.apptTypes()
    var type by remember(apptTypes) { mutableStateOf(apptTypes.firstOrNull() ?: "") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FormDropdown(
            label = stringResource(Res.string.form_appt_doctor_label),
            options = sampleDoctors.map { it.name },
            selectedOption = doctorName,
            onSelectOption = { doctorName = it },
        )

        Column {
            Text(stringResource(Res.string.form_appt_type_label), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormConstants.apptTypes().forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t) },
                        leadingIcon = {
                            if (type == t) Icon(Icons.CheckW400Outlinedfill1, null, Modifier.size(16.dp))
                        },
                    )
                }
            }
        }

        Button(
            onClick = { onSave(doctorName, mapOf("type" to type)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = doctorName.isNotEmpty(),
        ) { Text(stringResource(Res.string.form_appt_schedule_button)) }
    }
}

@Composable
fun MoodEntryForm(onSave: (Int, List<String>, List<String>, String) -> Unit, modifier: Modifier = Modifier) {
    var moodScore by remember { mutableIntStateOf(3) }
    val selectedTags = remember { mutableStateListOf<String>() }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 1. Mood Slider
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
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FormConstants.MoodEmojis.forEachIndexed { index, emoji ->
                    val score = index + 1
                    val isSelected = moodScore == score
                    val scale by
                        animateFloatAsState(if (isSelected) 1.5f else 1.0f, label = "scale")

                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier =
                        Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                            .clickable(
                                interactionSource =
                                remember { MutableInteractionSource() },
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

        // 2. Feelings Tags
        FormChipGroup(
            title = stringResource(Res.string.form_mood_question),
            options = FormConstants.moodTags(),
            selectedOptions = selectedTags,
            onOptionToggle = {
                if (selectedTags.contains(it)) selectedTags.remove(it) else selectedTags.add(it)
            },
        )

        // 4. Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(Res.string.form_mood_journal_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Button(
            onClick = { onSave(moodScore, selectedTags, selectedSymptoms, notes) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(Res.string.form_mood_log_button)) }
    }
}

// --- LAYOUT WRAPPERS & MENUS ---

@Composable
private fun AddEntryMenu(onSelectType: (SheetView) -> Unit) {
    Column(
        modifier =
        Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(Res.string.calendar_menu_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        MenuSelectionCard(
            title = stringResource(Res.string.calendar_menu_log_med),
            subtitle = stringResource(Res.string.calendar_menu_log_med_desc),
            icon = Icons.EditW400Outlinedfill1,
            color = MaterialTheme.colorScheme.primaryContainer,
            onClick = { onSelectType(SheetView.FORM_MED) },
        )
        MenuSelectionCard(
            title = stringResource(Res.string.calendar_menu_track_appt),
            subtitle = stringResource(Res.string.calendar_menu_track_appt_desc),
            icon = Icons.DateRangeW400Outlinedfill1,
            color = MaterialTheme.colorScheme.secondaryContainer,
            onClick = { onSelectType(SheetView.FORM_APPT) },
        )
        MenuSelectionCard(
            title = stringResource(Res.string.calendar_menu_track_mood),
            subtitle = stringResource(Res.string.calendar_menu_track_mood_desc),
            icon = Icons.FaceW400Outlined,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            onClick = { onSelectType(SheetView.FORM_MOOD) },
        )
    }
}

@Composable
fun MenuSelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                Modifier.size(48.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrapperFormLayout(
    title: String,
    entryDate: LocalDate,
    entryTime: Pair<Int, Int>,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
            entryDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
    val timePickerState =
        rememberTimePickerState(initialHour = entryTime.first, initialMinute = entryTime.second)

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.common_back_desc),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            OutlinedTextField(
                value = DateFormatter.formatDayMonthYear(entryDate),
                onValueChange = {},
                label = { Text(stringResource(Res.string.form_date_label)) },
                modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                enabled = false,
                colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                ),
                trailingIcon = { Icon(Icons.DateRangeW400Outlinedfill1, null) },
            )
            OutlinedTextField(
                value = DateFormatter.formatTime(entryTime.first, entryTime.second),
                onValueChange = {},
                label = { Text(stringResource(Res.string.form_time_label)) },
                modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                enabled = false,
                colors =
                OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                ),
                trailingIcon = { Icon(Icons.CheckCircleW400Outlinedfill1, null) },
            )
        }

        content()

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                onDateChange(
                                    Instant.fromEpochMilliseconds(it)
                                        .toLocalDateTime(TimeZone.UTC)
                                        .date,
                                )
                            }
                            showDatePicker = false
                        },
                    ) { Text(stringResource(Res.string.common_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.common_cancel)) }
                },
            ) { DatePicker(state = datePickerState) }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onTimeChange(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        },
                    ) { Text(stringResource(Res.string.common_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text(stringResource(Res.string.common_cancel)) }
                },
                text = { TimePicker(state = timePickerState) },
            )
        }
    }
}

// --- BASIC UI COMPONENTS ---

@Composable
fun AppointmentDetailSheetContent(
    appointment: Appointment,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.calendar_appt_details_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.EditW400Outlinedfill1,
                    stringResource(Res.string.calendar_appt_edit_desc),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Card(
            colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    appointment.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(appointment.type, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        DetailRow(Icons.DateRangeW400Outlinedfill1, stringResource(Res.string.form_date_label), appointment.date)
        Spacer(Modifier.height(16.dp))
        DetailRow(Icons.CheckCircleW400Outlinedfill1, stringResource(Res.string.form_time_label), appointment.time)
        Spacer(Modifier.height(16.dp))
        DetailRow(
            Icons.LocationOnW400Outlined,
            stringResource(Res.string.calendar_appt_location_label),
            stringResource(Res.string.calendar_appt_location_value),
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = {
        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.calendar_appt_directions_button)) }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors =
            ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Icon(Icons.DeleteW400Outlined, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.calendar_appt_cancel_button))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
            Modifier.size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun DailyActionCard(isTaken: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
        colors =
        CardDefaults.cardColors(
            containerColor =
            if (isTaken) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isTaken) {
                        stringResource(
                            Res.string.calendar_action_all_done,
                        )
                    } else {
                        stringResource(Res.string.calendar_action_take_prep)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (isTaken) {
                        stringResource(Res.string.calendar_action_streak_format, 12)
                    } else {
                        stringResource(Res.string.calendar_action_scheduled_format, "9:00 AM")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                if (isTaken) Icons.CheckCircleW400Outlinedfill1 else Icons.CheckCircleW400Outlined,
                null,
                tint =
                if (isTaken) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    val day = appointment.date.split(" ").getOrNull(1) ?: appointment.date.take(3)
    val month = appointment.date.split(" ").getOrNull(0) ?: ""
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
            Modifier.background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp),
            )
                .padding(vertical = 8.dp, horizontal = 12.dp),
        ) {
            Text(
                month,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                appointment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${appointment.type} • ${appointment.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
