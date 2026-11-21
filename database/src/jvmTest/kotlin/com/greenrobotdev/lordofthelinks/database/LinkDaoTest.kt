package com.greenrobotdev.lordofthelinks.database

import com.greenrobotdev.lordofthelinks.database.room.CategoryEntity
import com.greenrobotdev.lordofthelinks.database.room.LinkCategoryEntity
import com.greenrobotdev.lordofthelinks.database.room.LinkDatabase
import com.greenrobotdev.lordofthelinks.database.room.LinkTagEntity
import com.greenrobotdev.lordofthelinks.database.room.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkDaoTest {

    private lateinit var db: LinkDatabase
    private val dao get() = db.linkDao()

    @BeforeTest
    fun setup() {
        db = inMemoryDb()
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertedLinkCanBeReadBack() = runTest {
        dao.insertLink(link("a", title = "Kotlin"))

        assertEquals("Kotlin", dao.getLinkById("a")?.title)
        assertNull(dao.getLinkById("missing"))
    }

    @Test
    fun allLinksAreNewestFirstWithInsertOrderBreakingTies() = runTest {
        dao.insertLinks(listOf(link("old", createdAt = 1), link("new", createdAt = 3), link("tieA", createdAt = 2), link("tieB", createdAt = 2)))

        assertEquals(listOf("new", "tieB", "tieA", "old"), dao.getAllLinks().first().map { it.id })
    }

    @Test
    fun toggleFavoriteFlipsTheFlag() = runTest {
        dao.insertLink(link("a"))

        dao.toggleFavorite("a")
        assertTrue(dao.getLinkById("a")!!.isFavorite)
        dao.toggleFavorite("a")
        assertEquals(false, dao.getLinkById("a")!!.isFavorite)
    }

    @Test
    fun statusUpdatesReportWhetherTheLinkExisted() = runTest {
        dao.insertLink(link("a"))

        assertEquals(1, dao.markOpened("a"))
        assertTrue(dao.getLinkById("a")!!.isOpened)
        assertEquals(0, dao.markOpened("missing"))
        assertEquals(0, dao.toggleFavorite("missing"))
    }

    @Test
    fun setImageUrlTouchesOnlyThatColumn() = runTest {
        dao.insertLink(link("a", title = "Original"))
        dao.updateLink(dao.getLinkById("a")!!.copy(title = "Edited by user", isArchived = true))

        dao.setImageUrl("a", "https://example.com/og.png")

        val stored = dao.getLinkById("a")!!
        assertEquals("https://example.com/og.png", stored.imageUrl)
        assertEquals("Edited by user", stored.title)
        assertTrue(stored.isArchived)
    }

    @Test
    fun updateKeepsTheLinksTagsAndCategory() = runTest {
        dao.insertLink(link("a"))
        tagAndCategorize("a")

        dao.updateLink(dao.getLinkById("a")!!.copy(title = "Renamed"))

        assertEquals(listOf("t"), db.linkTagDao().getTagIdsForLink("a").first())
        assertEquals("c", db.categoryDao().getCategoryForLink("a")?.id)
    }

    @Test
    fun deletingALinkRemovesItsTagAndCategoryRows() = runTest {
        dao.insertLink(link("a"))
        tagAndCategorize("a")

        assertEquals(1, dao.deleteLinkById("a"))

        assertNull(dao.getLinkById("a"))
        assertEquals(emptyList(), db.linkTagDao().getLinkIdsForTag("t").first())
        assertEquals(emptyMap(), db.categoryDao().getAllLinkCategories().first().associate { it.linkId to it.categoryId })
        // the tag and category themselves stay
        assertEquals("t", db.tagDao().getTagById("t").first()?.id)
        assertEquals("c", db.categoryDao().getCategoryByIdOnce("c")?.id)
    }

    private suspend fun tagAndCategorize(linkId: String) {
        db.tagDao().insertTag(TagEntity(id = "t", name = "t"))
        db.linkTagDao().assignTagToLink(LinkTagEntity(id = "lt", linkId = linkId, tagId = "t"))
        db.categoryDao().insertCategory(CategoryEntity(id = "c", name = "c"))
        db.categoryDao().assignLinkToCategory(LinkCategoryEntity(linkId = linkId, categoryId = "c"))
    }
}
