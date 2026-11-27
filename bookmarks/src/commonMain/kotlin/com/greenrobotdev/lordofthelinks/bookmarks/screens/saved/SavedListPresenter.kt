package com.greenrobotdev.lordofthelinks.bookmarks.screens.saved

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Composable
fun SavedListPresenter(
    events: Flow<SavedListEvent>,
    linkRepository: LinkRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
): SavedListState {
    var state by remember { mutableStateOf(SavedListState()) }

    LaunchedEffect(Unit) {
        linkRepository.getLinksWithTags().collect { result ->
            result.onSuccess { rows -> state = state.copy(links = rows, isLoading = false) }
                .onFailure { state = state.copy(isLoading = false) }
        }
    }
    LaunchedEffect(Unit) {
        categoryRepository.getCategories().collect { result ->
            result.onSuccess { rows -> state = state.copy(categories = rows) }
        }
    }
    LaunchedEffect(Unit) {
        categoryRepository.getLinkCategoryIds().collect { result ->
            result.onSuccess { state = state.copy(linkCategoryIds = it) }
        }
    }
    LaunchedEffect(Unit) {
        tagRepository.getCategoryTagNames().collect { result ->
            result.onSuccess { state = state.copy(categoryTagNames = it) }
        }
    }
    LaunchedEffect(Unit) {
        // The links flow re-emits after each write, so these only write
        events.collect { event ->
            when (event) {
                is SavedListEvent.Opened -> linkRepository.markOpened(event.linkId).first()
                is SavedListEvent.Archive -> state.links.firstOrNull { it.id == event.linkId }
                    ?.let { linkRepository.updateLink(it.copy(isArchived = true)).first() }
                is SavedListEvent.Delete -> linkRepository.deleteLink(event.linkId).first()
                is SavedListEvent.Restore -> linkRepository.updateLink(event.link).first()
            }
        }
    }
    return state
}
