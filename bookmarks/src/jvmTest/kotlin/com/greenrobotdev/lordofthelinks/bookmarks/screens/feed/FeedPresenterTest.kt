package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed

import com.greenrobotdev.lordofthelinks.bookmarks.data.store.FeedFilterStore
import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.FeedFilters
import com.greenrobotdev.lordofthelinks.bookmarks.data.provider.HtmlLinkMetadataProvider
import com.greenrobotdev.lordofthelinks.testing.FakeCategoryRepository
import com.greenrobotdev.lordofthelinks.testing.FakeLinkRepository
import com.greenrobotdev.lordofthelinks.testing.FakeTagRepository
import com.greenrobotdev.lordofthelinks.testing.present
import com.greenrobotdev.lordofthelinks.testing.settle
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedPresenterTest {

    // imageUrl "" = already checked, so these don't trigger the preview backfill
    private val links = FakeLinkRepository(
        Link(id = "a", title = "Kotlin", url = "https://kotlinlang.org", imageUrl = ""),
        Link(id = "b", title = "Compose", url = "https://developer.android.com/compose", imageUrl = ""),
    )
    private val categories = FakeCategoryRepository(Category(id = "dev", name = "Dev"))
    private val tags = FakeTagRepository(Tag(id = "t", name = "ui", count = 0))
    private val settings = MapSettings()
    private var pages: (String) -> String? = { null } // url → html; null = 404
    private val metadata = HtmlLinkMetadataProvider(HttpClient(MockEngine { request ->
        pages(request.url.toString())?.let { respond(it) } ?: respondError(HttpStatusCode.NotFound)
    }))

    private suspend fun TestScope.open() = present { events ->
        FeedPresenter(events, links, categories, tags, FeedFilterStore(settings), metadata)
    }

    @Test
    fun loadsLinksCategoriesAndTags() = runTest {
        categories.linkCategories.value = mapOf("a" to "dev")

        val state = open().state

        assertFalse(state.isLoading)
        assertEquals(setOf("a", "b"), state.links.map { it.id }.toSet())
        assertEquals(listOf("dev"), state.categories.map { it.id })
        assertEquals(mapOf("a" to "dev"), state.linkCategoryIds)
        assertEquals(listOf("t"), state.tags.map { it.id })
    }

    @Test
    fun filtersAreKeptForTheNextLaunch() = runTest {
        val feed = open()
        val filters = FeedFilters(categoryIds = setOf("dev"), tagIds = setOf("t"))

        feed.send(FeedEvent.ApplyFilters(filters))

        assertEquals(filters, FeedFilterStore(settings).load())
    }

    @Test
    fun filterChipsCanBeRemovedOneByOneOrCleared() = runTest {
        val feed = open()
        feed.send(FeedEvent.ApplyFilters(FeedFilters(categoryIds = setOf("dev"), tagIds = setOf("t"))), FeedEvent.SearchChanged("kot"))

        feed.send(FeedEvent.RemoveTag("t"))
        assertEquals(setOf("dev"), feed.state.filters.categoryIds)
        assertEquals(emptySet(), feed.state.filters.tagIds)

        feed.send(FeedEvent.ClearFilters)
        assertEquals(FeedFilters(), feed.state.filters)
        assertEquals("", feed.state.searchQuery)
    }

    @Test
    fun deletedCategoryDropsOutOfTheFilters() = runTest {
        val feed = open()
        feed.send(FeedEvent.ApplyFilters(FeedFilters(categoryIds = setOf("dev"))))

        categories.categories.value = emptyList()
        settle() // let the flow emission land

        assertEquals(emptySet(), feed.state.filters.categoryIds)
    }

    @Test
    fun archiveDeleteRestoreAndOpenedAreWrittenThrough() = runTest {
        val feed = open()
        val before = links.link("a")

        feed.send(FeedEvent.Archive("a"), FeedEvent.Opened("b"))
        assertTrue(links.link("a").isArchived)
        assertTrue(links.link("b").isOpened)

        feed.send(FeedEvent.Delete("b"))
        assertEquals(listOf("a"), feed.state.links.map { it.id })

        feed.send(FeedEvent.Restore(before))
        assertFalse(links.link("a").isArchived)
    }

    @Test
    fun missingPreviewImagesAreFetchedOnce() = runTest {
        links.links.value = listOf(
            Link(id = "img", title = "has og:image", url = "https://example.com/post"),
            Link(id = "refused", title = "site says 404", url = "https://example.com/blocked"),
        )
        pages = { url -> if (url.endsWith("/post")) """<meta property="og:image" content="https://example.com/og.png">""" else null }

        val feed = open()

        feed.awaitState { s -> s.links.none { it.imageUrl == null } }
        assertEquals("https://example.com/og.png", links.link("img").imageUrl)
        assertEquals("", links.link("refused").imageUrl) // recorded as "no image", not asked again
    }
}
