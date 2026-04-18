package com.group8.comp2300.core.ui.accessibility

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.group8.comp2300.symbols.icons.materialsymbols.Icons
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CheckCircleW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.CloseW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.DateRangeW400Outlinedfill1
import com.group8.comp2300.symbols.icons.materialsymbols.icons.InfoW400Outlinedfill1

enum class IndicatorPattern {
    SOLID,
    DIAGONAL,
    GRID,
    DOTS,
    HORIZONTAL,
    VERTICAL,
}

enum class StatusIcon(val imageVector: ImageVector) {
    SUCCESS(Icons.CheckCircleW400Outlinedfill1),
    WARNING(Icons.InfoW400Outlinedfill1),
    DANGER(Icons.CloseW400Outlinedfill1),
    DATE(Icons.DateRangeW400Outlinedfill1),
}

@Composable
fun PatternSwatch(color: Color, pattern: IndicatorPattern, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.clip(RoundedCornerShape(999.dp)),
    ) {
        drawRect(color = color)
        drawIndicatorPattern(
            pattern = pattern,
            color = overlayColorFor(color),
            topLeft = Offset.Zero,
            size = size,
        )
    }
}

@Composable
fun AccessibleStatusChip(
    label: String,
    icon: StatusIcon,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon.imageVector,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
fun IndicatorLegendItem(label: String, color: Color, pattern: IndicatorPattern, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.defaultMinSize(minHeight = 32.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PatternSwatch(
                color = color,
                pattern = pattern,
                modifier = Modifier.size(width = 18.dp, height = 10.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

fun DrawScope.drawIndicatorPattern(pattern: IndicatorPattern, color: Color, topLeft: Offset, size: Size) {
    when (pattern) {
        IndicatorPattern.SOLID -> Unit

        IndicatorPattern.DIAGONAL -> drawDiagonalPattern(color, topLeft, size)

        IndicatorPattern.GRID -> {
            drawDiagonalPattern(color, topLeft, size)
            drawReverseDiagonalPattern(color, topLeft, size)
        }

        IndicatorPattern.DOTS -> drawDotPattern(color, topLeft, size)

        IndicatorPattern.HORIZONTAL -> drawStripedPattern(color, topLeft, size, horizontal = true)

        IndicatorPattern.VERTICAL -> drawStripedPattern(color, topLeft, size, horizontal = false)
    }
}

private fun DrawScope.drawDiagonalPattern(color: Color, topLeft: Offset, size: Size) {
    val step = 8.dp.toPx()
    val strokeWidth = 2.dp.toPx()
    var startX = topLeft.x - size.height
    while (startX < topLeft.x + size.width) {
        drawLine(
            color = color,
            start = Offset(startX, topLeft.y + size.height),
            end = Offset(startX + size.height, topLeft.y),
            strokeWidth = strokeWidth,
        )
        startX += step
    }
}

private fun DrawScope.drawReverseDiagonalPattern(color: Color, topLeft: Offset, size: Size) {
    val step = 8.dp.toPx()
    val strokeWidth = 2.dp.toPx()
    var startX = topLeft.x
    while (startX < topLeft.x + size.width + size.height) {
        drawLine(
            color = color,
            start = Offset(startX, topLeft.y),
            end = Offset(startX - size.height, topLeft.y + size.height),
            strokeWidth = strokeWidth,
        )
        startX += step
    }
}

private fun DrawScope.drawStripedPattern(color: Color, topLeft: Offset, size: Size, horizontal: Boolean) {
    val step = 7.dp.toPx()
    val strokeWidth = 2.dp.toPx()
    if (horizontal) {
        var y = topLeft.y + step / 2f
        while (y < topLeft.y + size.height) {
            drawLine(
                color = color,
                start = Offset(topLeft.x, y),
                end = Offset(topLeft.x + size.width, y),
                strokeWidth = strokeWidth,
            )
            y += step
        }
    } else {
        var x = topLeft.x + step / 2f
        while (x < topLeft.x + size.width) {
            drawLine(
                color = color,
                start = Offset(x, topLeft.y),
                end = Offset(x, topLeft.y + size.height),
                strokeWidth = strokeWidth,
            )
            x += step
        }
    }
}

private fun DrawScope.drawDotPattern(color: Color, topLeft: Offset, size: Size) {
    val step = 8.dp.toPx()
    val radius = 1.5.dp.toPx()
    var x = topLeft.x + step / 2f
    while (x < topLeft.x + size.width) {
        var y = topLeft.y + step / 2f
        while (y < topLeft.y + size.height) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            y += step
        }
        x += step
    }
}

private fun overlayColorFor(color: Color): Color = if (relativeBrightness(color) > 0.5f) {
    Color.Black.copy(alpha = 0.22f)
} else {
    Color.White.copy(alpha = 0.4f)
}

private fun relativeBrightness(color: Color): Float =
    (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
