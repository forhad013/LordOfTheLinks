package com.greenrobotdev.lordofthelinks.bookmarks.screens.library

import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components.CategoryTreeRow
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components.LibraryEmptyHint
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components.RemindersTile
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components.SectionLabel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components.SmartTile
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components.TagTile
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.foundation.layout.size
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.SavedScope
import com.greenrobotdev.lordofthelinks.design.components.layout.AppLazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenrobotdev.lordofthelinks.model.Category
import com.greenrobotdev.lordofthelinks.design.theme.LordsExtraColors

/**
 * Library tab base (design: Library categories & tags):
 * 2x2 smart-list tiles, category tree card, tags. Manage opens the manage screen.
 */
@Composable
fun LibraryScreen(
    onManage: () -> Unit,
    onOpenScope: (SavedScope) -> Unit,
    /** Opens the dedicated Reminders tab (falls back to the smart list when null). */
    onOpenReminders: (() -> Unit)? = null,
) {
    val viewModel: LibraryViewModel = viewModel { LibraryViewModel() }
    val state by viewModel.models.collectAsState()

    AppLazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                // Same pill style as the mock's "+ Category" (creation lives in Manage)
                Text(
                    "Manage",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onManage)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Smart lists — 2x2 colored tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SmartTile(
                        SavedScope.Unopened,
                        "Unopened",
                        "${state.unopenedCount} links",
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        Modifier.weight(1f),
                        onOpenScope
                    )
                    SmartTile(
                        SavedScope.Favorites,
                        "Favorites",
                        "${state.favoritesCount} links",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                        Modifier.weight(1f),
                        onOpenScope
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SmartTile(
                        SavedScope.Archived,
                        "Archived",
                        "${state.archivedCount} links",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                        Modifier.weight(1f),
                        onOpenScope
                    )
                    SmartTile(
                        SavedScope.Uncategorized,
                        "Uncategorized",
                        "${state.uncategorizedCount} links",
                        LordsExtraColors.successContainer,
                        LordsExtraColors.onSuccessContainer,
                        Modifier.weight(1f),
                        onOpenScope
                    )
                }
                RemindersTile(
                    count = state.remindersCount,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpenReminders?.invoke() ?: onOpenScope(SavedScope.Reminders) },
                )
            }
        }

        item { SectionLabel("Categories", Modifier.padding(top = 8.dp)) }

        // Category tree card, or how to make the first category
        if (state.categories.isEmpty()) {
            item {
                LibraryEmptyHint(
                    icon = Icons.Filled.FolderOpen,
                    title = "No categories yet",
                    message = "Group links by topic, like Dev › Android. Create your first category in Manage, or while saving a link.",
                    actionLabel = "Add a category",
                    onAction = onManage,
                )
            }
        }
        if (state.categories.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 6.dp)
                ) {
                    state.categories.filter { it.parentId == null }.forEach { category ->
                        CategoryTreeRow(category, state.categories, state.categoryTagNames, onOpen = { onOpenScope(SavedScope.InCategory(it)) })
                    }
                }
            }
        }

        // Tags — two-column grid of white pills, or how to make the first tag
        item { SectionLabel("Tags", Modifier.padding(top = 8.dp)) }
        if (state.tags.isEmpty()) {
            item {
                LibraryEmptyHint(
                    icon = Icons.Filled.Tag,
                    title = "No tags yet",
                    message = "Tags are quick labels like #kotlin or #recipes. Add them in Manage, or while saving a link.",
                    actionLabel = "Add a tag",
                    onAction = onManage,
                )
            }
        }
        if (state.tags.isNotEmpty()) {
            items(state.tags.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { tag -> TagTile("#${tag.name}", Modifier.weight(1f)) { onOpenScope(SavedScope.WithTag(tag.name)) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
