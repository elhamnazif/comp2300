package com.group8.comp2300.presentation.screens.medical

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.presentation.util.DateFormatter
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
fun DateValueField(label: String, value: LocalDate, onClick: () -> Unit) {
    OutlinedTextField(
        value = DateFormatter.formatDayMonthYear(value),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        enabled = false,
        colors =
        OutlinedTextFieldDefaults.colors(
            disabledTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
        ),
        trailingIcon = { androidx.compose.material3.Icon(Icons.DateRangeW400Outlinedfill1, null) },
    )
}

@Composable
fun TimeValueField(label: String, value: Long, onClick: () -> Unit) {
    val totalMinutes = (value / 60_000L).toInt()
    OutlinedTextField(
        value = DateFormatter.formatTime(totalMinutes / 60, totalMinutes % 60),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        enabled = false,
        colors =
        OutlinedTextFieldDefaults.colors(
            disabledTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline,
        ),
        trailingIcon = { androidx.compose.material3.Icon(Icons.CheckCircleW400Outlinedfill1, null) },
    )
}

@Composable
fun DatePickerSheet(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.atStartOfDayMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
                    }
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

@Composable
fun TimePickerSheet(initialTimeMs: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val totalMinutes = (initialTimeMs / 60_000L).toInt()
    val state = rememberTimePickerState(initialHour = totalMinutes / 60, initialMinute = totalMinutes % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(((state.hour * 60L) + state.minute) * 60_000L) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

fun parseColorHex(hex: String?): Color = try {
    val raw = (hex ?: Medication.PRESET_COLORS.first()).removePrefix("#")
    Color((raw.toLong(16) or 0xFF000000L).toInt())
} catch (_: Exception) {
    Color(0xFF42A5F5)
}

fun Color.toHexString(): String {
    val red = (this.red * 255).toInt().coerceIn(0, 255)
    val green = (this.green * 255).toInt().coerceIn(0, 255)
    val blue = (this.blue * 255).toInt().coerceIn(0, 255)
    return "#${red.hex2()}${green.hex2()}${blue.hex2()}"
}

private fun Int.hex2(): String = toString(16).uppercase().padStart(2, '0')

private fun LocalDate.atStartOfDayMillis(): Long =
    LocalDateTime(this, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds()
