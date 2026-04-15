package com.group8.comp2300.presentation.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import kotlinx.serialization.json.Json

abstract class Navigator : ViewModel() {
    abstract val backStack: MutableList<Screen>
    abstract val currentScreen: Screen?

    abstract var postLoginTarget: Screen?

    abstract fun navigate(screen: Screen)

    abstract fun goBack()

    abstract fun clearAndGoTo(screen: Screen)

    abstract fun requireAuth(targetScreen: Screen? = null)

    abstract fun onAuthSuccess()

    abstract fun setStartDestination(screen: Screen)
}

@OptIn(SavedStateHandleSaveableApi::class)
class RealNavigator(savedStateHandle: SavedStateHandle, startDestination: Screen = Screen.Onboarding) : Navigator() {

    override val backStack: MutableList<Screen> = savedStateHandle.saveable(
        key = "nav_stack",
        saver = listSaver(
            save = { list ->
                list.map { Json.encodeToString(it) }
            },
            restore = { savedList ->
                try {
                    savedList.map { Json.decodeFromString<Screen>(it) }.toMutableStateList()
                } catch (_: Exception) {
                    // Fallback in case of deserialization error
                    mutableStateListOf(startDestination)
                }
            },
        ),
    ) {
        // Initial value if no state is saved
        mutableStateListOf(startDestination)
    }

    override val currentScreen: Screen?
        get() = backStack.lastOrNull()

    override var postLoginTarget: Screen? by savedStateHandle.saveable {
        mutableStateOf(null)
    }

    override fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    override fun goBack() {
        if (currentScreen == Screen.Login) {
            postLoginTarget = null
        }
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    override fun clearAndGoTo(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    override fun requireAuth(targetScreen: Screen?) {
        postLoginTarget = targetScreen
        if (currentScreen != Screen.Login) {
            backStack.add(Screen.Login)
        }
    }

    override fun onAuthSuccess() {
        val target = postLoginTarget
        postLoginTarget = null
        if (currentScreen == Screen.Login && backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (target != null) {
            if (backStack.lastOrNull() != target) {
                backStack.add(target)
            }
        } else {
            clearAndGoTo(Screen.Home)
        }
    }

    override fun setStartDestination(screen: Screen) {
        if (backStack.size == 1 && backStack.first() == Screen.Onboarding) {
            backStack.clear()
            backStack.add(screen)
        }
    }
}
