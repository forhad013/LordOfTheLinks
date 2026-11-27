package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.components

import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerEvent
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.CategoryPickerState
import com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker.ParentDeleteChoiceKind
import com.greenrobotdev.lordofthelinks.bookmarks.components.picker.ImpactCard
import com.greenrobotdev.lordofthelinks.bookmarks.domain.DeletePhase
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

/** What deleting will do, and for a parent category what happens to its children. */
@Composable
internal fun CategoryDeletePanel(state: CategoryPickerState, onEvent: (CategoryPickerEvent) -> Unit) {
    val hasChildren = state.deletingCategory?.let { dc ->
        state.allCategories.any { it.parentId == dc.id }
    } == true
    val childNames = remember(state.deletingCategory, state.allCategories) {
        val dc = state.deletingCategory ?: return@remember emptyList()
        state.allCategories.filter { it.parentId == dc.id }.map { it.name }
    }

    // Delete flow: impact card + parent choice
    if (state.deletePhase == DeletePhase.CONFIRM || state.deletePhase == DeletePhase.SWIPING) {
        state.deletingCategory?.let { dc ->
            ImpactCard(
                categoryName = dc.name,
                linkCount = state.deletedLinkCount,
                isSwiping = state.deletePhase == DeletePhase.SWIPING,
            )
            if (hasChildren) {
                ParentChoiceCard(
                    parentName = dc.name,
                    childNames = childNames,
                    choice = state.parentDeleteChoice,
                    onChoice = { onEvent(CategoryPickerEvent.ParentDeleteChoice(it)) },
                )
            }
        }
    }
}

/** Radio choice card for how to handle a parent category's children. */
@Composable
private fun ParentChoiceCard(
    parentName: String,
    childNames: List<String>,
    choice: ParentDeleteChoiceKind?,
    onChoice: (ParentDeleteChoiceKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val childLabel = when {
        childNames.size == 1 -> childNames.first()
        childNames.size <= 3 -> childNames.joinToString(", ")
        else -> "${childNames.take(2).joinToString(", ")} +${childNames.size - 2}"
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Keep children option (recommended)
        val keepSelected = choice == ParentDeleteChoiceKind.KEEP_CHILDREN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (keepSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .border(
                    1.dp,
                    if (keepSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                )
                .clickable { onChoice(ParentDeleteChoiceKind.KEEP_CHILDREN) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = keepSelected,
                onClick = { onChoice(ParentDeleteChoiceKind.KEEP_CHILDREN) },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
            )
            Column {
                Text(
                    "Keep $childLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Move up a level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Delete all option
        val deleteAllSelected = choice == ParentDeleteChoiceKind.DELETE_ALL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (deleteAllSelected) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                .border(
                    1.dp,
                    if (deleteAllSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                )
                .clickable { onChoice(ParentDeleteChoiceKind.DELETE_ALL) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = deleteAllSelected,
                onClick = { onChoice(ParentDeleteChoiceKind.DELETE_ALL) },
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error),
            )
            Text(
                "Delete $parentName and $childLabel",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
