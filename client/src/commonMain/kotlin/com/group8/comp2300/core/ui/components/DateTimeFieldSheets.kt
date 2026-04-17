package com.group8.comp2300.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group8.comp2300.core.format.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import comp2300.i18n.generated.resources.Res
import comp2300.i18n.generated.resources.common_cancel
import comp2300.i18n.generated.resources.common_ok
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun DateValueField(label: String, value: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ValueField(
        label = label,
        value = DateFormatter.formatDayMonthYear(value),
        icon = Icons.DateRangeW400Outlinedfill1,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun TimeValueField(label: String, value: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val totalMinutes = (value / 60_000L).toInt()
    ValueField(
        label = label,
        value = DateFormatter.formatTime(totalMinutes / 60, totalMinutes % 60),
        icon = Icons.CheckCircleW400Outlinedfill1,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ValueField(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .focusable()
                .clickable(onClick = onClick)
                .padding(0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            TextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                singleLine = true,
                enabled = false,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                trailingIcon = {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                colors = TextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun DatePickerSheet(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDayMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { selectedDateMillis ->
                        onConfirm(Instant.fromEpochMilliseconds(selectedDateMillis).toLocalDateTime(TimeZone.UTC).date)
                    }
                },
            ) { Text(stringResource(Res.string.common_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
    ) { DatePicker(state = state) }
}

@Composable
fun TimePickerSheet(initialTimeMs: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val totalMinutes = (initialTimeMs / 60_000L).toInt()
    val state = rememberTimePickerState(initialHour = totalMinutes / 60, initialMinute = totalMinutes % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(((state.hour * 60L) + state.minute) * 60_000L) }) {
                Text(stringResource(Res.string.common_ok))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
        text = { TimePicker(state = state) },
    )
}

private fun LocalDate.atStartOfDayMillis(): Long =
    LocalDateTime(this, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds()
