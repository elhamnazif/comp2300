package com.group8.comp2300.app.navigation

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
    abstract val mainShellBackStack: MutableList<Screen>
    abstract val currentScreen: Screen?
    abstract val currentTab: Screen?

    abstract var postLoginTarget: Screen?

    abstract fun navigate(screen: Screen)

    abstract fun navigateWithinShell(screen: Screen)

    abstract fun goBack()

    abstract fun goBackWithinShell()

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

    override val mainShellBackStack: MutableList<Screen> = savedStateHandle.saveable(
        key = "main_shell_back_stack",
        saver = listSaver(
            save = { list -> list.map { Json.encodeToString(it) } },
            restore = { savedList ->
                try {
                    savedList.map { Json.decodeFromString<Screen>(it) }.toMutableStateList()
                } catch (_: Exception) {
                    mutableStateListOf(Screen.Home)
                }
            },
        ),
    ) {
        mutableStateListOf(Screen.Home)
    }

    override val currentScreen: Screen?
        get() = backStack.lastOrNull()

    override val currentTab: Screen?
        get() = mainShellBackStack.lastOrNull(Screen::isMainTab)

    override var postLoginTarget: Screen? by savedStateHandle.saveable {
        mutableStateOf(null)
    }

    init {
        normalizeRestoredStacks()
    }

    override fun navigate(screen: Screen) {
        if (screen.isMainTab()) {
            selectTab(screen)
            if (backStack.none { it == Screen.MainShell }) {
                clearAndGoTo(screen)
            }
            return
        }
        backStack.add(screen)
    }

    override fun navigateWithinShell(screen: Screen) {
        if (screen.isMainTab()) {
            selectTab(screen)
            return
        }
        mainShellBackStack.add(screen)
    }

    override fun goBack() {
        if (currentScreen == Screen.Login) {
            postLoginTarget = null
        }
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    override fun goBackWithinShell() {
        if (mainShellBackStack.lastOrNull()?.isMainTab() == false) {
            mainShellBackStack.removeAt(mainShellBackStack.lastIndex)
        }
    }

    override fun clearAndGoTo(screen: Screen) {
        backStack.clear()
        if (screen.isMainTab()) {
            resetTabs(screen)
            backStack.add(Screen.MainShell)
        } else {
            backStack.add(screen)
        }
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
            if (target.isMainTab()) {
                clearAndGoTo(target)
            } else if (backStack.lastOrNull() != target) {
                backStack.add(target)
            }
        } else {
            clearAndGoTo(Screen.Home)
        }
    }

    override fun setStartDestination(screen: Screen) {
        if (backStack.size == 1 && backStack.first() == Screen.Onboarding) {
            clearAndGoTo(screen)
        }
    }

    private fun selectTab(tab: Screen) {
        if (!tab.isMainTab()) return
        while (mainShellBackStack.lastOrNull()?.isMainTab() == false) {
            mainShellBackStack.removeAt(mainShellBackStack.lastIndex)
        }
        mainShellBackStack.remove(tab)
        mainShellBackStack.add(tab)
    }

    private fun resetTabs(tab: Screen) {
        mainShellBackStack.clear()
        mainShellBackStack.add(tab)
    }

    private fun normalizeRestoredStacks() {
        if (backStack.contains(Screen.MainShell)) return
        val rootTabIndex = backStack.indexOfLast(Screen::isMainTab)
        if (rootTabIndex == -1) return

        val rootTab = backStack[rootTabIndex]
        val trailingOverlays = backStack.drop(rootTabIndex + 1)

        resetTabs(rootTab)
        backStack.clear()
        backStack.add(Screen.MainShell)
        backStack.addAll(trailingOverlays)
    }
}
