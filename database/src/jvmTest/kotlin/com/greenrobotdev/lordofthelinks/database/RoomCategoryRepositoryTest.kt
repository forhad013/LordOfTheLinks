package com.greenrobotdev.lordofthelinks.database

import com.greenrobotdev.lordofthelinks.database.repository.RoomCategoryRepository
import com.greenrobotdev.lordofthelinks.database.room.LinkDatabase
import com.greenrobotdev.lordofthelinks.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomCategoryRepositoryTest {

    private lateinit var db: LinkDatabase
    private lateinit var repo: RoomCategoryRepository

    @BeforeTest
    fun setup() {
        db = inMemoryDb()
        repo = RoomCategoryRepository(db.categoryDao())
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun add(id: String, parentId: String? = null) =
        repo.addCategory(Category(id = id, name = id, parentId = parentId)).first().getOrThrow()

    private suspend fun names() = repo.getCategories().first().getOrThrow().map { it.name }.sorted()

    @Test
    fun addCategoryGeneratesAnIdWhenEmpty() = runTest {
        val saved = repo.addCategory(Category(id = "", name = "Health")).first().getOrThrow()

        assertTrue(saved.id.isNotEmpty())
        assertEquals("Health", repo.getCategoryById(saved.id).first().getOrThrow()?.name)
    }

    @Test
    fun getCategoriesReEmitsWhenOneIsAdded() = runTest {
        val emissions = backgroundScope.emissionsOf(repo.getCategories())
        assertEquals(emptyList(), emissions.receive().getOrThrow())

        add("health")

        assertEquals(listOf("health"), emissions.receive().getOrThrow().map { it.id })
    }

    @Test
    fun renameChangesTheName() = runTest {
        add("old")

        repo.renameCategory("old", "New").first().getOrThrow()

        assertEquals("New", repo.getCategoryById("old").first().getOrThrow()?.name)
    }

    @Test
    fun renamingAMissingCategoryFails() = runTest {
        assertTrue(repo.renameCategory("missing", "x").first().isFailure)
        assertTrue(repo.deleteCategoryKeepChildren("missing").first().isFailure)
    }

    @Test
    fun daoRuleViolationsComeBackAsFailures() = runTest {
        add("parent")
        add("child", "parent")

        assertTrue(repo.deleteCategory("parent").first().isFailure, "has a subcategory")
        assertTrue(repo.moveCategory("parent", "child").first().isFailure, "under its own descendant")
        assertEquals(listOf("child", "parent"), names())
    }

    @Test
    fun deleteKeepChildrenMovesChildrenUpALevel() = runTest {
        add("root")
        add("middle", "root")
        add("leaf", "middle")

        repo.deleteCategoryKeepChildren("middle").first().getOrThrow()

        assertEquals("root", repo.getCategoryById("leaf").first().getOrThrow()?.parentId)
        assertEquals(listOf("leaf", "root"), names())
    }

    @Test
    fun deleteTreeRemovesDescendants() = runTest {
        add("root")
        add("child", "root")
        add("other")

        repo.deleteCategoryTree("root").first().getOrThrow()

        assertEquals(listOf("other"), names())
    }

    @Test
    fun linkCategoryAssignmentsShowUpAndReEmit() = runTest {
        db.linkDao().insertLink(link("a"))
        add("c1")
        add("c2")
        val emissions = backgroundScope.emissionsOf(repo.getLinkCategoryIds())
        assertEquals(emptyMap(), emissions.receive().getOrThrow())

        repo.assignLinkToCategory("a", "c1").first().getOrThrow()
        assertEquals(mapOf("a" to "c1"), emissions.receive().getOrThrow())

        repo.assignLinkToCategory("a", "c2").first().getOrThrow() // one category per link: replaces c1
        assertEquals("c2", repo.getCategoryForLink("a").first().getOrThrow()?.id)

        repo.clearLinkCategory("a").first().getOrThrow()
        assertNull(repo.getCategoryForLink("a").first().getOrThrow())
    }

    @Test
    fun countsIncludeLinksInEachCategory() = runTest {
        db.linkDao().insertLinks(listOf(link("a"), link("b")))
        add("c")
        repo.assignLinkToCategory("a", "c").first().getOrThrow()
        repo.assignLinkToCategory("b", "c").first().getOrThrow()

        assertEquals(2, repo.getCategoriesWithCounts().first().getOrThrow().single().count)
    }
}
