package com.greenrobotdev.lordofthelinks.bookmarks.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import kotlinx.coroutines.flow.Flow

/** Library tab presenter: smart list counts, categories and tags, all live. */
@Composable
fun LibraryPresenter(categoryRepository: CategoryRepository, tagRepository: TagRepository): LibraryState {
    var state by remember { mutableStateOf(LibraryState()) }

    // shortcut: direct DAO count flows would avoid the Flow-of-Result wrapper; fine at this size
    LaunchedEffect(Unit) {
        categoryRepository.getSmartListCounts().collect { result ->
            result.onSuccess { state = state.copy(
                unopenedCount = it.unopened,
                favoritesCount = it.favorites,
                archivedCount = it.archived,
                uncategorizedCount = it.uncategorized,
                remindersCount = it.reminders
            ) }
        }
    }
    LaunchedEffect(Unit) {
        categoryRepository.getCategoriesWithCounts().collect { result ->
            result.onSuccess { rows -> state = state.copy(categories = rows) }
        }
    }
    LaunchedEffect(Unit) {
        tagRepository.getTags().collect { result ->
            result.onSuccess { tags -> state = state.copy(tags = tags) }
        }
    }
    LaunchedEffect(Unit) {
        tagRepository.getCategoryTagNames().collect { result ->
            result.onSuccess { state = state.copy(categoryTagNames = it) }
        }
    }

    return state
}
