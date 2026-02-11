@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.screens.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.*
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Standard text input for the app with consistent styling */
@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    errorMessage: StringResource? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } }, // Null content desc for decor
        trailingIcon = trailingIcon,
        isError = errorMessage != null,
        supportingText = {
            errorMessage?.let { res: StringResource ->
                Text(stringResource(res), color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth()
    )
}

/** A text field that acts like a button. Used for Date Pickers and Dropdowns to ensure the WHOLE area is clickable. */
@Composable
fun ClickableTextField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingIcon?.let { { Icon(it, contentDescription = null) } },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true, // Visual only
            singleLine = true
        )
        // Overlay Box that captures the click
        Box(modifier = Modifier.matchParentSize().alpha(0f).clickable(onClick = onClick))
    }
}

/** Reusable Dropdown component */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ClickableTextField(
            value = selectedValue,
            label = label,
            onClick = { expanded = true },
            trailingIcon = Icons.ArrowDropDownW400Outlinedfill1
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.7f) // Adjust width as needed
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectOption(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
