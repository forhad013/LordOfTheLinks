package com.greenrobotdev.lordofthelinks.bookmarks.screens.details

import com.greenrobotdev.lordofthelinks.testing.FAILURE
import com.greenrobotdev.lordofthelinks.testing.FakeCategoryRepository
import com.greenrobotdev.lordofthelinks.testing.FakeLinkRepository
import com.greenrobotdev.lordofthelinks.testing.FakeTagRepository
import com.greenrobotdev.lordofthelinks.testing.present
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkDetailPresenterTest {

    private val links = FakeLinkRepository(Link(id = "a", title = "Kotlin", url = "https://kotlinlang.org"))
    private val categories = FakeCategoryRepository(Category(id = "c", name = "Dev"))
    private val tags = FakeTagRepository(Tag(id = "t1", name = "one", count = 0), Tag(id = "t2", name = "two", count = 0))

    private suspend fun TestScope.open(linkId: String = "a") =
        present { events -> LinkDetailPresenter(events, links, categories, tags, linkId) }

    @Test
    fun loadsTheLinkWithItsCategoriesAndTags() = runTest {
        categories.linkCategories.value = mapOf("a" to "c")
        tags.linkTags.value = mapOf("a" to setOf("t1"))

        val state = open().state

        assertFalse(state.isLoading)
        assertEquals("Kotlin", state.link?.title)
        assertEquals("Dev", state.category?.name)
        assertEquals(listOf("t1"), state.linkTags.map { it.id })
    }

    @Test
    fun missingLinkShowsAnError() = runTest {
        assertEquals("Link not found", open(linkId = "missing").state.error)
    }

    @Test
    fun togglesAreSaved() = runTest {
        val screen = open()

        screen.send(LinkDetailEvent.ToggleFavorite, LinkDetailEvent.ToggleArchived, LinkDetailEvent.MarkOpened)

        with(links.link("a")) {
            assertTrue(isFavorite)
            assertTrue(isArchived)
            assertTrue(isOpened)
        }
    }

    @Test
    fun failedSaveRollsBackAndShowsTheError() = runTest {
        val screen = open()
        links.failWrites = true

        screen.send(LinkDetailEvent.ToggleFavorite)

        assertFalse(screen.state.link!!.isFavorite)
        assertEquals(FAILURE, screen.state.error)
        screen.send(LinkDetailEvent.ClearError)
        assertNull(screen.state.error)
    }

    @Test
    fun renameTrimsAndIgnoresBlank() = runTest {
        val screen = open()

        screen.send(LinkDetailEvent.Rename("  "))
        assertEquals("Kotlin", links.link("a").title)

        screen.send(LinkDetailEvent.Rename("  Kotlin docs "))
        assertEquals("Kotlin docs", links.link("a").title)
    }

    @Test
    fun setCategoryAssignsAndClears() = runTest {
        val screen = open()

        screen.send(LinkDetailEvent.SetCategory("c"))
        assertEquals("c", screen.state.categoryId)

        screen.send(LinkDetailEvent.SetCategory(null))
        assertNull(screen.state.categoryId)
    }

    @Test
    fun setTagsAddsAndRemovesTheDifference() = runTest {
        tags.linkTags.value = mapOf("a" to setOf("t1"))
        val screen = open()

        screen.send(LinkDetailEvent.SetTags(setOf("t2")))

        assertEquals(setOf("t2"), tags.linkTags.value["a"])
        assertEquals(listOf("t2"), screen.state.linkTags.map { it.id })
    }

    @Test
    fun noteIsSavedOnceTypingStops() = runTest {
        val screen = open()

        screen.send(LinkDetailEvent.NoteChanged("dr"), LinkDetailEvent.NoteChanged("draft"))

        assertEquals("draft", links.link("a").description)
        assertEquals("draft", screen.state.link?.description)
        assertFalse(screen.state.noteSaving)
    }

    @Test
    fun deleteRemovesTheLink() = runTest {
        val screen = open()

        screen.send(LinkDetailEvent.Delete)

        assertTrue(screen.state.isDeleted)
        assertEquals(emptyList(), links.links.value)
    }

    @Test
    fun failedDeleteKeepsTheLink() = runTest {
        val screen = open()
        links.failWrites = true

        screen.send(LinkDetailEvent.Delete)

        assertFalse(screen.state.isDeleted)
        assertEquals(FAILURE, screen.state.error)
    }
}
