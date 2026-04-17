package com.group8.comp2300.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    topPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = horizontalPadding, end = horizontalPadding, top = topPadding),
        content = content,
    )
}
