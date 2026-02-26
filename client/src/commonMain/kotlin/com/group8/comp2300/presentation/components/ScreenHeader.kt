@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScreenHeader(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    topPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = horizontalPadding, end = horizontalPadding, top = topPadding),
        content = content
    )
}
