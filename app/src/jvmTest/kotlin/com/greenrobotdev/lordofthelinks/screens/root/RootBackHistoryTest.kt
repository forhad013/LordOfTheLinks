package com.greenrobotdev.lordofthelinks.screens.root

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootBackHistoryTest {
    @Test
    fun backWalksTabHistoryAndIsObservable() {
        val state = RootNavigationState(RootScreens.FeedTab, mutableStateOf(RootScreens.FeedTab))
        state.visit(RootScreens.LibraryTab)
        state.visit(RootScreens.SettingsTab)

        // Compose only re-enables Back if canGoBack() reads snapshot state
        var reads = 0
        Snapshot.observe(readObserver = { reads++ }) { assertTrue(state.canGoBack()) }
        assertTrue(reads > 0)

        assertEquals(RootScreens.LibraryTab, state.goBackTab())
        assertEquals(RootScreens.FeedTab, state.goBackTab())
        assertEquals(null, state.goBackTab())
    }
}
