package com.group8.comp2300.feature.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector

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
            shape = AuthFieldShape,
            colors = authTextFieldColors(),
        )
        Box(modifier = Modifier.matchParentSize().alpha(0f).clickable(onClick = onClick))
    }
}
