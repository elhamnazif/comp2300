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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// --- Mock Data Models (Replacing external imports for self-contained file) ---

enum class AdherenceStatus {
    TAKEN,
    MISSED,
    NONE,
    APPOINTMENT
}

data class CalendarDay(
    val dayOfMonth: Int,
    val date: LocalDate,
    val status: AdherenceStatus,
    val isToday: Boolean,
    val isCurrentMonth: Boolean // Added to visually dim days from prev/next months
)

data class Appointment(
    val id: String,
    val title: String,
    val type: String,
    val date: String,
    val time: String
)

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

private fun LocalDate.formatToDisplay(): String =
    "${day.toString().padStart(2, '0')}/${month.number.toString().padStart(2, '0')}/${year}"

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour == 0 || hour == 12) 12 else hour % 12
    return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
}

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
            isCurrentMonth = isCurrentMonth
        )
    }
}

// --- Constants & Enums ---

private enum class SheetView {
    MENU,
    FORM_MED,
    FORM_APPT,
    FORM_MOOD,
    DETAILS_APPT
}

private object FormConstants {
    val CommonMeds = listOf("PrEP", "Truvada", "Descovy", "DoxyPEP", "Multivitamin")
    val ApptTypes = listOf("Consultation", "Lab Work", "Follow-up")
    val Dosages = listOf(1, 2, 3)
    val MoodEmojis = listOf("😢", "😕", "😐", "🙂", "🤩")
    val MoodLabels = listOf("Very Sad", "Sad", "Neutral", "Happy", "Great")
    val MoodTags =
        listOf("Anxious", "Calm", "Irritable", "Energetic", "Tired", "Stressed", "Focused")
}

// --- Main Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(isGuest: Boolean = false, onRequireAuth: () -> Unit = {}) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Default to today if nothing selected
                    val defaultDay =
                        CalendarDay(today.day, today, AdherenceStatus.NONE, true, true)
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
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, contentDescription = "Add Entry") }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Your Schedule",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
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
                    }
                )
            }

            item {
                Column {
                    Text(
                        text = "Today's Action",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DailyActionCard(
                        isTaken = isMedicationTaken,
                        onToggle = { isMedicationTaken = !isMedicationTaken }
                    )
                }
            }

            item {
                Text(
                    "Upcoming",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            items(sampleAppointments) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onClick = {
                        selectedAppointment = appointment
                        currentSheetView = SheetView.DETAILS_APPT
                        showBottomSheet = true
                    }
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
                                title = "Log Medication",
                                entryDate = entryDate,
                                entryTime = entryTime,
                                onDateChange = { entryDate = it },
                                onTimeChange = { h, m -> entryTime = h to m },
                                onBack = { currentSheetView = SheetView.MENU }
                            ) {
                                MedicationForm { name, extras ->
                                    println("Saved: $name, $extras")
                                    closeSheet()
                                }
                            }

                        SheetView.FORM_APPT ->
                            WrapperFormLayout(
                                title = "Track Appointment",
                                entryDate = entryDate,
                                entryTime = entryTime,
                                onDateChange = { entryDate = it },
                                onTimeChange = { h, m -> entryTime = h to m },
                                onBack = { currentSheetView = SheetView.MENU }
                            ) {
                                AppointmentForm { doc, extras ->
                                    println("Saved: $doc, $extras")
                                    closeSheet()
                                }
                            }

                        SheetView.FORM_MOOD ->
                            WrapperFormLayout(
                                title = "Track Mood",
                                entryDate = entryDate,
                                entryTime = entryTime,
                                onDateChange = { entryDate = it },
                                onTimeChange = { h, m -> entryTime = h to m },
                                onBack = { currentSheetView = SheetView.MENU }
                            ) {
                                MoodEntryForm { score, tags, symptoms, notes ->
                                    println("Mood: $score, $tags, $symptoms, $notes")
                                    closeSheet()
                                }
                            }

                        SheetView.DETAILS_APPT ->
                            selectedAppointment?.let { appt ->
                                AppointmentDetailSheetContent(
                                    appt,
                                    onEdit = {},
                                    onDelete = { closeSheet() }
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
    onDayClick: (CalendarDay) -> Unit
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
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header with Navigation
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                }

                Text(
                    text =
                        "${displayDate.month.name.lowercase().replaceFirstChar { it.titlecase() }} ${displayDate.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(onClick = { monthOffset++ }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Next Month",
                        modifier = Modifier.rotate(180f)
                    )
                }
            }

            // Days of Week Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Calendar Grid
            val weeks = calendarDays.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            DayCell(
                                day = day,
                                todayTaken = isMedicationTaken,
                                isSelected = selectedDate?.date == day.date,
                                onClick = { onDayClick(day) }
                            )
                        }
                    }
                    if (week.size < 7)
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun DayCell(day: CalendarDay, todayTaken: Boolean, isSelected: Boolean, onClick: () -> Unit) {
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
            Modifier.aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    if (isSelected) 2.dp else 1.dp,
                    borderColor,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onClick() }
                .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight =
                    if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
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
    onOptionSelected: (String) -> Unit
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
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOptions.contains(option)
                FilterChip(
                    selected = isSelected,
                    onClick = { onOptionToggle(option) },
                    label = { Text(option) },
                    leadingIcon =
                        if (isSelected) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null,
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
    }
}

// --- FORM IMPLEMENTATIONS ---

@Composable
fun MedicationForm(onSave: (String, Map<String, Any>) -> Unit) {
    var name by remember { mutableStateOf("PrEP") }
    var dosage by remember { mutableIntStateOf(1) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FormDropdown(
            label = "Medication",
            options = FormConstants.CommonMeds,
            selectedOption = name,
            onOptionSelected = { name = it }
        )

        Column {
            Text("Dosage", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormConstants.Dosages.forEach { count ->
                    FilterChip(
                        selected = dosage == count,
                        onClick = { dosage = count },
                        label = { Text("$count pill${if (count > 1) "s" else ""}") },
                        leadingIcon = {
                            if (dosage == count)
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        Button(
            onClick = { onSave(name, mapOf("dosage" to dosage)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotEmpty()
        ) { Text("Log Medication") }
    }
}

@Composable
fun AppointmentForm(onSave: (String, Map<String, Any>) -> Unit) {
    var doctorName by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(FormConstants.ApptTypes.first()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FormDropdown(
            label = "Doctor / Clinic",
            options = sampleDoctors.map { it.name },
            selectedOption = doctorName,
            onOptionSelected = { doctorName = it }
        )

        Column {
            Text("Type", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormConstants.ApptTypes.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(t) },
                        leadingIcon = {
                            if (type == t) Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }
                    )
                }
            }
        }

        Button(
            onClick = { onSave(doctorName, mapOf("type" to type)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = doctorName.isNotEmpty()
        ) { Text("Schedule Appointment") }
    }
}

@Composable
fun MoodEntryForm(onSave: (Int, List<String>, List<String>, String) -> Unit) {
    var moodScore by remember { mutableIntStateOf(3) }
    val selectedTags = remember { mutableStateListOf<String>() }
    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var notes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // 1. Mood Slider
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = FormConstants.MoodLabels[moodScore - 1],
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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
                                    indication = null
                                ) { moodScore = score }
                    )
                }
            }
            Slider(
                value = moodScore.toFloat(),
                onValueChange = { moodScore = it.toInt() },
                valueRange = 1f..5f,
                steps = 3
            )
        }

        HorizontalDivider()

        // 2. Feelings Tags
        FormChipGroup(
            title = "How are you feeling?",
            options = FormConstants.MoodTags,
            selectedOptions = selectedTags,
            onOptionToggle = {
                if (selectedTags.contains(it)) selectedTags.remove(it) else selectedTags.add(it)
            }
        )

        // 4. Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Journal / Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = { onSave(moodScore, selectedTags, selectedSymptoms, notes) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Log Mood") }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "What would you like to add?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        MenuSelectionCard(
            "Log Medication",
            "Track your daily intake",
            Icons.Default.Edit,
            MaterialTheme.colorScheme.primaryContainer
        ) { onSelectType(SheetView.FORM_MED) }
        MenuSelectionCard(
            "Track Appointment",
            "Track your visit or lab work",
            Icons.Default.DateRange,
            MaterialTheme.colorScheme.secondaryContainer
        ) { onSelectType(SheetView.FORM_APPT) }
        MenuSelectionCard(
            "Track Mood",
            "Log how you're feeling today",
            Icons.Outlined.Face,
            MaterialTheme.colorScheme.tertiaryContainer
        ) { onSelectType(SheetView.FORM_MOOD) }
    }
}

@Composable
fun MenuSelectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(48.dp)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    content: @Composable () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                entryDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        )
    val timePickerState =
        rememberTimePickerState(initialHour = entryTime.first, initialMinute = entryTime.second)

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            OutlinedTextField(
                value = entryDate.formatToDisplay(),
                onValueChange = {},
                label = { Text("Date") },
                modifier = Modifier.weight(1f).clickable { showDatePicker = true },
                enabled = false,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                trailingIcon = { Icon(Icons.Default.DateRange, null) }
            )
            OutlinedTextField(
                value = formatTime(entryTime.first, entryTime.second),
                onValueChange = {},
                label = { Text("Time") },
                modifier = Modifier.weight(1f).clickable { showTimePicker = true },
                enabled = false,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                trailingIcon = { Icon(Icons.Default.CheckCircle, null) }
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
                                        .date
                                )
                            }
                            showDatePicker = false
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
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
                        }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                },
                text = { TimePicker(state = timePickerState) }
            )
        }
    }
}

// --- BASIC UI COMPONENTS ---

@Composable
fun AppointmentDetailSheetContent(
    appointment: Appointment,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier =
            Modifier.navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Appointment Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(24.dp))
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    appointment.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(appointment.type, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(24.dp))
        DetailRow(Icons.Default.DateRange, "Date", appointment.date)
        Spacer(Modifier.height(16.dp))
        DetailRow(Icons.Default.CheckCircle, "Time", appointment.time)
        Spacer(Modifier.height(16.dp))
        DetailRow(Icons.Outlined.LocationOn, "Location", "Room 304, Sexual Health Clinic")
        Spacer(Modifier.height(32.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Get Directions") }
        TextButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
        ) {
            Icon(Icons.Outlined.Delete, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cancel Appointment")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun DailyActionCard(isTaken: Boolean, onToggle: () -> Unit) {
    Card(
        onClick = onToggle,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isTaken) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isTaken) "All done!" else "Take PrEP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isTaken) "Streak: 12 Days" else "Scheduled for 9:00 AM",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                if (isTaken) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                null,
                tint =
                    if (isTaken) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun AppointmentCard(appointment: Appointment, onClick: () -> Unit = {}) {
    val day = appointment.date.split(" ").getOrNull(1) ?: appointment.date.take(3)
    val month = appointment.date.split(" ").getOrNull(0) ?: ""
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(8.dp)
                )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                month,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                appointment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${appointment.type} • ${appointment.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
