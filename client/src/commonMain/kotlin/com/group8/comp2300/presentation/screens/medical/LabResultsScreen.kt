@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.symbols.icons.materialsymbols.Icons
import com.app.symbols.icons.materialsymbols.icons.*
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.model.medical.LabStatus
import com.group8.comp2300.mock.sampleResults
import com.group8.comp2300.presentation.util.DateFormatter
import comp2300.i18n.generated.resources.*
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.jetbrains.compose.resources.stringResource

// Helper to create timestamp from date components
private fun dateToTimestamp(year: Int, month: Int, day: Int): Long =
    kotlinx.datetime.LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabResultsScreen(onBack: () -> Unit, onScheduleTest: () -> Unit, modifier: Modifier = Modifier) {
    // Mock complete lab results data using proper types
    // Mock complete lab results data using proper types
    val allResults = remember { sampleResults }

    val filterAll = stringResource(Res.string.medical_lab_results_filter_all)
    val filterHiv = stringResource(Res.string.medical_lab_results_filter_hiv)
    val filterSti = stringResource(Res.string.medical_lab_results_filter_sti)
    val filterHepatitis = stringResource(Res.string.medical_lab_results_filter_hepatitis)

    var selectedFilter by remember { mutableStateOf(filterAll) }
    val filterOptions = listOf(filterAll, filterHiv, filterSti, filterHepatitis)

    val filteredResults =
        remember(selectedFilter) {
            when (selectedFilter) {
                filterHiv -> allResults.filter { it.testName.contains("HIV", ignoreCase = true) }

                filterSti ->
                    allResults.filter {
                        it.testName.contains("Chlamydia", ignoreCase = true) ||
                            it.testName.contains("Gonorrhea", ignoreCase = true) ||
                            it.testName.contains("Syphilis", ignoreCase = true)
                    }

                filterHepatitis -> allResults.filter { it.testName.contains("Hepatitis", ignoreCase = true) }

                else -> allResults
            }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.medical_lab_results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.ArrowBackW400Outlinedfill1,
                            contentDescription = stringResource(Res.string.medical_lab_results_back_desc)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(Res.string.medical_lab_results_history_header),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(Res.string.medical_lab_results_history_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                filterOptions.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            // Results count
            Text(
                text =
                    if (filteredResults.size == 1) {
                        stringResource(Res.string.medical_lab_results_count_single, filteredResults.size)
                    } else {
                        stringResource(Res.string.medical_lab_results_count_multiple, filteredResults.size)
                    },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            // Results list
            filteredResults.forEach { result ->
                LabResultCard(
                    result
                )
            }

            // Action button
            Button(
                onClick = onScheduleTest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(Res.string.medical_lab_results_schedule_next))
            }

            // Educational info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.medical_lab_results_recommendations_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.medical_lab_results_recommendations_content),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LabResultCard(result: LabResult) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = DateFormatter.formatMonthDayYear(result.testDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                StatusBadge(result)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                DetailRow(
                    stringResource(Res.string.medical_lab_results_detail_type),
                    result.testName
                )
                DetailRow(
                    stringResource(Res.string.medical_lab_results_detail_date),
                    DateFormatter.formatMonthDayYear(result.testDate)
                )
                val statusRes =
                    when (result.status) {
                        LabStatus.PENDING -> Res.string.lab_status_pending
                        LabStatus.NEGATIVE -> Res.string.lab_status_negative
                        LabStatus.POSITIVE -> Res.string.lab_status_positive
                        LabStatus.INCONCLUSIVE -> Res.string.lab_status_inconclusive
                    }
                DetailRow(
                    stringResource(Res.string.medical_lab_results_detail_status),
                    stringResource(statusRes)
                )
                DetailRow(
                    stringResource(Res.string.medical_lab_results_detail_location),
                    stringResource(Res.string.medical_lab_results_location_default)
                )
                DetailRow(
                    stringResource(Res.string.medical_lab_results_detail_next),
                    stringResource(Res.string.medical_lab_results_next_test_date)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.medical_lab_results_review_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(result: LabResult) {
    val bgColor = if (result.isPositive) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val textColor = if (result.isPositive) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)

    val statusRes =
        when (result.status) {
            LabStatus.PENDING -> Res.string.lab_status_pending
            LabStatus.NEGATIVE -> Res.string.lab_status_negative
            LabStatus.POSITIVE -> Res.string.lab_status_positive
            LabStatus.INCONCLUSIVE -> Res.string.lab_status_inconclusive
        }
    Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            stringResource(statusRes),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
