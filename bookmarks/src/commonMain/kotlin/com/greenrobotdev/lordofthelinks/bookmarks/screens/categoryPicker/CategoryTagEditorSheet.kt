package com.greenrobotdev.lordofthelinks.bookmarks.screens.categoryPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.bookmarks.screens.add.components.TagInput
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.model.Tag

/**
 * Tag-editing sheet for one category (Manage › selected category › Tags, design:
 * manage_categories_tag_edit): toggling a tag assigns/removes it on the category,
 * typing creates a new tag and assigns it. Links in the category inherit the tags.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryTagEditorSheet(
    category: Category?,
    tags: List<Tag>,
    assignedNames: Set<String>,
    onToggle: (tagId: String) -> Unit,
    onCreate: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    category?.name ?: "Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Links in this category inherit its tags when you search by tag.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Type to create + assign a new tag
            TagInput(onAdd = onCreate)

            // Every tag: tap toggles it on this category
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                tags.forEach { tag ->
                    val assigned = tag.name in assignedNames
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (assigned) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(onClickLabel = "Toggle #${tag.name}") { onToggle(tag.id) }
                            .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (assigned) {
                            AppIcon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                        } else {
                            AppIcon(
                                Icons.Filled.Tag,
                                contentDescription = null,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            "#${tag.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (assigned) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (tags.isEmpty()) {
                Text(
                    "No tags yet — type one above to create the first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
