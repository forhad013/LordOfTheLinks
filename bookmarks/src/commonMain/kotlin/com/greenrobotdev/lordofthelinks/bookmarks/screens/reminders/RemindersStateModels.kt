package com.greenrobotdev.lordofthelinks.bookmarks.screens.reminders

import com.greenrobotdev.lordofthelinks.model.Link
import kotlinx.datetime.plus

data class RemindersState(
    val isLoading: Boolean = true,
    val links: List<Link> = emptyList(),
)

sealed interface RemindersEvent {
    /** Swipe: drop the reminder, keep the link. */
    data class Clear(val linkId: String) : RemindersEvent
    /** Snackbar Undo: write the reminder back as it was. */
    data class Restore(val link: Link) : RemindersEvent
}
