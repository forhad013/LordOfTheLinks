package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components

import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerState
import com.greenrobotdev.lordofthelinks.bookmarks.components.picker.CreateRow
import com.greenrobotdev.lordofthelinks.bookmarks.components.picker.InlineTreeField
import com.greenrobotdev.lordofthelinks.bookmarks.components.picker.SelectableRow
import com.greenrobotdev.lordofthelinks.bookmarks.domain.DeletePhase
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.TreeRow
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.flattenTree
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.needsCreateRow
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.searchRows
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.subtreeIds
import com.greenrobotdev.lordofthelinks.design.components.layout.AppLazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.components.button.DashedPillButton

/** One visible entry in the browse list: a category row, or an inline editing field. */
internal sealed interface PickerItem {
    val key: String
    data class Row(val row: TreeRow) : PickerItem { override val key = row.category.id }
    data class SubField(val depth: Int) : PickerItem { override val key = "add-sub-field" }
    data class RenameField(val depth: Int, val categoryId: String) : PickerItem { override val key = "rename-$categoryId" }
}

/** Interleave inline edit fields at the right tree position. */
internal fun buildPickerItems(rows: List<TreeRow>, creatingSubOf: String?, renamingId: String?): List<PickerItem> = buildList {
    val parentIdx = if (creatingSubOf != null) rows.indexOfFirst { it.category.id == creatingSubOf } else -1
    val parentDepth = if (parentIdx >= 0) rows[parentIdx].depth else -1
    val insertAfter = if (parentIdx >= 0) {
        var last = parentIdx
        for (i in parentIdx + 1 until rows.size) { if (rows[i].depth > parentDepth) last = i else break }
        last
    } else -1

    for ((i, row) in rows.withIndex()) {
        if (row.category.id == renamingId) add(PickerItem.RenameField(row.depth, row.category.id))
        else add(PickerItem.Row(row))
        if (i == insertAfter) add(PickerItem.SubField(parentDepth + 1))
    }
}

/** The category tree or search results, with the inline create and rename fields. */
@Composable
internal fun CategoryList(
    state: CategoryPickerState,
    onEvent: (CategoryPickerEvent) -> Unit,
    edit: CategoryEditState,
    selectOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    val creatingTopLevel = edit.creatingTopLevel
    val creatingSubOf = edit.creatingSubOf
    val renamingId = edit.renamingId
    val rows = if (state.browsing) flattenTree(state.categories, state.expandedIds)
    else searchRows(state.categories, state.query)
    // Delete flow: which rows are in the danger zone
    val deletingIds: Set<String> = remember(state.deletingCategory, state.allCategories) {
        val dc = state.deletingCategory ?: return@remember emptySet()
        subtreeIds(dc.id, state.allCategories)
    }
    val pickerItems = if (state.browsing) buildPickerItems(rows, creatingSubOf, renamingId) else emptyList()
    val listState = rememberLazyListState()
    LaunchedEffect(creatingTopLevel) {
        if (creatingTopLevel) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
    }
    LaunchedEffect(creatingSubOf) {
        if (creatingSubOf != null) {
            val idx = pickerItems.indexOfFirst { it is PickerItem.SubField }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    // Browse tree / search results
    AppLazyColumn(
        modifier = modifier.statusBarsPadding(),
        state = listState
    ) {
        if (state.browsing) {
            items(pickerItems, key = { it.key }) { item ->
                when (item) {
                    is PickerItem.Row -> {
                        val catId = item.row.category.id
                        val isDeleteTarget = state.deletePhase != DeletePhase.NONE && catId in deletingIds
                        SelectableRow(
                            text = item.row.category.name,
                            count = item.row.category.count,
                            depth = item.row.depth,
                            selected = if (selectOnly) item.row.category.id in state.checkedIds
                            else !edit.isEditing && item.row.category.id == state.selectedCategoryId,
                            expandable = item.row.hasChildren,
                            expanded = item.row.category.id in state.expandedIds,
                            onClick = {
                                if (selectOnly) onEvent(CategoryPickerEvent.ToggleChecked(item.row.category.id))
                                else if (!edit.isEditing) onEvent(CategoryPickerEvent.Select(item.row.category.id))
                            },
                            onToggle = { onEvent(CategoryPickerEvent.ToggleExpand(item.row.category.id)) },
                            isDeleting = isDeleteTarget && state.deletePhase == DeletePhase.CONFIRM,
                            isSwiping = isDeleteTarget && state.deletePhase == DeletePhase.SWIPING,
                            disabled = edit.isEditing,
                        )
                    }
                    is PickerItem.SubField -> InlineTreeField(
                        name = edit.subName,
                        onNameChange = { edit.subName = it },
                        taken = edit.subNameTaken,
                        hint = "Inside ${edit.subParent?.name ?: "?"} · you can nest deeper after",
                        errorHint = "\"${edit.subName.trim()}\" already exists in ${edit.subParent?.name ?: "this category"}",
                        onDone = edit::createSub,
                        depth = item.depth,
                    )
                    is PickerItem.RenameField -> InlineTreeField(
                        name = edit.renameName,
                        onNameChange = { edit.renameName = it },
                        taken = edit.renameNameTaken,
                        hint = if (edit.renameParent != null) "Inside ${edit.renameParent.name} · its links keep this category"
                        else "Top level · its links keep this category",
                        errorHint = "\"${edit.renameName.trim()}\" already exists ${
                            if (edit.renameParent != null) "in ${edit.renameParent.name}" else "at the top level"
                        }",
                        onDone = edit::doRename,
                        depth = item.depth,
                        icon = Icons.Filled.Edit,
                    )
                }
            }
        } else {
            items(rows, key = { it.category.id }) { row ->
                SelectableRow(
                    text = row.breadcrumb,
                    count = row.category.count,
                    selected = if (selectOnly) row.category.id in state.checkedIds
                    else !edit.isEditing && row.category.id == state.selectedCategoryId,
                    onClick = {
                        if (selectOnly) onEvent(CategoryPickerEvent.ToggleChecked(row.category.id))
                        else if (!edit.isEditing) onEvent(CategoryPickerEvent.Select(row.category.id))
                    },
                    disabled = edit.isEditing,
                )
            }
            if (!selectOnly && needsCreateRow(state.categories, state.query)) {
                item(key = "create") {
                    CreateRow(
                        text = state.query.trim(),
                        onClick = { onEvent(CategoryPickerEvent.CreateFromSearch) },
                    )
                }
            }
            if (rows.isEmpty() && (selectOnly || !needsCreateRow(state.categories, state.query))) {
                item(key = "empty") {
                    Text(
                        "No matching category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }

        if (state.browsing && !selectOnly) {
            item(key = "new-top-level") {
                if (creatingTopLevel) {
                    InlineTreeField(
                        name = edit.newName,
                        onNameChange = { edit.newName = it },
                        taken = edit.newNameTaken,
                        hint = "Top level · you can add subcategories after",
                        errorHint = "\"${edit.newName.trim()}\" already exists at the top level",
                        onDone = edit::createTopLevel,
                        depth = 0,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    DashedPillButton(
                        text = "New top-level category",
                        onClick = { onEvent(CategoryPickerEvent.AddTopLevel) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
