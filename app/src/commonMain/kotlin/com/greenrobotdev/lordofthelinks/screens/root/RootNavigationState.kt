package com.greenrobotdev.lordofthelinks.screens.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Root Navigation State - Manages the 4 top-level tabs
 * Each tab maintains its own independent navigation state
 */
class RootNavigationState(
    startTab: RootScreens,
    currentTab: MutableState<RootScreens>
) {
    var currentTab: RootScreens by currentTab
    val startTab = startTab

    /** Tab visit order, most recent last; Feed → Library → Reminders, back lands on Library. */
    internal val tabHistory = mutableStateListOf(startTab) // observable: Back enablement reads it

    /**
     * Get all available tabs
     */
    val tabs: List<RootScreens>
        get() = listOf(
            RootScreens.FeedTab,
            RootScreens.LibraryTab,
            RootScreens.RemindersTab,
            RootScreens.SettingsTab
        )

    fun indexOf(tab: RootScreens): Int = tabs.indexOf(tab)

    /** Switch forward (or re-visit): record it so Back can walk the history. */
    fun visit(tab: RootScreens) {
        if (tab == currentTab) return
        // Re-selecting an earlier tab moves it to the front of history, browser-style
        tabHistory.remove(tab)
        tabHistory.add(tab)
        currentTab = tab
    }

    /** True when Back should return to the previous tab instead of leaving the app. */
    fun canGoBack(): Boolean = tabHistory.size > 1

    /** Back: return to the previously visited tab (and drop the current one from history). */
    fun goBackTab(): RootScreens? {
        if (tabHistory.size <= 1) return null
        tabHistory.removeAt(tabHistory.lastIndex)
        val previous = tabHistory.last()
        currentTab = previous
        return previous
    }
}

/**
 * Create root navigation state for managing tabs
 */
@Composable
fun rememberRootNavigationState(
    startTab: RootScreens = RootScreens.FeedTab
): RootNavigationState {
    val currentTab = remember { mutableStateOf(startTab) }

    return remember(startTab) {
        RootNavigationState(
            startTab = startTab,
            currentTab = currentTab
        )
    }
}

/**
 * Root Navigator - Manages tab switching
 */
class RootNavigator(val state: RootNavigationState) {
    /**
     * Switch to a different tab
     */
    fun switchTab(tab: RootScreens) {
        state.visit(tab)
    }

    /**
     * Get the current active tab
     */
    fun getCurrentTab(): RootScreens {
        return state.currentTab
    }

    /**
     * Check if a tab is currently active
     */
    fun isTabActive(tab: RootScreens): Boolean {
        return state.currentTab == tab
    }
}