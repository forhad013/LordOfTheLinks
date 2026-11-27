package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Radio choice card for how to handle a parent category's children. */
@Composable
internal fun ParentChoiceCard(
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
