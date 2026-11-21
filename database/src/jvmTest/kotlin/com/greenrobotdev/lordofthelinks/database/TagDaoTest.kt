package com.greenrobotdev.lordofthelinks.database

import com.greenrobotdev.lordofthelinks.database.room.LinkDatabase
import com.greenrobotdev.lordofthelinks.database.room.LinkTagEntity
import com.greenrobotdev.lordofthelinks.database.room.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagDaoTest {

    private lateinit var db: LinkDatabase
    private val tags get() = db.tagDao()
    private val linkTags get() = db.linkTagDao()

    @BeforeTest
    fun setup() {
        db = inMemoryDb()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun tag(id: String) = tags.insertTag(TagEntity(id = id, name = id))
    private suspend fun assign(linkId: String, tagId: String) =
        linkTags.assignTagToLink(LinkTagEntity(id = "$linkId-$tagId", linkId = linkId, tagId = tagId))

    @Test
    fun tagsAreSortedByName() = runTest {
        tag("zebra")
        tag("apple")

        assertEquals(listOf("apple", "zebra"), tags.getAllTags().first().map { it.name })
        assertEquals("apple", tags.getTagById("apple").first()?.name)
        assertNull(tags.getTagById("missing").first())
    }

    @Test
    fun tagsForLinkAndLinksForTagFollowAssignments() = runTest {
        db.linkDao().insertLinks(listOf(link("a", createdAt = 1), link("b", createdAt = 2)))
        tag("kotlin")
        tag("android")
        assign("a", "kotlin")
        assign("a", "android")
        assign("b", "kotlin")

        assertEquals(listOf("android", "kotlin"), tags.getTagsForLink("a").first().map { it.id })
        assertEquals(listOf("b", "a"), tags.getLinksForTag("kotlin").first().map { it.id })
        assertTrue(linkTags.hasLinkTag("b", "kotlin"))
        assertFalse(linkTags.hasLinkTag("b", "android"))
    }

    @Test
    fun countsIncludeUnusedTags() = runTest {
        db.linkDao().insertLinks(listOf(link("a"), link("b")))
        tag("used")
        tag("unused")
        assign("a", "used")
        assign("b", "used")

        val counts = tags.getAllTagsWithCounts().first().associate { it.id to it.linkCount }
        assertEquals(mapOf("unused" to 0, "used" to 2), counts)
        assertEquals(2, tags.getTagLinkCount("used").first())
    }

    @Test
    fun assigningTheSameTagTwiceKeepsOneRow() = runTest {
        db.linkDao().insertLink(link("a"))
        tag("t")

        assign("a", "t")
        linkTags.assignTagToLink(LinkTagEntity(id = "another-id", linkId = "a", tagId = "t"))

        assertEquals(listOf("t"), linkTags.getTagIdsForLink("a").first())
    }

    @Test
    fun removingATagFromALinkLeavesOtherAssignments() = runTest {
        db.linkDao().insertLink(link("a"))
        tag("x")
        tag("y")
        assign("a", "x")
        assign("a", "y")

        linkTags.removeTagFromLink("a", "x")
        assertEquals(listOf("y"), linkTags.getTagIdsForLink("a").first())

        linkTags.removeAllTagsFromLink("a")
        assertEquals(emptyList(), linkTags.getTagIdsForLink("a").first())
    }

    @Test
    fun deletingATagRemovesItsAssignmentsButNotTheLinks() = runTest {
        db.linkDao().insertLink(link("a"))
        tag("t")
        assign("a", "t")

        tags.deleteTagById("t")

        assertEquals(emptyList(), tags.getTagsForLink("a").first())
        assertEquals("a", db.linkDao().getLinkById("a")?.id)
    }
}
