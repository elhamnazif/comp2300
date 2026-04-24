package com.group8.comp2300.feature.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.group8.comp2300.core.ui.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import com.group8.comp2300.util.formatCurrency
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookingSuccessScreen(
    clinicId: String,
    appointmentId: String,
    appointmentTime: Long,
    wasRescheduled: Boolean,
    onBack: () -> Unit,
    onViewCalendar: () -> Unit,
    onDone: () -> Unit,
    onManageBooking: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val clinic = state.selectedClinic?.takeIf { it.id == clinicId } ?: viewModel.getClinic(clinicId)
    val appointment = viewModel.getLastBookedAppointment(appointmentId)

    LaunchedEffect(clinicId) {
        if (clinic == null) {
            viewModel.loadClinicDetails(clinicId)
        }
    }

    LaunchedEffect(appointmentId) {
        viewModel.loadPersistedAppointment(appointmentId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = { Text(if (wasRescheduled) "Updated" else "Paid") },
                onBackClick = onBack,
                backContentDescription = "Back",
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.CheckCircleW400Outlinedfill1,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            if (wasRescheduled) "Booking updated" else "Payment complete",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(slotSummary(appointmentTime), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Clinic", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            clinic?.name ?: "Clinic booked",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        clinic?.address?.takeIf(String::isNotBlank)?.let {
                            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        appointment?.let {
                            Text(
                                appointmentStatusLabel(it.status),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Payment: ${paymentStatusLabel(it.paymentStatus)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!wasRescheduled) {
                                it.paymentMethod?.let { method ->
                                    Text(
                                        "Method: ${paymentMethodLabel(method)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                it.paymentAmount?.let { amount ->
                                    Text(
                                        "Paid: ${formatCurrency(amount)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                it.transactionId?.let { transactionId ->
                                    Text(
                                        "Transaction: $transactionId",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        appointment?.appointmentType?.let {
                            Text(appointmentTypeLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        appointment?.bookingId?.let {
                            Text("Reference: $it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onViewCalendar,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View calendar")
                }
            }

            if (appointment != null) {
                item {
                    OutlinedButton(
                        onClick = { onManageBooking(appointment.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Manage booking")
                    }
                }
            }

            item {
                TextButton(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Done")
                }
            }
        }
    }
}
