package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed

import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.FeedFilters
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.OpenedFilter
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.kind
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.startMs
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.subtreeIds
import kotlinx.datetime.minus
import kotlin.time.ExperimentalTime

/** Search + [filters], then sorted. Used for the Feed and for the sheet's live "Show N saves" count. */
@OptIn(ExperimentalTime::class)
fun filterFeed(
    state: FeedState,
    filters: FeedFilters,
    nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
): List<Link> {
    val q = state.searchQuery.trim()
    val inCategories: Set<String>? = filters.categoryIds.takeIf { it.isNotEmpty() }
        ?.flatMap { subtreeIds(it, state.categories) }?.toSet()
    val tagNames = state.tags.filter { it.id in filters.tagIds }.map { it.name.lowercase() }.toSet()
    val since = filters.saved.startMs(nowMs)
    return state.links.filter { link ->
        val catId = state.linkCategoryIds[link.id]
        val inheritedTags = catId?.let { state.categoryTagNames[it] }?.map { it.lowercase() }.orEmpty()
        val linkTags = (link.tags.map { it.lowercase() } + inheritedTags).distinct()
        when (filters.opened) {
            OpenedFilter.ALL -> filters.includeArchived || !link.isArchived
            OpenedFilter.UNOPENED -> !link.isOpened && (filters.includeArchived || !link.isArchived)
            OpenedFilter.OPENED -> link.isOpened && (filters.includeArchived || !link.isArchived)
            OpenedFilter.ARCHIVED -> link.isArchived
        } &&
            (!filters.favoritesOnly || link.isFavorite) &&
            (filters.kinds.isEmpty() || link.kind() in filters.kinds) &&
            (inCategories == null || state.linkCategoryIds[link.id] in inCategories) &&
            (tagNames.isEmpty() || if (filters.tagMatchAll) linkTags.containsAll(tagNames) else linkTags.any { it in tagNames }) &&
            // undated links (saved before createdAt existed) never match a date range
            (since == null || (link.createdAt ?: Long.MIN_VALUE) >= since) &&
            (q.isEmpty() || listOfNotNull(
                link.title, link.url, link.description, link.fetchedDescription, state.categoryOf(link)?.name,
            ).any { it.contains(q, ignoreCase = true) } || link.tags.any { it.contains(q.removePrefix("#"), ignoreCase = true) })
    }.let { list ->
        // Repo order is newest first (createdAt, then insertion order for undated links); the stable
        // sort keeps that tie-break, and Oldest is its exact reverse so undated links flip too.
        val newest = list.sortedByDescending { it.createdAt ?: Long.MIN_VALUE }
        if (filters.newestFirst) newest else newest.asReversed()
    }
}
