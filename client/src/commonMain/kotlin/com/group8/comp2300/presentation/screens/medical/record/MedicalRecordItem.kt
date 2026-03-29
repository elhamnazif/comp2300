package com.group8.comp2300.presentation.screens.medical.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable

fun MedicalRecordItem(
    record: MedicalRecordResponse
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //icon based on extension
            val isPdf = record.fileName.endsWith(".pdf", ignoreCase = true)
            Icon(
                imageVector = if (isPdf) // idk
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Space(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(record.fileName, style = MaterialTheme.typography.titleMedium)

                //helper to format
                val date = remember(record.createdAt) {
                    java.text.SimpleDateFormat("MM/dd/yyyy").format(record.createdAt)
                }
                val size = remember(record.fileSize) { "${record.fileSize / 1024} KB" }

                Text("$size.$date", style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = onDelete) {
                Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Delete record", tint = Color.Red)
            }
        }
    }
}
