package com.group8.comp2300.presentation.screens.medical.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.RecordSortOrder
import com.group8.comp2300.presentation.components.AppTopBar
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.AddW400Outlinedfill1

@Composable
fun MedicalRecordScreen(viewModel: MedicalRecordViewModel, onNavigateBack: () -> Unit) {
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("My Medical Records") },
                onBackClick = onNavigateBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: file picker — platform-specific */ },
            ) {
                Icon(Icons.AddW400Outlinedfill1, contentDescription = "Upload record")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(RecordSortOrder.entries) { order ->
                    FilterChip(
                        selected = state.selectedSort == order,
                        onClick = { viewModel.loadRecords(order) },
                        label = { Text(order.displayName) },
                    )
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.records.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No records found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (!state.isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.records, key = { it.id }) { record ->
                        MedicalRecordItem(
                            record = record,
                            onDelete = { viewModel.deleteRecord(record.id) },
                        )
                    }
                }
            }
        }
    }
}
