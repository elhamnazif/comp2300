@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.ArrowBackW400Outlinedfill1
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

enum class AppTopBarStyle {
    Surface,
    TransparentOverlay
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    centered: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    backContentDescription: String? = null,
    style: AppTopBarStyle = AppTopBarStyle.Surface,
    containerColor: Color? = null,
    contentColor: Color? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val resolvedContainerColor =
        containerColor ?: when (style) {
            AppTopBarStyle.Surface -> MaterialTheme.colorScheme.surface
            AppTopBarStyle.TransparentOverlay -> Color.Transparent
        }
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    val colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = resolvedContainerColor,
            titleContentColor = resolvedContentColor,
            navigationIconContentColor = resolvedContentColor,
            actionIconContentColor = resolvedContentColor
        )

    val navigationIcon: @Composable () -> Unit = {
        if (onBackClick != null) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.ArrowBackW400Outlinedfill1,
                    contentDescription = backContentDescription
                )
            }
        }
    }

    if (centered) {
        CenterAlignedTopAppBar(
            modifier = modifier,
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors
        )
    } else {
        TopAppBar(
            modifier = modifier,
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors
        )
    }
}
