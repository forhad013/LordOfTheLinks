package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components

import com.greenrobotdev.lordofthelinks.bookmarks.domain.feed.kind
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerDialog
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerState
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.validateCategoryName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState

/** The inline create / rename text being typed, shared by the list, the footer and the action bar. */
internal class CategoryEditState(
    private val state: CategoryPickerState,
    private val onEvent: (CategoryPickerEvent) -> Unit,
    newNameState: MutableState<String>,
    subNameState: MutableState<String>,
    renameNameState: MutableState<String>,
    val creatingTopLevel: Boolean,
    val creatingSubOf: String?,
    val renamingId: String?,
) {
    var newName by newNameState
    var subName by subNameState
    var renameName by renameNameState

    val isEditing = creatingTopLevel || creatingSubOf != null || renamingId != null

    // New top-level name
    val newNameTaken get() = newName.isNotBlank() && validateCategoryName(newName, null, state.categories) != null
    val canCreate get() = newName.isNotBlank() && !newNameTaken
    fun createTopLevel() { if (canCreate) onEvent(CategoryPickerEvent.DialogName(newName.trim())) }

    // Add sub-category name
    val subParent = creatingSubOf?.let { id -> state.categories.firstOrNull { it.id == id } }
    val subNameTaken get() = subName.isNotBlank() && creatingSubOf != null &&
        validateCategoryName(subName, creatingSubOf, state.categories) != null
    val canCreateSub get() = subName.isNotBlank() && !subNameTaken
    fun createSub() { if (canCreateSub) onEvent(CategoryPickerEvent.DialogName(subName.trim())) }

    // Rename
    val renamingCategory = renamingId?.let { id -> state.categories.firstOrNull { it.id == id } }
    val renameParent = renamingCategory?.parentId?.let { pid -> state.categories.firstOrNull { it.id == pid } }
    val renameNameTaken get() = renameName.isNotBlank() && renamingCategory != null &&
        validateCategoryName(renameName, renamingCategory.parentId, state.categories, excludeId = renamingId) != null
    val canRename get() = renameName.isNotBlank() && !renameNameTaken && renameName.trim() != (state.dialog?.initialName ?: "")
    fun doRename() { if (canRename) onEvent(CategoryPickerEvent.DialogName(renameName.trim())) }
}

@Composable
internal fun rememberCategoryEditState(
    state: CategoryPickerState,
    onEvent: (CategoryPickerEvent) -> Unit,
): CategoryEditState {
    // Inline editing modes derived from the dialog
    val creatingTopLevel = state.dialog?.let { it.kind == CategoryPickerDialog.Kind.ADD_SUB && it.categoryId == null } == true
    val creatingSubOf = state.dialog?.let { if (it.kind == CategoryPickerDialog.Kind.ADD_SUB && it.categoryId != null) it.categoryId else null }
    val renamingId = state.dialog?.let { if (it.kind == CategoryPickerDialog.Kind.RENAME) it.categoryId else null }
    val newName = remember(creatingTopLevel) { mutableStateOf("") }
    val subName = remember(creatingSubOf) { mutableStateOf("") }
    val renameName = remember(renamingId) { mutableStateOf(state.dialog?.initialName ?: "") }
    return CategoryEditState(state, onEvent, newName, subName, renameName, creatingTopLevel, creatingSubOf, renamingId)
}
