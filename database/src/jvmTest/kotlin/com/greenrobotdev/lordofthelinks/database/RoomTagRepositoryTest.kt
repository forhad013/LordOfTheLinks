package com.greenrobotdev.lordofthelinks.database

import androidx.room.Room
import com.greenrobotdev.lordofthelinks.database.repository.RoomTagRepository
import com.greenrobotdev.lordofthelinks.database.room.LinkDatabase
import com.greenrobotdev.lordofthelinks.database.room.TagEntity
import com.greenrobotdev.lordofthelinks.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomTagRepositoryTest {

    private lateinit var db: LinkDatabase
    private lateinit var repo: RoomTagRepository

    @BeforeTest
    fun setup() {
        db = Room.inMemoryDatabaseBuilder<LinkDatabase>()
            .setDriver(androidx.sqlite.driver.bundled.BundledSQLiteDriver())
            .build()
        repo = RoomTagRepository(db.tagDao(), db.linkTagDao())
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun renameKeepsCreatedAt() = runTest {
        db.tagDao().insertTag(TagEntity(id = "t", name = "old", createdAt = 1_000L, updatedAt = 1_000L))

        repo.updateTag(Tag(id = "t", name = "new", count = 0)).first().getOrThrow()

        val stored = db.tagDao().getTagById("t").first()!!
        assertEquals("new", stored.name)
        assertEquals(1_000L, stored.createdAt)
        assertTrue(stored.updatedAt > 1_000L)
    }

    @Test
    fun updatingAMissingTagFails() = runTest {
        assertTrue(repo.updateTag(Tag(id = "nope", name = "x", count = 0)).first().isFailure)
    }

    @Test
    fun addTagGeneratesAnIdWhenEmpty() = runTest {
        val saved = repo.addTag(Tag(id = "", name = "kotlin", count = 0)).first().getOrThrow()

        assertTrue(saved.id.isNotEmpty())
        assertEquals("kotlin", repo.getTagById(saved.id).first().getOrThrow()?.name)
    }

    @Test
    fun tagsForLinkReEmitOnAssignAndRemove() = runTest {
        db.linkDao().insertLink(link("a"))
        repo.addTag(Tag(id = "t", name = "kotlin", count = 0))
        val emissions = backgroundScope.emissionsOf(repo.getTagsForLink("a"))
        assertEquals(emptyList(), emissions.receive().getOrThrow())

        repo.assignTagToLink("a", "t").first().getOrThrow()
        assertEquals(listOf("t"), emissions.receive().getOrThrow().map { it.id })

        repo.removeTagFromLink("a", "t").first().getOrThrow()
        assertEquals(emptyList(), emissions.receive().getOrThrow())
    }

    @Test
    fun tagsCarryTheirLinkCount() = runTest {
        db.linkDao().insertLinks(listOf(link("a"), link("b")))
        repo.addTag(Tag(id = "t", name = "kotlin", count = 0))
        repo.assignTagToLink("a", "t").first().getOrThrow()
        repo.assignTagToLink("b", "t").first().getOrThrow()

        assertEquals(2, repo.getTags().first().getOrThrow().single().count)
    }

    @Test
    fun assigningATagToAMissingLinkFails() = runTest {
        repo.addTag(Tag(id = "t", name = "kotlin", count = 0))

        assertTrue(repo.assignTagToLink("missing", "t").first().isFailure)
    }

    @Test
    fun deleteTagRemovesIt() = runTest {
        repo.addTag(Tag(id = "t", name = "kotlin", count = 0))

        repo.deleteTag("t").first().getOrThrow()

        assertEquals(emptyList(), repo.getTags().first().getOrThrow())
    }
}
