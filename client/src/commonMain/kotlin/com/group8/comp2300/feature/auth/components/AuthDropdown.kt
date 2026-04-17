package com.group8.comp2300.feature.auth.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowDropDownW400Outlinedfill1

@Composable
fun AuthDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ClickableTextField(
            value = selectedValue,
            label = label,
            onClick = { expanded = true },
            trailingIcon = Icons.ArrowDropDownW400Outlinedfill1,
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.7f),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectOption(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
