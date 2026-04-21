package com.group8.comp2300.feature.chatbot.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.group8.comp2300.app.navigation.LocalNavigator
import com.group8.comp2300.app.navigation.LocalUseRootOverlayForShellChildren
import com.group8.comp2300.app.navigation.Screen
import com.group8.comp2300.app.navigation.overlayNavigationMetadata
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.feature.chatbot.ChatbotScreen
import org.koin.compose.koinInject
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

val chatbotGraphModule = module {
    navigation<Screen.Chatbot>(metadata = overlayNavigationMetadata()) {
        val navigator = LocalNavigator.current
        val useRootOverlayForShellChildren = LocalUseRootOverlayForShellChildren.current
        val authRepository = koinInject<AuthRepository>()
        val session by authRepository.session.collectAsState()

        if (session !is AuthSession.SignedIn) {
            LaunchedEffect(Unit) {
                navigator.requireAuth(Screen.Chatbot)
            }
            return@navigation
        }

        ChatbotScreen(
            onBack = if (useRootOverlayForShellChildren) navigator::goBack else navigator::goBackWithinShell,
        )
    }
}
