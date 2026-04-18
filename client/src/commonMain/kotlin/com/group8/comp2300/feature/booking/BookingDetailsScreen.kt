package com.group8.comp2300.feature.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CalendarMonthW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CallW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LocationOnW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.StethoscopeW400Outlinedfill1
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun BookingDetailsScreen(
    clinicId: String,
    rescheduleAppointment: Appointment? = null,
    onBack: () -> Unit,
    onContinueToConfirmation: (String, String, Appointment?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val clinic = state.selectedClinic?.takeIf { it.id == clinicId } ?: viewModel.getClinic(clinicId)
    val selectedSlotId = state.bookingDraft?.takeIf { it.clinicId == clinicId }?.slotId
    val selectedSlot = state.availableSlots.firstOrNull { it.id == selectedSlotId }
    val groupedSlots = state.availableSlots.groupBy { slotDateKey(it) }.entries.toList()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(clinicId) {
        rescheduleAppointment?.let(viewModel::prepareReschedule)
        viewModel.loadClinicDetails(clinicId)
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearBookingError()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = { Text(if (rescheduleAppointment == null) "Clinic" else "Reschedule") },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
        bottomBar = {
            if (clinic != null && groupedSlots.isNotEmpty()) {
                SelectionBar(
                    selectedSlot = selectedSlot,
                    isReschedule = rescheduleAppointment != null,
                    onContinue = {
                        selectedSlot?.let { onContinueToConfirmation(clinic.id, it.id, rescheduleAppointment) }
                    },
                )
            }
        },
    ) { paddingValues ->
        when {
            state.isLoadingClinic || state.isLoadingSlots -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            clinic == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Not found")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item {
                        ClinicHero(clinic = clinic, slotCount = state.availableSlots.size)
                    }

                    item {
                        AvailabilityHeader(
                            hasSlots = groupedSlots.isNotEmpty(),
                            selectedSlot = selectedSlot,
                        )
                    }

                    if (groupedSlots.isEmpty()) {
                        item {
                            EmptyPanel(
                                title = "No slots",
                                body = "Try another clinic.",
                            )
                        }
                    } else {
                        groupedSlots.forEachIndexed { index, entry ->
                            item {
                                AvailabilityDaySection(
                                    dateKey = entry.key,
                                    slots = entry.value,
                                    selectedSlotId = selectedSlotId,
                                    onSelect = { slot -> viewModel.selectSlot(clinic.id, slot.id) },
                                )
                            }
                            if (index != groupedSlots.lastIndex) {
                                item {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(selectedSlot: AppointmentSlot?, isReschedule: Boolean, onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceBright,
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (selectedSlot == null) "Select a time" else "Selected time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = selectedSlot?.let { slotSummary(it.startTime) } ?: "Choose a slot to continue.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Button(
                onClick = onContinue,
                enabled = selectedSlot != null,
            ) {
                Text(if (isReschedule) "Review change" else "Continue")
            }
        }
    }
}

@Composable
private fun ClinicHero(clinic: Clinic, slotCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            ) {
                ClinicImage(
                    clinic = clinic,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = clinic.name,
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = clinic.formattedDistance,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).weight(1f),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = clinic.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = clinic.address ?: "Clinic details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    MetadataRow(
                        icon = Icons.CalendarMonthW400Outlinedfill1,
                        label = "Next slot",
                        value = slotSummary(clinic.nextAvailableSlot),
                        iconTint = MaterialTheme.colorScheme.primary,
                    )

                    clinic.phone?.takeIf(String::isNotBlank)?.let {
                        MetadataRow(
                            icon = Icons.CallW400Outlinedfill1,
                            label = "Phone",
                            value = it,
                            iconTint = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    clinic.address?.takeIf(String::isNotBlank)?.let {
                        MetadataRow(
                            icon = Icons.LocationOnW400Outlinedfill1,
                            label = "Address",
                            value = it,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                        )
                    }

                    val profileTags = buildList {
                        addAll(clinic.serviceTypes.map { it.displayName() })
                        if (clinic.tags.isNotEmpty()) addAll(clinic.tags.map { it.replaceFirstChar(Char::uppercase) })
                        clinic.pricingTier?.let { add(it.displayIcon()) }
                        if (clinic.inclusivityFlags.wheelchairAccessible) add("Wheelchair access")
                        if (clinic.inclusivityFlags.lgbtqFriendly) add("LGBTQ+ friendly")
                    }.distinct()

                    if (profileTags.isNotEmpty()) {
                        TagCloud(
                            icon = Icons.StethoscopeW400Outlinedfill1,
                            title = "Services",
                            tags = profileTags,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SummaryTile(
                            modifier = Modifier.weight(1f),
                            label = "Openings",
                            value = if (slotCount == 1) "1 slot" else "$slotCount slots",
                        )
                        SummaryTile(
                            modifier = Modifier.weight(1f),
                            label = "Booking",
                            value = "Live",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(icon: ImageVector, label: String, value: String, iconTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceBright,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = iconTint,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloud(icon: ImageVector, title: String, tags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { tag ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AvailabilityHeader(hasSlots: Boolean, selectedSlot: AppointmentSlot?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Available times",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = when {
                        !hasSlots -> "No live openings right now."

                        selectedSlot != null -> "Selected: ${slotSummary(
                            selectedSlot.startTime,
                        )} • ${slotDurationLabel(selectedSlot)}"

                        else -> "Pick a time. Most visits take 30 min."
                    },
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvailabilityDaySection(
    dateKey: String,
    slots: List<AppointmentSlot>,
    selectedSlotId: String?,
    onSelect: (AppointmentSlot) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = dateKey,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            slots.forEach { slot ->
                SlotTile(
                    slot = slot,
                    selected = slot.id == selectedSlotId,
                    onSelect = { onSelect(slot) },
                )
            }
        }
    }
}

@Composable
private fun SlotTile(slot: AppointmentSlot, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onSelect),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceBright
        },
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = slotTimeLabel(slot),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = slotDurationLabel(slot),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

internal fun slotSummary(timestamp: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${DateFormatter.formatDayMonthYear(
        dateTime.date,
    )} • ${DateFormatter.formatTime(dateTime.hour, dateTime.minute)}"
}

private fun slotDateKey(slot: AppointmentSlot): String {
    val dateTime = Instant.fromEpochMilliseconds(slot.startTime).toLocalDateTime(TimeZone.currentSystemDefault())
    return DateFormatter.formatDayMonthYear(dateTime.date)
}

private fun slotTimeLabel(slot: AppointmentSlot): String {
    val dateTime = Instant.fromEpochMilliseconds(slot.startTime).toLocalDateTime(TimeZone.currentSystemDefault())
    return DateFormatter.formatTime(dateTime.hour, dateTime.minute)
}

private fun slotDurationLabel(slot: AppointmentSlot): String {
    val minutes = ((slot.endTime - slot.startTime) / 60_000L).toInt().coerceAtLeast(1)
    return if (minutes < 60) {
        "$minutes min"
    } else {
        "${minutes / 60}h ${minutes % 60}m"
    }
}
