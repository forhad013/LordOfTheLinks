package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed

import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.FeedFilters
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
import kotlinx.datetime.minus

data class FeedState(
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

sealed interface FeedEvent {
    data class SearchChanged(val query: String) : FeedEvent
    /** Filters sheet "Show N saves". */
    data class ApplyFilters(val filters: FeedFilters) : FeedEvent
    data class RemoveCategory(val categoryId: String) : FeedEvent
    data class RemoveTag(val tagId: String) : FeedEvent
    /** Reset search and every filter. */
    object ClearFilters : FeedEvent
    /** Card tapped: the link is about to open in the browser. */
    data class Opened(val linkId: String) : FeedEvent
    data class Archive(val linkId: String) : FeedEvent
    data class Delete(val linkId: String) : FeedEvent
    /** Undo: write back the link as it was before the last action. */
    data class Restore(val link: Link) : FeedEvent
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
