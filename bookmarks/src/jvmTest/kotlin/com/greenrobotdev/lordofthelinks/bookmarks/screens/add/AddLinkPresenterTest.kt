package com.greenrobotdev.lordofthelinks.bookmarks.screens.add

import com.greenrobotdev.lordofthelinks.bookmarks.data.provider.HtmlLinkMetadataProvider
import com.greenrobotdev.lordofthelinks.testing.FAILURE
import com.greenrobotdev.lordofthelinks.testing.FakeCategoryRepository
import com.greenrobotdev.lordofthelinks.testing.FakeLinkRepository
import com.greenrobotdev.lordofthelinks.testing.FakeTagRepository
import com.greenrobotdev.lordofthelinks.testing.present
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddLinkPresenterTest {

    private val url = "https://example.com/my-article"
    private val links = FakeLinkRepository()
    private val tags = FakeTagRepository(Tag(id = "k", name = "Kotlin", count = 0))
    private val categories = FakeCategoryRepository(Category(id = "dev", name = "Dev"))
    private var page = """<html><head><meta property="og:title" content="My Article"><meta property="og:image" content="/cover.png"></head></html>"""
    private val metadata = HtmlLinkMetadataProvider(HttpClient(MockEngine { if (page.isEmpty()) respondError(HttpStatusCode.NotFound) else respond(page) }))

    private suspend fun TestScope.open(initialUrl: String? = url) = present { events ->
        AddLinkPresenter(
            initialState = AddLinkState(url = initialUrl.orEmpty(), askForUrl = initialUrl.isNullOrBlank()),
            events = events,
            linkRepository = links,
            tagRepository = tags,
            categoryRepository = categories,
            metadataProvider = metadata,
            initialUrl = initialUrl,
        )
    }

    @Test
    fun sharedUrlIsStoredRightAway() = runTest {
        val screen = open()

        val stored = links.links.value.single()
        assertEquals(stored.id, screen.state.savedLinkId)
        assertEquals(url, stored.url)
        assertNotNull(stored.createdAt)
    }

    @Test
    fun alreadySavedUrlShowsThatSaveInstead() = runTest {
        links.links.value = listOf(Link(id = "old", title = "Saved before", url = "http://www.example.com/my-article/"))

        val screen = open()

        assertEquals(SavedDuplicate("old", "Saved before", null), screen.state.duplicate)
        assertEquals(1, links.links.value.size)
        assertNull(screen.state.savedLinkId)
    }

    @Test
    fun fetchedMetadataFillsTheTitleAndUpdatesTheStoredLink() = runTest {
        val screen = open()

        screen.send(AddLinkEvent.FetchMetadata)
        val state = screen.awaitState { it.fetchedMetadata != null }

        assertEquals("My Article", state.title)
        screen.awaitState { links.link(it.savedLinkId!!).title == "My Article" }
        assertEquals("https://example.com/cover.png", links.links.value.single().imageUrl)
    }

    @Test
    fun failedFetchFallsBackToATitleFromTheUrl() = runTest {
        page = ""
        val screen = open()

        screen.send(AddLinkEvent.FetchMetadata)
        val state = screen.awaitState { it.fetchError != null }

        assertEquals("example.com - My Article", state.title)
        assertFalse(state.isFetching)
    }

    @Test
    fun saveAssignsTheChosenTagsAndCategory() = runTest {
        val screen = open()

        screen.send(
            AddLinkEvent.ToggleTag("k"),
            AddLinkEvent.SetCategory("dev"),
            AddLinkEvent.NotesChanged("read later"),
            AddLinkEvent.Submit,
        )

        val id = screen.state.savedLinkId!!
        assertTrue(screen.state.success)
        assertEquals(setOf("k"), tags.linkTags.value[id])
        assertEquals("dev", categories.linkCategories.value[id])
        assertEquals("read later", links.link(id).description)
        assertEquals(1, links.links.value.size) // Save updated the stored link, no copy
    }

    @Test
    fun failedSaveShowsTheError() = runTest {
        val screen = open()
        links.failWrites = true

        screen.send(AddLinkEvent.Submit)

        assertEquals(FAILURE, screen.state.error)
        assertFalse(screen.state.success)
        assertFalse(screen.state.isLoading)
    }

    @Test
    fun quickAddStoresTheLinkOnSave() = runTest {
        val screen = open(initialUrl = null)
        assertEquals(emptyList(), links.links.value)

        screen.send(AddLinkEvent.UrlChanged("https://kotlinlang.org/docs"), AddLinkEvent.Submit)

        assertTrue(screen.state.success)
        assertEquals("https://kotlinlang.org/docs", links.links.value.single().url)
    }

    @Test
    fun quickAddOfASavedUrlShowsThatSave() = runTest {
        links.links.value = listOf(Link(id = "old", title = "Docs", url = "https://kotlinlang.org/docs"))
        val screen = open(initialUrl = null)

        screen.send(AddLinkEvent.UrlChanged("https://kotlinlang.org/docs"), AddLinkEvent.Submit)

        assertEquals("old", screen.state.duplicate?.linkId)
        assertFalse(screen.state.success)
        assertEquals(1, links.links.value.size)
    }

    @Test
    fun discardDeletesTheStoredLink() = runTest {
        val screen = open()

        screen.send(AddLinkEvent.Discard)

        assertEquals(emptyList(), links.links.value)
        assertTrue(screen.state.success)
    }

    @Test
    fun addTagByNameReusesOrCreates() = runTest {
        val screen = open()

        screen.send(AddLinkEvent.AddTagByName(" #kotlin "), AddLinkEvent.AddTagByName("compose"))

        val compose = tags.tags.value.single { it.name == "compose" }
        assertEquals(setOf("k", compose.id), screen.state.selectedTags)
        assertEquals(2, tags.tags.value.size)
    }
}
