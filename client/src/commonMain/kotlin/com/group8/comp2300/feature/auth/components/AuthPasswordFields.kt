package com.group8.comp2300.feature.auth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.LockW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityOffW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.VisibilityW400Outlinedfill1
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class AuthPasswordFieldStyle {
    Filled,
    Outlined,
}

fun passwordVisualTransformation(isPasswordVisible: Boolean): VisualTransformation =
    if (isPasswordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

@Composable
fun PasswordVisibilityToggle(
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    contentDescription: String,
) {
    IconButton(onClick = onTogglePasswordVisibility) {
        val icon = if (isPasswordVisible) {
            Icons.VisibilityW400Outlinedfill1
        } else {
            Icons.VisibilityOffW400Outlinedfill1
        }
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    passwordToggleDescription: String,
    modifier: Modifier = Modifier,
    style: AuthPasswordFieldStyle = AuthPasswordFieldStyle.Filled,
    errorMessage: StringResource? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    when (style) {
        AuthPasswordFieldStyle.Filled -> {
            AuthTextField(
                value = value,
                onValueChange = onValueChange,
                label = label,
                modifier = modifier,
                leadingIcon = Icons.LockW400Outlinedfill1,
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        contentDescription = passwordToggleDescription,
                    )
                },
                errorMessage = errorMessage,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = passwordVisualTransformation(isPasswordVisible),
            )
        }

        AuthPasswordFieldStyle.Outlined -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                leadingIcon = {
                    Icon(Icons.LockW400Outlinedfill1, contentDescription = null)
                },
                trailingIcon = {
                    PasswordVisibilityToggle(
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = onTogglePasswordVisibility,
                        contentDescription = passwordToggleDescription,
                    )
                },
                visualTransformation = passwordVisualTransformation(isPasswordVisible),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(stringResource(it)) } },
                modifier = modifier.fillMaxWidth(),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}
