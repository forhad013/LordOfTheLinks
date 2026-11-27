package com.greenrobotdev.lordofthelinks.bookmarks.screens.library

import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.FeedFilters
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.OpenedFilter
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.kind
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.startMs
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.subtreeIds
import kotlinx.datetime.minus
import kotlin.time.ExperimentalTime

data class LibraryState(
    val isLoading: Boolean = true,
    val links: List<Link> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    /** linkId → categoryId */
    val linkCategoryIds: Map<String, String> = emptyMap(),
    /** categoryId → set of tag names (inherited tags from category_tags). */
    val categoryTagNames: Map<String, Set<String>> = emptyMap(),
    val searchQuery: String = "",
    val filters: FeedFilters = FeedFilters(),
) {
    val filteredLinks: List<Link> get() = filterFeed(this, filters)

    /** Saves in the inbox (not archived), and how many of them are opened: the header's progress ring. */
    val inboxTotal: Int get() = links.count { !it.isArchived }
    val inboxOpened: Int get() = links.count { !it.isArchived && it.isOpened }

    /** Unopened, unarchived saves, newest first: the Feed's "Pick up where you left off" row. */
    val unopenedLinks: List<Link>
        get() = links.filter { !it.isOpened && !it.isArchived }.sortedByDescending { it.createdAt ?: Long.MIN_VALUE }

    /**
     * Older unopened saves for the "You saved this…" card: past the pick-up row, saved a week or
     * more ago, or undated (saved before dates were kept). Oldest first.
     */
    fun resurfaceCandidates(nowMs: Long): List<Link> =
        unopenedLinks.drop(PICK_UP_COUNT)
            .filter { link -> link.createdAt.let { it == null || nowMs - it >= 7 * DAY_MS } }
            .asReversed()
    fun categoryOf(link: Link): Category? = linkCategoryIds[link.id]?.let { id -> categories.firstOrNull { it.id == id } }
}

/** Cards in the Feed's "Pick up where you left off" row. */
const val PICK_UP_COUNT = 5

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Search + [filters], then sorted. Used for the Feed and for the sheet's live "Show N saves" count. */
@OptIn(ExperimentalTime::class)
fun filterFeed(
    state: LibraryState,
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

sealed interface LibraryEvent {
    data class SearchChanged(val query: String) : LibraryEvent
    /** Filters sheet "Show N saves". */
    data class ApplyFilters(val filters: FeedFilters) : LibraryEvent
    data class RemoveCategory(val categoryId: String) : LibraryEvent
    data class RemoveTag(val tagId: String) : LibraryEvent
    /** Reset search and every filter. */
    object ClearFilters : LibraryEvent
    /** Card tapped: the link is about to open in the browser. */
    data class Opened(val linkId: String) : LibraryEvent
    data class Archive(val linkId: String) : LibraryEvent
    data class Delete(val linkId: String) : LibraryEvent
    /** Undo: write back the link as it was before the last action. */
    data class Restore(val link: Link) : LibraryEvent
}

/**
 * Whether the Feed shows "All caught up": only right after unopened saves went from 1+ to 0 with no
 * link deleted (opening, Mark as opened and archiving count; deleting, restoring or simply opening
 * the app with nothing unopened don't). Stays on until something new is unopened. -1 = no reading yet.
 */
fun celebrateCaughtUp(celebrating: Boolean, lastUnopened: Int, lastLinkCount: Int, unopened: Int, linkCount: Int): Boolean = when {
    unopened > 0 -> false
    lastUnopened > 0 && linkCount >= lastLinkCount -> true
    else -> celebrating
}
