package com.group8.comp2300.feature.chatbot.navigation

import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.LocalUseRootOverlayForShellChildren
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.feature.chatbot.ChatbotScreen
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val chatbotGraphModule = module {
    navigation<Screen.Chatbot>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current

        ChatbotScreen(
            onBack = if (useRootOverlayForShellChildren) navigator::goBack else navigator::goBackWithinShell,
        )
    }
}
