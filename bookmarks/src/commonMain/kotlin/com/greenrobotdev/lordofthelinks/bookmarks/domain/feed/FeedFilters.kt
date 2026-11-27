package com.greenrobotdev.lordofthelinks.bookmarks.domain.feed

import com.greenrobotdev.lordofthelinks.model.Link
import kotlinx.datetime.DatePeriod
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** ARCHIVED = only archived links, opened or not. */
@Serializable
enum class OpenedFilter(val label: String) { ALL("All"), UNOPENED("Unopened"), OPENED("Opened"), ARCHIVED("Archived") }

/** Link kinds the Type section offers; NO_PREVIEW = no title fetched yet. */
@Serializable
enum class LinkKind(val label: String) { ARTICLE("Articles"), VIDEO("Videos"), SOCIAL("Social"), NO_PREVIEW("No preview") }

@Serializable
enum class SavedRange(val label: String) { ANY("Any time"), TODAY("Today"), THIS_WEEK("This week"), THIS_MONTH("This month") }

/** Everything the Filters sheet sets. Default = no filtering (archived hidden). */
@Serializable
data class FeedFilters(
    val opened: OpenedFilter = OpenedFilter.ALL,
    val favoritesOnly: Boolean = false,
    val kinds: Set<LinkKind> = emptySet(),
    /** Any of these categories, each including everything inside it. */
    val categoryIds: Set<String> = emptySet(),
    val tagIds: Set<String> = emptySet(),
    /** false: a link needs any of [tagIds]; true: all of them. */
    val tagMatchAll: Boolean = false,
    val saved: SavedRange = SavedRange.ANY,
    val includeArchived: Boolean = false,
    /** Sort order. Not a filter, so it doesn't count toward [activeCount] or hide the Feed's discovery rows. */
    val newestFirst: Boolean = true,
) {
    /** For the Filters button badge: one per section that is set. */
    val activeCount: Int
        get() = listOf(
            opened != OpenedFilter.ALL, favoritesOnly, kinds.isNotEmpty(), categoryIds.isNotEmpty(),
            tagIds.isNotEmpty(), saved != SavedRange.ANY, includeArchived && opened != OpenedFilter.ARCHIVED,
        ).count { it }
}

fun Link.kind(): LinkKind = when {
    title.isBlank() || title == url -> LinkKind.NO_PREVIEW
    type == "video" -> LinkKind.VIDEO
    type == "social" -> LinkKind.SOCIAL
    else -> LinkKind.ARTICLE
}

/** Start of the range in epoch ms (device time zone), or null for Any time. */
@OptIn(ExperimentalTime::class)
fun SavedRange.startMs(nowMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()): Long? {
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
    val start: LocalDate = when (this) {
        SavedRange.ANY -> return null
        SavedRange.TODAY -> today
        SavedRange.THIS_WEEK -> today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1)) // Monday
        SavedRange.THIS_MONTH -> LocalDate(today.year, today.month, 1)
    }
    return start.atStartOfDayIn(zone).toEpochMilliseconds()
}
