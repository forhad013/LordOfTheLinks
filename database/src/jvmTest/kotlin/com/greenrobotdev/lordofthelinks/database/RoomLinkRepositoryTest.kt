package com.greenrobotdev.lordofthelinks.database

import com.greenrobotdev.lordofthelinks.database.repository.RoomLinkRepository
import com.greenrobotdev.lordofthelinks.database.repository.RoomTagRepository
import com.greenrobotdev.lordofthelinks.database.room.LinkDatabase
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomLinkRepositoryTest {

    private lateinit var db: LinkDatabase
    private lateinit var repo: RoomLinkRepository
    private lateinit var tags: RoomTagRepository

    @BeforeTest
    fun setup() {
        db = inMemoryDb()
        repo = RoomLinkRepository(db.linkDao(), db.tagDao(), db.categoryDao())
        tags = RoomTagRepository(db.tagDao(), db.linkTagDao())
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun addLinkFillsInIdAndCreatedAt() = runTest {
        val saved = repo.addLink(Link(id = "", title = "Kotlin", url = "https://kotlinlang.org")).first().getOrThrow()

        assertTrue(saved.id.isNotEmpty())
        assertNotNull(saved.createdAt)
        assertEquals(listOf(saved), repo.getLinks().first().getOrThrow())
    }

    @Test
    fun addLinkKeepsAGivenIdAndCreatedAt() = runTest {
        val saved = repo.addLink(Link(id = "a", title = "a", url = "u", createdAt = 42)).first().getOrThrow()

        assertEquals("a", saved.id)
        assertEquals(42L, saved.createdAt)
    }

    @Test
    fun getLinksReEmitsWhenALinkIsAdded() = runTest {
        val emissions = backgroundScope.emissionsOf(repo.getLinks())
        assertEquals(emptyList(), emissions.receive().getOrThrow())

        repo.addLink(Link(id = "a", title = "a", url = "u"))

        assertEquals(listOf("a"), emissions.receive().getOrThrow().map { it.id })
    }

    @Test
    fun linksWithTagsCarryTagNamesAndReEmitWhenATagIsAssigned() = runTest {
        repo.addLink(Link(id = "a", title = "a", url = "u"))
        tags.addTag(Tag(id = "t", name = "kotlin", count = 0))
        val emissions = backgroundScope.emissionsOf(repo.getLinksWithTags())
        assertEquals(emptyList(), emissions.receive().getOrThrow().single().tags)

        tags.assignTagToLink("a", "t").first().getOrThrow()

        assertEquals(listOf("kotlin"), emissions.receive().getOrThrow().single().tags)
    }

    @Test
    fun updateLinkSavesChangesAndKeepsTags() = runTest {
        val link = repo.addLink(Link(id = "a", title = "Old", url = "u")).first().getOrThrow()
        tags.addTag(Tag(id = "t", name = "kotlin", count = 0))
        tags.assignTagToLink("a", "t").first().getOrThrow()

        repo.updateLink(link.copy(title = "New", isArchived = true)).first().getOrThrow()

        val stored = repo.getLinksWithTags().first().getOrThrow().single()
        assertEquals("New", stored.title)
        assertTrue(stored.isArchived)
        assertEquals(listOf("kotlin"), stored.tags)
    }

    @Test
    fun toggleFavoriteReturnsTheUpdatedLink() = runTest {
        repo.addLink(Link(id = "a", title = "a", url = "u"))

        assertTrue(repo.toggleFavorite("a").first().getOrThrow().isFavorite)
    }

    @Test
    fun toggleFavoriteOnAMissingLinkFails() = runTest {
        assertTrue(repo.toggleFavorite("missing").first().isFailure)
    }

    @Test
    fun markOpenedSetImageUrlAndDelete() = runTest {
        repo.addLink(Link(id = "a", title = "a", url = "u"))

        repo.markOpened("a").first().getOrThrow()
        repo.setImageUrl("a", "https://example.com/og.png").first().getOrThrow()
        val stored = repo.getLinks().first().getOrThrow().single()
        assertTrue(stored.isOpened)
        assertEquals("https://example.com/og.png", stored.imageUrl)

        repo.deleteLink("a").first().getOrThrow()
        assertEquals(emptyList(), repo.getLinks().first().getOrThrow())
    }
}
