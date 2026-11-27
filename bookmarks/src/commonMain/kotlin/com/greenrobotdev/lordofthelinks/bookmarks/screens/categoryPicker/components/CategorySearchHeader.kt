package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerState
import com.greenrobotdev.lordofthelinks.design.components.layout.AppLazyRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.components.feedback.ErrorMessageCard
import com.greenrobotdev.lordofthelinks.design.components.textfield.SearchField

/** Search field, error and the recently used categories. */
@Composable
internal fun CategorySearchHeader(
    state: CategoryPickerState,
    onEvent: (CategoryPickerEvent) -> Unit,
    isEditing: Boolean,
    selectOnly: Boolean,
) {
    // Search / create — filled pill field like the mock
    SearchField(
        query = state.query,
        onQueryChange = { onEvent(CategoryPickerEvent.Search(it)) },
        placeholder = "Search or create a category",
        singleLine = true,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    state.error?.let { error ->
        ErrorMessageCard(error = error, onDismiss = { onEvent(CategoryPickerEvent.ClearError) })
    }

    // Recent: label and chips share one scrolling line
    if (state.browsing && state.recent.isNotEmpty() && !isEditing) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Recent",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            AppLazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.recent, key = { it.id }) { category ->
                    RecentChip(
                        text = category.path(state.categories),
                        onClick = {
                            onEvent(if (selectOnly) CategoryPickerEvent.ToggleChecked(category.id) else CategoryPickerEvent.Select(category.id))
                        },
                    )
                }
            }
        }
    }
}

/** Plain outlined pill used in the Recent row (mock: white fill, light outline, dark text, no check). */
@Composable
private fun RecentChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
