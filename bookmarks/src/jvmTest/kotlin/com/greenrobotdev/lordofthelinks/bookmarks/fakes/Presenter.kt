package com.greenrobotdev.lordofthelinks.bookmarks.fakes

import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlin.time.Duration.Companion.minutes

/**
 * Virtual time a test lets pass after each event: longer than any presenter timer (undo window,
 * note debounce). delay(), not advanceUntilIdle(): the latter stops once only backgroundScope
 * work is left, and that is where the presenter runs.
 */
private val SETTLE = 1.minutes

/** A Molecule presenter running on the test scheduler; [send] delivers events and lets their effects finish. */
class PresenterRun<E, S>(private val events: MutableSharedFlow<E>, private val states: StateFlow<S>) {
    val state: S get() = states.value

    suspend fun send(vararg event: E) {
        event.forEach {
            events.emit(it)
            delay(SETTLE)
        }
    }

    /** For work on a real dispatcher (e.g. the metadata fetch), which virtual time doesn't wait for. */
    suspend fun awaitState(predicate: (S) -> Boolean): S = states.first(predicate)

    /** All events back to back with no time passing between them (e.g. Undo inside the undo window), then settle. */
    suspend fun sendTogether(vararg event: E) {
        event.forEach { events.emit(it) }
        delay(SETTLE)
    }
}

suspend fun <E, S> TestScope.present(presenter: @Composable (Flow<E>) -> S): PresenterRun<E, S> {
    val events = MutableSharedFlow<E>(extraBufferCapacity = 64)
    val states = backgroundScope.launchMolecule(RecompositionMode.Immediate) { presenter(events) }
    delay(SETTLE)
    return PresenterRun(events, states)
}
