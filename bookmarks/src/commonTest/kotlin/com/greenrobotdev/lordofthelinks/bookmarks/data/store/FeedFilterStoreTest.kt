package com.greenrobotdev.lordofthelinks.bookmarks.data.store

import com.greenrobotdev.lordofthelinks.bookmarks.data.store.FeedFilterStore
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.FeedFilters
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.LinkKind
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.OpenedFilter
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.SavedRange
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedFilterStoreTest {
    @Test
    fun savedFiltersComeBack() {
        val settings = MapSettings()
        val filters = FeedFilters(
            opened = OpenedFilter.ARCHIVED, favoritesOnly = true, kinds = setOf(LinkKind.VIDEO),
            categoryIds = setOf("c1"), tagIds = setOf("t1", "t2"), tagMatchAll = true,
            saved = SavedRange.THIS_WEEK, includeArchived = true,
        )
        FeedFilterStore(settings).save(filters)
        assertEquals(filters, FeedFilterStore(settings).load())
    }

    @Test
    fun nothingSavedOrUnreadableGivesDefaults() {
        val settings = MapSettings()
        assertEquals(FeedFilters(), FeedFilterStore(settings).load())
        settings.putString("feed_filters", """{"opened":"NO_SUCH_VALUE"}""")
        assertEquals(FeedFilters(), FeedFilterStore(settings).load())
    }
}
