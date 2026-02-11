@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.medical

/* ------------------------------------------------------------------
 * Demo data
 * ------------------------------------------------------------------ */
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.mock.baseTimeSlots
import com.group8.comp2300.presentation.util.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import kotlin.time.Clock
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/* ------------------------------------------------------------------
 * Demo data
 * ------------------------------------------------------------------ */
// Base slots imported from shared mock data
val baseSlots = baseTimeSlots

/* ------------------------------------------------------------------
 * Screen
 * ------------------------------------------------------------------ */
@OptIn(ExperimentalMaterial3Api::class, kotlin.time.ExperimentalTime::class)
@Composable
fun BookingDetailsScreen(
    clinicId: String,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = koinViewModel()
) {
    val clinic = viewModel.getClinicById(clinicId)
    if (clinic == null) {
        // Handle case where clinic is not found
        onBack()
        return
    }
    val timeZone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(timeZone).date

    // Dynamic Mock Data: Generate taken slots relative to 'today'
    val takenSlots =
        remember(today) {
            mapOf(
                // Today: Some slots taken
                today.toString() to setOf("09:30 AM", "02:00 PM"),
                // Tomorrow: Fully booked (Demonstrates "No slots left")
                today.plus(1, DateTimeUnit.DAY).toString() to
                    baseSlots.toSet(),
                // Day after tomorrow: Morning busy
                today.plus(2, DateTimeUnit.DAY).toString() to setOf("09:00 AM", "09:30 AM", "10:00 AM")
            )
        }

    var selectedDate by remember { mutableStateOf(today) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var reasonText by remember { mutableStateOf("") }
    var showNotifyToggle by remember { mutableStateOf(false) }

    // Logic Fix: Default to empty set if date not found (meaning all slots free)
    val availableSlots =
        baseSlots.filter { slot ->
            val takenOnDate = takenSlots[selectedDate.toString()] ?: emptySet()
            !takenOnDate.contains(slot)
        }

    LaunchedEffect(selectedDate) {
        if (selectedTimeSlot !in availableSlots) selectedTimeSlot = null
        showNotifyToggle = availableSlots.isEmpty()
    }

    val focusManager = LocalFocusManager.current
    val contextString =
        when {
            selectedTimeSlot == null -> stringResource(Res.string.medical_booking_details_choose_time)
            reasonText.isBlank() -> stringResource(Res.string.medical_booking_details_add_reason)
            else -> stringResource(Res.string.medical_booking_details_confirm_format, "$85.00")
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.medical_booking_details_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.ArrowBackW400Outlinedfill1,
                            contentDescription = stringResource(Res.string.medical_booking_back_desc)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.medical_booking_details_total_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            stringResource(Res.string.medical_booking_details_price_format, "85.00"),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        enabled = selectedTimeSlot != null && reasonText.isNotBlank(),
                        modifier = Modifier.width(200.dp)
                    ) {
                        Text(contextString)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // REFACTORED: Now showing Clinic details
            item { ClinicSummaryHeader(clinic) }

            item {
                Column {
                    SectionHeader(
                        stringResource(Res.string.medical_booking_details_select_date),
                        Icons.DateRangeW400Outlinedfill1
                    )
                    Spacer(Modifier.height(12.dp))
                    CalendarGrid(
                        baseDate = today,
                        selectedDate = selectedDate,
                        takenSlots = takenSlots,
                        onDateSelect = { selectedDate = it }
                    )
                }
            }

            item {
                Column {
                    SectionHeader(
                        stringResource(Res.string.medical_booking_details_select_time),
                        Icons.DateRangeW400Outlinedfill1
                    )
                    Spacer(Modifier.height(12.dp))
                    if (availableSlots.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Text(
                                stringResource(Res.string.medical_booking_details_no_slots),
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Checkbox(checked = showNotifyToggle, onCheckedChange = { showNotifyToggle = it })
                            Text(stringResource(Res.string.medical_booking_details_notify_on_slot))
                        }
                    } else {
                        TimeGrid(
                            availableSlots,
                            selectedTimeSlot
                        ) { selectedTimeSlot = it }
                    }
                }
            }

            item {
                Column {
                    SectionHeader(
                        stringResource(Res.string.medical_booking_details_reason_title),
                        Icons.InfoW400Outlined
                    )
                    Spacer(Modifier.height(12.dp))
                    var expanded by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        modifier = Modifier.fillMaxWidth().height(if (expanded) 100.dp else 56.dp),
                        placeholder = { Text(stringResource(Res.string.medical_booking_details_reason_placeholder)) },
                        singleLine = !expanded,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (!expanded && reasonText.isBlank()) {
                        Text(
                            stringResource(Res.string.medical_booking_details_expand_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    LaunchedEffect(reasonText) { expanded = reasonText.length > 30 || reasonText.contains("\n") }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------
 * Sub-components
 * ------------------------------------------------------------------ */
@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// NEW: Clinic Header instead of Doctor Header
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClinicSummaryHeader(clinic: Clinic) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(64.dp)
                        .clip(RoundedCornerShape(12.dp)) // Square for buildings/locations
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.LocationOnW400Outlinedfill1,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(clinic.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))

                // Location / Tags
                Text(
                    "${clinic.formattedDistance} • ${clinic.tags.joinToString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.StarW500Outlined, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.medical_booking_details_verified_clinic),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                stringResource(Res.string.medical_booking_details_price_format, "85.00"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* ------------------------------------------------------------------
 * Calendar using kotlinx-datetime
 * ------------------------------------------------------------------ */
@Composable
private fun CalendarGrid(
    baseDate: LocalDate,
    selectedDate: LocalDate,
    takenSlots: Map<String, Set<String>>,
    onDateSelect: (LocalDate) -> Unit
) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val displayDate = baseDate.plus(monthOffset, DateTimeUnit.MONTH)
    val firstOfMonth = LocalDate(displayDate.year, displayDate.month.number, 1)
    val daysInMonth = displayDate.yearMonth.numberOfDays
    val startOffset = (firstOfMonth.dayOfWeek.isoDayNumber - 1) % 7

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset-- }) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_booking_details_prev_month)
                )
            }
            Text(
                DateFormatter.formatMonthYear(displayDate),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { monthOffset++ }) {
                Icon(
                    Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = stringResource(Res.string.medical_booking_details_next_month),
                    modifier = Modifier.rotate(180f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                Res.string.day_initial_mon,
                Res.string.day_initial_tue,
                Res.string.day_initial_wed,
                Res.string.day_initial_thu,
                Res.string.day_initial_fri,
                Res.string.day_initial_sat,
                Res.string.day_initial_sun
            )
                .forEach {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
        }
        Spacer(Modifier.height(4.dp))
        val firstCalDate = firstOfMonth.minus(startOffset, DateTimeUnit.DAY)
        val totalCells = ((daysInMonth + startOffset + 6) / 7) * 7
        val dates: List<LocalDate> = (0 until totalCells).map { firstCalDate.plus(it, DateTimeUnit.DAY) }

        dates.chunked(7).forEach { weekDates ->
            Row(Modifier.fillMaxWidth()) {
                weekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isPast = date < baseDate
                    val isCurrentMonth = date.month == displayDate.month
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary

                                        isPast || !isCurrentMonth ->
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = !isPast && isCurrentMonth) { onDateSelect(date) }
                                .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                date.day.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                            )
                            if (takenSlots[date.toString()]?.size ==
                                baseSlots.size
                            ) {
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    modifier =
                                        Modifier.size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------
 * Time grid
 * ------------------------------------------------------------------ */
@Composable
private fun TimeGrid(slots: List<String>, selected: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { slot ->
                    val isSelected = slot == selected
                    val isAm = slot.endsWith("AM") && slot != "12:00 PM"

                    // IMPROVEMENT: Use MaterialTheme colors instead of
                    // hardcoded hex values for
                    // better Dark Mode support.
                    val unselectedContainer =
                        if (isAm) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }

                    val containerColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            unselectedContainer
                        }
                    val contentColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    val borderColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }

                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(containerColor)
                                .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(8.dp))
                                .clickable { onSelect(slot) }
                                .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            slot,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = contentColor
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
