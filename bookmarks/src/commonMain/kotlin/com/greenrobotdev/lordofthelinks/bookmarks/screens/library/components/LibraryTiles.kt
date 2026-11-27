package com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components

import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.layout.size
import com.greenrobotdev.lordofthelinks.bookmarks.motion.sharedScope
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.SavedScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
internal fun TagTile(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Open $text", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    )
}

@Composable
internal fun SmartTile(
    scope: SavedScope,
    label: String,
    countLabel: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onOpen: (SavedScope) -> Unit,
) {
    Column(
        modifier = modifier
            .height(84.dp)
            // Grows into the header of the list it opens
            .sharedScope(scope.key)
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .clickable(onClickLabel = "Open $label") { onOpen(scope) }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = content)
        Text(countLabel, style = MaterialTheme.typography.bodySmall, color = content)
    }
}

@Composable
internal fun RemindersTile(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
            .clickable(onClickLabel = "Open Reminders", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            Icons.Filled.Notifications,
            contentDescription = null,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            "Reminders",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
