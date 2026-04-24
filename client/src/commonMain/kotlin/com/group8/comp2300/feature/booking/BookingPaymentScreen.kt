package com.group8.comp2300.feature.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.BookingPaymentMethod
import com.group8.comp2300.util.formatCurrency
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookingPaymentScreen(
    clinicId: String,
    slotId: String,
    appointmentType: String,
    reason: String,
    hasReminder: Boolean,
    onBack: () -> Unit,
    onBookingConfirm: (com.group8.comp2300.domain.model.medical.Appointment, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    rescheduleAppointment: Appointment? = null,
    viewModel: BookingViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val clinic = state.selectedClinic?.takeIf { it.id == clinicId } ?: viewModel.getClinic(clinicId)
    val slot = viewModel.getSlot(slotId)
    val draft = state.bookingDraft?.takeIf { it.clinicId == clinicId && it.slotId == slotId }
    val currentOnBookingConfirm by rememberUpdatedState(onBookingConfirm)

    LaunchedEffect(clinicId, slotId, appointmentType, reason, hasReminder, rescheduleAppointment?.id) {
        viewModel.ensureBookingDraft(
            clinicId = clinicId,
            slotId = slotId,
            appointmentType = appointmentType,
            reason = reason,
            hasReminder = hasReminder,
            rescheduleAppointment = rescheduleAppointment,
        )
        if (clinic == null || slot == null) {
            viewModel.loadClinicDetails(clinicId)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.bookingEvents.collect { event ->
            when (event) {
                is BookingViewModel.BookingEvent.Submitted -> if (!event.wasRescheduled) {
                    currentOnBookingConfirm(event.appointment, false)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text("Payment") },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
        bottomBar = {
            if (clinic != null && slot != null && draft != null) {
                Surface(shadowElevation = 12.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.paymentErrorMessage?.let { paymentError ->
                            Text(
                                text = paymentError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = formatCurrency(draft.quotedFee),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.bookClinicAppointment(
                                    clinicId = clinic.id,
                                    slotId = slot.id,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSubmitting,
                        ) {
                            if (state.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text("Pay ${formatCurrency(draft.quotedFee)} and book")
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (clinic == null || slot == null || draft == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 320.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading payment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = clinic.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = slotSummary(slot.startTime),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = appointmentTypeLabel(draft.appointmentType),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PaymentSummaryRow(label = "Consultation", value = formatCurrency(draft.quotedFee))
                        PaymentSummaryRow(
                            label = "Total",
                            value = formatCurrency(draft.quotedFee),
                            emphasize = true,
                        )
                    }
                }
            }

            item {
                PaymentSectionHeader(
                    title = "Payment method",
                    supporting = "Choose how you want to pay.",
                )
            }

            items(BookingPaymentMethod.entries.toList()) { method ->
                PaymentMethodRow(
                    method = method,
                    isSelected = draft.selectedPaymentMethod == method,
                    onSelect = { viewModel.selectPaymentMethod(method) },
                )
            }

            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun PaymentSectionHeader(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = supporting,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PaymentSummaryRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PaymentMethodRow(method: BookingPaymentMethod, isSelected: Boolean, onSelect: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = method.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
            )
        }
    }
}
