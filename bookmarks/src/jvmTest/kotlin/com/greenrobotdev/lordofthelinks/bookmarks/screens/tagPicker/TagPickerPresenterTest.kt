package com.greenrobotdev.lordofthelinks.bookmarks.screens.tagPicker

import com.greenrobotdev.lordofthelinks.testing.FAILURE
import com.greenrobotdev.lordofthelinks.testing.FakeTagRepository
import com.greenrobotdev.lordofthelinks.testing.present
import com.greenrobotdev.lordofthelinks.bookmarks.domain.DeletePhase
import com.greenrobotdev.lordofthelinks.model.Tag
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagPickerPresenterTest {

    private val repo = FakeTagRepository(Tag(id = "k", name = "kotlin", count = 0), Tag(id = "a", name = "android", count = 0))

    private suspend fun TestScope.open(multiSelect: Boolean = true, checked: Set<String> = emptySet()) =
        present { events -> TagPickerPresenter(events, multiSelect, checked, repo) }

    private fun names() = repo.tags.value.map { it.name }.sorted()

    @Test
    fun loadsTagsSortedByName() = runTest {
        val state = open().state

        assertEquals(listOf("android", "kotlin"), state.tags.map { it.name })
    }

    @Test
    fun tapTogglesChecksInMultiSelect() = runTest {
        val picker = open(checked = setOf("k"))

        picker.send(TagPickerEvent.Tap("a"), TagPickerEvent.Tap("k"))

        assertEquals(setOf("a"), picker.state.checkedIds)
        assertEquals("k", picker.state.focusedId)
    }

    @Test
    fun tapOnlyFocusesInManageMode() = runTest {
        val picker = open(multiSelect = false)

        picker.send(TagPickerEvent.Tap("a"))
        assertEquals("a", picker.state.focusedId)
        assertEquals(emptySet(), picker.state.checkedIds)

        picker.send(TagPickerEvent.Tap("a"))
        assertNull(picker.state.focusedId)
    }

    @Test
    fun searchFiltersVisibleTags() = runTest {
        val picker = open()

        picker.send(TagPickerEvent.Search(" KOT "))

        assertEquals(listOf("kotlin"), picker.state.visibleTags.map { it.name })
    }

    @Test
    fun createFromSearchAddsAndChecksTheTag() = runTest {
        val picker = open()

        picker.send(TagPickerEvent.Search("compose"), TagPickerEvent.CreateFromSearch)

        val created = repo.tags.value.single { it.name == "compose" }
        assertTrue(created.id in picker.state.checkedIds)
        assertEquals("", picker.state.query)
    }

    @Test
    fun duplicateNameIsRejected() = runTest {
        val picker = open()

        picker.send(TagPickerEvent.StartCreate, TagPickerEvent.SubmitName("Kotlin"))

        assertNotNull(picker.state.error)
        assertEquals(listOf("android", "kotlin"), names())
    }

    @Test
    fun failedCreateShowsTheError() = runTest {
        val picker = open()
        repo.failWrites = true

        picker.send(TagPickerEvent.StartCreate, TagPickerEvent.SubmitName("compose"))

        assertEquals(FAILURE, picker.state.error)
        picker.send(TagPickerEvent.ClearError)
        assertNull(picker.state.error)
    }

    @Test
    fun renameTheFocusedTag() = runTest {
        val picker = open()

        picker.send(TagPickerEvent.Tap("k"), TagPickerEvent.StartRename, TagPickerEvent.SubmitName(" kotlin-lang "))

        assertEquals(listOf("android", "kotlin-lang"), names())
        assertNull(picker.state.renamingId)
    }

    @Test
    fun swipedTagIsDeletedAfterTheUndoWindow() = runTest {
        val picker = open(checked = setOf("k"))

        picker.send(TagPickerEvent.Tap("a"), TagPickerEvent.Tap("k"), TagPickerEvent.Remove)
        assertEquals(DeletePhase.CONFIRM, picker.state.deletePhase)
        picker.send(TagPickerEvent.SwipeComplete)

        assertEquals(listOf("android"), names())
        assertEquals(DeletePhase.NONE, picker.state.deletePhase)
        assertEquals(setOf("a"), picker.state.checkedIds)
    }

    @Test
    fun undoKeepsTheTag() = runTest {
        val picker = open(multiSelect = false)

        picker.send(TagPickerEvent.Tap("k"), TagPickerEvent.Remove)
        picker.sendTogether(TagPickerEvent.SwipeComplete, TagPickerEvent.UndoDelete)

        assertEquals(listOf("android", "kotlin"), names())
        assertEquals(DeletePhase.NONE, picker.state.deletePhase)
    }

    @Test
    fun doneAndClear() = runTest {
        val picker = open(checked = setOf("k", "a"))

        picker.send(TagPickerEvent.ClearChecked, TagPickerEvent.Done)

        assertEquals(emptySet(), picker.state.checkedIds)
        assertTrue(picker.state.applied)
    }
}
