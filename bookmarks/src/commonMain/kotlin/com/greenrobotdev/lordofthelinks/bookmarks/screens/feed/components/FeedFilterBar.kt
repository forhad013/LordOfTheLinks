package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon

// Feed pieces (design: Feed)

/**
 * "All saves" row right above the list: Filters sits next to what it changes.
 * [picked] is the row of chosen category/tag chips, shown under it.
 */
@Composable
internal fun AllSavesHeader(
    activeFilters: Int,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier,
    picked: @Composable () -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "All saves",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() },
            )
            FiltersButton(activeCount = activeFilters, onClick = onFilters)
        }
        picked()
    }
}

/** "Filters" button in the All saves row; filled with a count badge when any filter is active. */
@Composable
internal fun FiltersButton(activeCount: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val active = activeCount > 0
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer)
            .clickable(role = Role.Button, onClickLabel = "Open filters", onClick = onClick)
            .semantics { contentDescription = if (active) "Filters, $activeCount active" else "Filters" }
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIcon(Icons.Filled.Tune, contentDescription = null, color = fg, modifier = Modifier.size(20.dp))
        if (active) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("$activeCount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Text("Filters", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

/** A picked category or tag on the Feed; ✕ removes just that filter. */
@Composable
internal fun SelectedFilterChip(text: String, isTag: Boolean, onRemove: () -> Unit) {
    val bg = if (isTag) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
    val fg = if (isTag) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(role = Role.Button, onClickLabel = "Remove filter $text", onClick = onRemove)
            .heightIn(min = 36.dp)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = fg, maxLines = 1)
        AppIcon(Icons.Filled.Close, contentDescription = null, color = fg, modifier = Modifier.size(16.dp))
    }
}
