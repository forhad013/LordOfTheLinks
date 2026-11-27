package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker

import com.greenrobotdev.lordofthelinks.bookmarks.domain.DeletePhase
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.MAX_CATEGORY_DEPTH
import com.greenrobotdev.lordofthelinks.testing.FAILURE
import com.greenrobotdev.lordofthelinks.testing.FakeCategoryRepository
import com.greenrobotdev.lordofthelinks.testing.present
import com.greenrobotdev.lordofthelinks.model.Category
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryPickerPresenterTest {

    // health › sleep › naps, plus work
    private val repo = FakeCategoryRepository(
        Category(id = "health", name = "Health"),
        Category(id = "sleep", name = "Sleep", parentId = "health"),
        Category(id = "naps", name = "Naps", parentId = "sleep"),
        Category(id = "work", name = "Work"),
    )

    private suspend fun TestScope.open(linkId: String? = "link", initialSelection: String? = null) =
        present { events -> CategoryPickerPresenter(CategoryPickerState(), events, linkId, initialSelection, repo) }

    private fun ids() = repo.categories.value.map { it.id }.toSet()

    @Test
    fun selectsTheLinksCategoryAndExpandsItsAncestors() = runTest {
        repo.linkCategories.value = mapOf("link" to "naps")

        val state = open().state

        assertEquals("naps", state.selectedCategoryId)
        assertTrue(setOf("health", "sleep").all { it in state.expandedIds })
    }

    @Test
    fun useThisAssignsTheSelectedCategory() = runTest {
        val picker = open()

        picker.send(CategoryPickerEvent.Select("work"), CategoryPickerEvent.UseThis)

        assertEquals(mapOf("link" to "work"), repo.linkCategories.value)
        assertTrue(picker.state.applied)
    }

    @Test
    fun useThisWithNothingSelectedClearsTheCategory() = runTest {
        repo.linkCategories.value = mapOf("link" to "work")
        val picker = open()

        picker.send(CategoryPickerEvent.Select(null), CategoryPickerEvent.UseThis)

        assertEquals(emptyMap(), repo.linkCategories.value)
    }

    @Test
    fun failedAssignShowsTheErrorAndStaysOpen() = runTest {
        val picker = open()
        repo.failWrites = true

        picker.send(CategoryPickerEvent.Select("work"), CategoryPickerEvent.UseThis)

        assertEquals(FAILURE, picker.state.error)
        assertEquals(false, picker.state.applied)
    }

    @Test
    fun createFromSearchAddsUnderTheSelection() = runTest {
        val picker = open()

        picker.send(CategoryPickerEvent.Select("work"), CategoryPickerEvent.Search(" Reports "), CategoryPickerEvent.CreateFromSearch)

        val created = repo.categories.value.single { it.name == "Reports" }
        assertEquals("work", created.parentId)
        assertEquals(created.id, picker.state.selectedCategoryId)
        assertEquals("", picker.state.query)
    }

    @Test
    fun siblingWithTheSameNameIsRejected() = runTest {
        val picker = open()

        picker.send(CategoryPickerEvent.AddTopLevel, CategoryPickerEvent.DialogName("work"))

        assertNotNull(picker.state.error)
        assertEquals(4, repo.categories.value.size)
    }

    @Test
    fun cannotGoDeeperThanTheLimit() = runTest {
        // naps is level 3; add levels 4 and 5, then a 6th must fail
        val picker = open(initialSelection = "naps")
        picker.send(CategoryPickerEvent.AddSub, CategoryPickerEvent.DialogName("L4"))
        picker.send(CategoryPickerEvent.AddSub, CategoryPickerEvent.DialogName("L5"))

        picker.send(CategoryPickerEvent.AddSub, CategoryPickerEvent.DialogName("L6"))

        assertEquals("Categories can be up to $MAX_CATEGORY_DEPTH levels deep", picker.state.error)
        assertEquals(6, repo.categories.value.size)
    }

    @Test
    fun renameTheSelectedCategory() = runTest {
        val picker = open(initialSelection = "work")

        picker.send(CategoryPickerEvent.Rename)
        assertEquals("Work", picker.state.dialog?.initialName)
        picker.send(CategoryPickerEvent.DialogName("Job"))

        assertEquals("Job", repo.categories.value.single { it.id == "work" }.name)
        assertNull(picker.state.dialog)
    }

    @Test
    fun deletingALeafAfterTheUndoWindow() = runTest {
        val picker = open(initialSelection = "work")

        picker.send(CategoryPickerEvent.Remove)
        assertEquals(DeletePhase.CONFIRM, picker.state.deletePhase)
        picker.send(CategoryPickerEvent.SwipeComplete)

        assertEquals(setOf("health", "sleep", "naps"), ids())
        assertNull(picker.state.selectedCategoryId)
        assertEquals(DeletePhase.NONE, picker.state.deletePhase)
    }

    @Test
    fun undoInsideTheWindowKeepsTheCategory() = runTest {
        val picker = open(initialSelection = "work")

        picker.send(CategoryPickerEvent.Remove)
        picker.sendTogether(CategoryPickerEvent.SwipeComplete, CategoryPickerEvent.UndoDelete)

        assertEquals(4, repo.categories.value.size)
        assertEquals("work", picker.state.selectedCategoryId)
    }

    @Test
    fun deletingAParentCanKeepItsChildren() = runTest {
        val picker = open(initialSelection = "sleep")

        picker.send(
            CategoryPickerEvent.Remove,
            CategoryPickerEvent.ParentDeleteChoice(ParentDeleteChoiceKind.KEEP_CHILDREN),
            CategoryPickerEvent.ConfirmRemove,
        )

        assertEquals(setOf("health", "naps", "work"), ids())
        assertEquals("health", repo.categories.value.single { it.id == "naps" }.parentId)
        assertEquals("health", picker.state.selectedCategoryId) // falls back to the parent
    }

    @Test
    fun deletingAParentCanTakeItsChildren() = runTest {
        val picker = open(initialSelection = "health")

        picker.send(
            CategoryPickerEvent.Remove,
            CategoryPickerEvent.ParentDeleteChoice(ParentDeleteChoiceKind.DELETE_ALL),
            CategoryPickerEvent.ConfirmRemove,
        )

        assertEquals(setOf("work"), ids())
    }

    @Test
    fun filterModeTicksSeveral() = runTest {
        val picker = open(linkId = null)

        picker.send(
            CategoryPickerEvent.ToggleChecked("work"),
            CategoryPickerEvent.ToggleChecked("sleep"),
            CategoryPickerEvent.ToggleChecked("work"),
        )
        assertEquals(setOf("sleep"), picker.state.checkedIds)

        picker.send(CategoryPickerEvent.ClearChecked, CategoryPickerEvent.UseThis)
        assertEquals(emptySet(), picker.state.checkedIds)
        assertTrue(picker.state.applied) // no link: nothing assigned
        assertEquals(emptyMap(), repo.linkCategories.value)
    }
}
