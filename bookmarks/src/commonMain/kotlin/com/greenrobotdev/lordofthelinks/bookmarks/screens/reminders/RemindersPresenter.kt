package com.greenrobotdev.lordofthelinks.bookmarks.screens.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Reminders presenter: every link with a reminder, soonest first, live. Every event
 * writes to the DB; the links flow brings the change back.
 */
@Composable
fun RemindersPresenter(
    events: Flow<RemindersEvent>,
    linkRepository: LinkRepository,
): RemindersState {
    var state by remember { mutableStateOf(RemindersState()) }

    LaunchedEffect(Unit) {
        linkRepository.getLinksWithTags().collect { result ->
            result.fold(
                onSuccess = { rows ->
                    state = state.copy(
                        links = rows.filter { it.reminderAt != null && !it.isArchived }.sortedBy { it.reminderAt },
                        isLoading = false,
                    )
                },
                onFailure = { state = state.copy(isLoading = false) },
            )
        }
    }
    LaunchedEffect(Unit) {
        // The links flow re-emits after each write, so these only write
        events.collect { event ->
            when (event) {
                is RemindersEvent.Clear -> linkRepository.clearReminder(event.linkId).first()
                is RemindersEvent.Restore -> linkRepository.setReminder(event.link.id, event.link.reminderAt!!).first()
            }
        }
    }
    return state
}
