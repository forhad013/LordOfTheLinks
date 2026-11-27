package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker

import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components.CategoryActionBar
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components.CategoryDeletePanel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components.CategoryList
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components.CategorySearchHeader
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components.CategorySelectionFooter
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components.rememberCategoryEditState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Picker body shared by the sheet and full-screen hosts: search, recent chips, tree/search list,
 * footer, and the mode-dependent action bar.
 */
@Composable
internal fun CategoryPickerContent(
    state: CategoryPickerState,
    onEvent: (CategoryPickerEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showUseThis: Boolean = true,
    /** Filtering: pick one category only. No create, rename or delete; bar is Clear + Apply. */
    selectOnly: Boolean = false,
) {
    val edit = rememberCategoryEditState(state, onEvent)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CategorySearchHeader(state, onEvent, edit.isEditing, selectOnly)
        CategoryList(state, onEvent, edit, selectOnly, Modifier.weight(1f))
        CategorySelectionFooter(state, edit, selectOnly)
        CategoryDeletePanel(state, onEvent)
        CategoryActionBar(state, onEvent, edit, showUseThis, selectOnly)
    }
}
