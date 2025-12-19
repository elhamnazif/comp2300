package com.group8.comp2300.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.group8.comp2300.domain.model.Screen
import kotlinx.serialization.json.Json

abstract class Navigator : ViewModel() {
    abstract val backStack: MutableList<Screen>
    abstract val currentScreen: Screen?

    abstract var isGuest: Boolean

    abstract fun navigate(screen: Screen)

    abstract fun goBack()

    abstract fun clearAndGoTo(screen: Screen)

    abstract fun requireAuth()
}

@OptIn(SavedStateHandleSaveableApi::class)
class RealNavigator(savedStateHandle: SavedStateHandle, startDestination: Screen = Screen.Onboarding) : Navigator() {

    override val backStack: MutableList<Screen> = savedStateHandle.saveable(
        key = "nav_stack",
        saver = listSaver(
            save = { list ->
                // Convert List<Screen> -> List<String> (JSON)
                list.map { Json.encodeToString(it) }
            },
            restore = { savedList ->
                // Convert List<String> (JSON) -> MutableList<Screen>
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

    override var isGuest: Boolean by savedStateHandle.saveable { mutableStateOf(true) }

    override fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    override fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    override fun clearAndGoTo(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    override fun requireAuth() {
        navigate(Screen.Login)
    }
}

class FakeNavigator(startDestination: Screen = Screen.Onboarding) : Navigator() {

    override val backStack = mutableListOf(startDestination)

    override val currentScreen: Screen?
        get() = backStack.lastOrNull()

    override fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    override fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    override fun clearAndGoTo(screen: Screen) {
        backStack.clear()
        backStack.add(screen)
    }

    override fun requireAuth() {
        navigate(Screen.Login)
    }

    override var isGuest: Boolean = true
}
