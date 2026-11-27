package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components

import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerState
import com.greenrobotdev.lordofthelinks.bookmarks.domain.DELETE_UNDO_MS
import com.greenrobotdev.lordofthelinks.bookmarks.domain.DeletePhase
import com.greenrobotdev.lordofthelinks.bookmarks.domain.category.canAddChildUnder
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.components.layout.AppActionBar
import com.greenrobotdev.lordofthelinks.design.components.layout.AppBarAction
import com.greenrobotdev.lordofthelinks.design.components.layout.AppBarPill
import com.greenrobotdev.lordofthelinks.design.components.layout.AppUndoBar
import com.greenrobotdev.lordofthelinks.design.components.layout.SwipeConfirmBar

/** The "Selected: …" line, depending on the current mode. */
@Composable
internal fun CategorySelectionFooter(state: CategoryPickerState, edit: CategoryEditState, selectOnly: Boolean) {
    val creatingTopLevel = edit.creatingTopLevel
    val creatingSubOf = edit.creatingSubOf
    val renamingId = edit.renamingId
    // Footer
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            when {
                state.deletePhase != DeletePhase.NONE -> "Deleting: "
                creatingTopLevel -> "New top-level: "
                creatingSubOf != null -> "New sub in: "
                renamingId != null -> "Renaming: "
                selectOnly -> "Filtering by: "
                else -> "Selected: "
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            when {
                state.deletePhase != DeletePhase.NONE -> state.deletingCategory?.name ?: ""
                creatingTopLevel -> edit.newName.trim()
                creatingSubOf != null -> edit.subParent?.path(state.categories) ?: ""
                renamingId != null -> edit.renamingCategory?.path(state.categories) ?: ""
                selectOnly -> state.categories.filter { it.id in state.checkedIds }
                    .joinToString(", ") { it.path(state.categories) }.ifEmpty { "all categories" }
                else -> state.selectedPath ?: "No category"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The bottom bar for the current mode. */
@Composable
internal fun CategoryActionBar(
    state: CategoryPickerState,
    onEvent: (CategoryPickerEvent) -> Unit,
    edit: CategoryEditState,
    showUseThis: Boolean,
    selectOnly: Boolean,
) {
    val creatingTopLevel = edit.creatingTopLevel
    val creatingSubOf = edit.creatingSubOf
    val renamingId = edit.renamingId
    val selected = state.selected
    val hasChildren = state.deletingCategory?.let { dc -> state.allCategories.any { it.parentId == dc.id } } == true

    // Bar: mode-dependent
    when {
        state.deletePhase == DeletePhase.UNDO -> AppUndoBar(
            title = "${state.deletingCategory?.name ?: ""} deleted",
            subtitle = if (state.deletedLinkCount > 0)
                "${state.deletedLinkCount} link${if (state.deletedLinkCount != 1) "s" else ""} now uncategorized" else null,
            onUndo = { onEvent(CategoryPickerEvent.UndoDelete) },
            durationMillis = DELETE_UNDO_MS.toInt(),
        )
        state.deletePhase == DeletePhase.CONFIRM || state.deletePhase == DeletePhase.SWIPING ->
            SwipeConfirmBar(
                onCancel = { onEvent(CategoryPickerEvent.UndoDelete) },
                onConfirm = { onEvent(CategoryPickerEvent.SwipeComplete) },
                enabled = !hasChildren || state.parentDeleteChoice != null,
            )
        creatingTopLevel || creatingSubOf != null -> AppActionBar {
            AppBarAction(Icons.Filled.Close, "Cancel", { onEvent(CategoryPickerEvent.CancelDialog) }, enabled = true)
            AppBarPill(
                "Create",
                onClick = if (creatingTopLevel) edit::createTopLevel else edit::createSub,
                enabled = if (creatingTopLevel) edit.canCreate else edit.canCreateSub,
                modifier = Modifier.weight(1f),
            )
        }
        renamingId != null -> AppActionBar {
            AppBarAction(Icons.Filled.Close, "Cancel", { onEvent(CategoryPickerEvent.CancelDialog) }, enabled = true)
            AppBarPill("Save name", onClick = edit::doRename, enabled = edit.canRename, modifier = Modifier.weight(1f))
        }
        selectOnly -> AppActionBar {
            AppBarAction(
                Icons.Filled.Close, "Clear", { onEvent(CategoryPickerEvent.ClearChecked) },
                enabled = state.checkedIds.isNotEmpty(),
            )
            val n = state.checkedIds.size
            AppBarPill(
                text = if (n == 0) "Show all" else "Apply ($n)",
                onClick = { onEvent(CategoryPickerEvent.UseThis) },
                modifier = Modifier.weight(1f),
            )
        }
        else -> AppActionBar {
            // Without the confirm pill (e.g. Manage's embedded tab), spread actions evenly
            val spread = if (showUseThis) Modifier else Modifier.weight(1f)
            AppBarAction(
                Icons.Filled.CreateNewFolder, "Add sub",
                { onEvent(CategoryPickerEvent.AddSub) },
                enabled = selected != null && canAddChildUnder(selected.id, state.categories),
                modifier = spread,
            )
            AppBarAction(Icons.Filled.Edit, "Rename", { onEvent(CategoryPickerEvent.Rename) }, enabled = selected != null, modifier = spread)
            AppBarAction(Icons.Filled.Delete, "Remove", { onEvent(CategoryPickerEvent.Remove) }, enabled = selected != null, isError = true, modifier = spread)
            if (showUseThis) AppBarPill(text = "Use this", onClick = { onEvent(CategoryPickerEvent.UseThis) }, modifier = Modifier.weight(1f))
        }
    }
}
