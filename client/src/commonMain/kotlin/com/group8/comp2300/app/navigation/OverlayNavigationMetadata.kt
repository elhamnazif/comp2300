package com.group8.comp2300.app.navigation

import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay

fun overlayNavigationMetadata(base: Map<String, Any> = emptyMap()): Map<String, Any> =
    base + metadata {
        put(NavDisplay.TransitionKey) { pushAnimation }
        put(NavDisplay.PopTransitionKey) { popAnimation }
        put(NavDisplay.PredictivePopTransitionKey) { _ -> popAnimation }
    }
