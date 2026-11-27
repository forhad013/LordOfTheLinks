package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed

import com.greenrobotdev.lordofthelinks.bookmarks.data.store.FeedFilterStore
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.FeedFilters
import com.greenrobotdev.lordofthelinks.bookmarks.domain.provider.LinkMetadataProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.greenrobotdev.lordofthelinks.bookmarks.domain.provider.HttpStatusException
import com.greenrobotdev.lordofthelinks.model.repository.CategoryRepository
import com.greenrobotdev.lordofthelinks.model.repository.LinkRepository
import com.greenrobotdev.lordofthelinks.model.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Feed presenter: four live sources (links, categories, link→category, tags) plus filter events. */
@Composable
fun FeedPresenter(
    events: Flow<FeedEvent>,
    linkRepository: LinkRepository,
    categoryRepository: CategoryRepository,
    tagRepository: TagRepository,
    filterStore: FeedFilterStore,
    metadataProvider: LinkMetadataProvider,
): FeedState {
    var state by remember { mutableStateOf(FeedState(filters = filterStore.load())) }

    // Saved on every change, so the Feed reopens with the same filters after the app is closed
    LaunchedEffect(state.filters) { filterStore.save(state.filters) }

    LaunchedEffect(Unit) {
        linkRepository.getLinksWithTags().collect { result ->
            result.onSuccess { entities -> state = state.copy(links = entities, isLoading = false) }
                .onFailure { state = state.copy(isLoading = false) }
        }
    }
    // Preview images for links saved before they were kept: one page at a time, in the background.
    // A network failure (offline, timeout) stays null and is retried next time.
    LaunchedEffect(Unit) {
        val missing = linkRepository.getLinks().first().getOrNull().orEmpty().filter { it.imageUrl == null }
        for (link in missing) {
            metadataProvider.fetchMetadata(link.url).first()
                .onSuccess { metadata -> linkRepository.setImageUrl(link.id, metadata.imageUrl.orEmpty()).first() }
                // The site refuses us (Facebook answers 400): record "no image" instead of asking on every launch
                .onFailure { if (it is HttpStatusException) linkRepository.setImageUrl(link.id, "").first() }
        }
    }
    LaunchedEffect(Unit) {
        categoryRepository.getCategories().collect { result ->
            result.onSuccess { rows ->
                // Drop picks whose category was deleted, or the Feed would stay empty with no chip to remove
                val categories = rows
                state = state.copy(categories = categories, filters = state.filters.copy(categoryIds = state.filters.categoryIds.filterTo(mutableSetOf()) { id -> categories.any { it.id == id } }))
            }
        }
    }
    LaunchedEffect(Unit) {
        categoryRepository.getLinkCategoryIds().collect { result ->
            result.onSuccess { state = state.copy(linkCategoryIds = it) }
        }
    }
    LaunchedEffect(Unit) {
        tagRepository.getTagsWithCounts().collect { result ->
            result.onSuccess { rows ->
                val tags = rows
                state = state.copy(tags = tags, filters = state.filters.copy(tagIds = state.filters.tagIds.filterTo(mutableSetOf()) { id -> tags.any { it.id == id } }))
            }
        }
    }
    LaunchedEffect(Unit) {
        tagRepository.getCategoryTagNames().collect { result ->
            result.onSuccess { state = state.copy(categoryTagNames = it) }
        }
    }

    LaunchedEffect(Unit) {
        events.collect { event ->
            state = when (event) {
                is FeedEvent.SearchChanged -> state.copy(searchQuery = event.query)
                is FeedEvent.ApplyFilters -> state.copy(filters = event.filters)
                is FeedEvent.RemoveCategory -> state.copy(filters = state.filters.copy(categoryIds = state.filters.categoryIds - event.categoryId))
                is FeedEvent.RemoveTag -> state.copy(filters = state.filters.copy(tagIds = state.filters.tagIds - event.tagId))
                FeedEvent.ClearFilters -> state.copy(searchQuery = "", filters = FeedFilters())
                is FeedEvent.Archive -> {
                    // the links flow re-emits with isArchived = true, which hides it from the Feed
                    state.links.firstOrNull { it.id == event.linkId }
                        ?.let { linkRepository.updateLink(it.copy(isArchived = true)).first() }
                    state
                }
                is FeedEvent.Delete -> {
                    // the links flow re-emits without it; tags and category rows go with it (FK cascade)
                    linkRepository.deleteLink(event.linkId).first()
                    state
                }
                is FeedEvent.Restore -> {
                    linkRepository.updateLink(event.link).first()
                    state
                }
                is FeedEvent.Opened -> {
                    // the links flow re-emits with isOpened = true; nothing to change here
                    linkRepository.markOpened(event.linkId).first()
                    state
                }
            }
        }
    }

    return state
}
