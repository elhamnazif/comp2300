package com.group8.comp2300.presentation.screens.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowDropDownW400Outlinedfill1
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

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
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingIcon,
            isError = errorMessage != null,
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            )
        )
        // Error slot + field spacer. Uses min-height so long messages aren't clipped.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 24.dp)
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            contentAlignment = androidx.compose.ui.Alignment.TopStart
        ) {
            if (errorMessage != null) {
                Text(
                    stringResource(errorMessage),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ClickableTextField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Box(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = trailingIcon?.let { { Icon(it, contentDescription = null) } },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            )
        )
        Box(modifier = Modifier.matchParentSize().alpha(0f).clickable(onClick = onClick))
    }
}

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
