package com.group8.comp2300.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun AppPrivacyMask(
    modifier: Modifier = Modifier,
    shouldBlurWhenBackgrounded: Boolean,
    content: @Composable () -> Unit,
) {
    var isAppInForeground by remember { mutableStateOf(true) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isAppInForeground = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        isAppInForeground = false
    }

    val shouldBlurApp = shouldBlurWhenBackgrounded && !isAppInForeground

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
            modifier.let { baseModifier ->
                if (shouldBlurApp) {
                    baseModifier.blur(24.dp)
                } else {
                    baseModifier
                }
            },
        ) {
            content()
        }

        if (shouldBlurApp) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
            )
        }
    }
}
