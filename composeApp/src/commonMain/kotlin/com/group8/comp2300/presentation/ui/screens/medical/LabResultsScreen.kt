package com.group8.comp2300.presentation.ui.screens.medical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.mock.sampleResults
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

// Helper to format timestamp for display
private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val months =
        listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
        )
    return "${months[localDateTime.month.number - 1]} ${localDateTime.day}, ${localDateTime.year}"
}

// Helper to create timestamp from date components
private fun dateToTimestamp(year: Int, month: Int, day: Int): Long {
    return kotlinx.datetime.LocalDate(year, month, day)
        .atStartOfDayIn(TimeZone.UTC)
        .toEpochMilliseconds()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabResultsScreen(onBack: () -> Unit, onScheduleTest: () -> Unit) {
    // Mock complete lab results data using proper types
    // Mock complete lab results data using proper types
    val allResults = remember { sampleResults }

    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "HIV", "STI Panel", "Hepatitis")

    val filteredResults =
        remember(selectedFilter) {
            when (selectedFilter) {
                "HIV" -> allResults.filter { it.testName.contains("HIV", ignoreCase = true) }
                "STI Panel" ->
                    allResults.filter {
                        it.testName.contains("Chlamydia", ignoreCase = true) ||
                                it.testName.contains("Gonorrhea", ignoreCase = true) ||
                                it.testName.contains("Syphilis", ignoreCase = true)
                    }

                "Hepatitis" ->
                    allResults.filter {
                        it.testName.contains("Hepatitis", ignoreCase = true)
                    }

                else -> allResults
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lab Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Complete Test History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View all your lab results and testing history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Filter chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    "${filteredResults.size} ${if (filteredResults.size == 1) "result" else "results"}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            // Results list
            filteredResults.forEach { result -> LabResultCard(result) }

            // Action button
            Button(
                onClick = onScheduleTest,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
            ) { Text("Schedule Next Test") }

            // Educational info
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Testing Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                            "• HIV testing: Every 3-6 months for sexually active individuals\n" +
                                    "• STI screening: Every 3-6 months or as recommended\n" +
                                    "• Hepatitis: Annually or as recommended by your provider",
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
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
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
                        text = formatDate(result.testDate),
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

                DetailRow("Test Type", result.testName)
                DetailRow("Date Performed", formatDate(result.testDate))
                DetailRow("Result Status", result.status.displayName)
                DetailRow("Testing Location", "HealthPlus Clinic")
                DetailRow("Next Recommended", "February 28, 2025")

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "All test results are reviewed by licensed healthcare providers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(result: LabResult) {
    val bgColor =
        if (result.isPositive) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
    val textColor = if (result.isPositive) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)

    Surface(color = bgColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            result.status.displayName,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
