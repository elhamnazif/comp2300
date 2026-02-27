package com.group8.comp2300.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import kotlinx.coroutines.launch

fun Modifier.shimmerEffect(): Modifier = this then ShimmerNodeElement()

private data class ShimmerNodeElement(val durationMillis: Int = 1000) : ModifierNodeElement<ShimmerNode>() {
    override fun create(): ShimmerNode = ShimmerNode(durationMillis)

    override fun update(node: ShimmerNode) {
        node.durationMillis = durationMillis
    }
}

private class ShimmerNode(var durationMillis: Int) :
    Modifier.Node(),
    DrawModifierNode {
    // Animates from 0f to 1f
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            while (true) {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis, easing = LinearEasing),
                )
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val width = size.width
        // Map 0..1 progress to -2w..2w range
        val startOffsetX = -2 * width + (4 * width * progress.value)

        val brush = Brush.linearGradient(
            colors = listOf(Color(0xFFB8B5B5), Color(0xFF8F8B8B), Color(0xFFB8B5B5)),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + width, size.height),
        )

        // Draw shimmer background
        drawRect(brush)

        // Draw actual content on top
        drawContent()
    }
}
