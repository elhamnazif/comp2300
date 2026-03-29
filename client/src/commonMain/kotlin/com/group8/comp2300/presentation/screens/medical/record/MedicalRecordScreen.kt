package com.group8.comp2300.presentation.screens.medical.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.MessageBlock
import com.github.ajalt.colormath.Color
import com.group8.comp2300.presentation.components.ScreenHeader

@Composable

fun MedicalRecordScreen(
    viewModel: MedicalRecordViewModel,
    onNavigateBack: () -> Unit
) {
    val state = viewModel.uiState

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        ScreenHeader(horizontalPadding = 16.dp){
            Text("My Medical Records", style = MaterialTheme.typography.headlineSmall)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(RecordSortOrder.entries) { order ->
                FilterChip(
                    selected = state.selectedSort == order,
                    onClick = { viewModel.loadRecords(order) },
                    label = { Text(order.name.lowercase().capitalize()) }
                )
            }
        }
    }

    if (state.isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    if (state.records.isEmpty() && !state.isLoading) {
        EmptyStateView()
    } else {
        LazyColumn (
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.records) { record ->
                MedicalRecordItem(
                    record = record,
                    onDelete = { viewModel.deleteRecord(record.id)}
                )
            }
        }
    }
}