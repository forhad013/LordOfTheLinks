package com.greenrobotdev.lordofthelinks.bookmarks.screens.library.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.model.Category

@Composable
internal fun CategoryTreeRow(
    category: Category,
    all: List<Category>,
    categoryTagNames: Map<String, Set<String>>,
    onOpen: (String) -> Unit,
    depth: Int = 0,
) {
    val children = all.filter { it.parentId == category.id }
    var expanded by rememberSaveable(category.id) { mutableStateOf(false) }

    Column(Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                // The row opens the category's saves; the chevron expands its subcategories
                .clickable(onClickLabel = "Open ${category.name}") { onOpen(category.id) }
                .padding(start = (2 + depth * 20).dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Chevron gutter: always reserved so leaf names line up with their siblings
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .then(if (children.isNotEmpty()) Modifier.clickable { expanded = !expanded } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (children.isNotEmpty()) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "${category.name}, ${if (expanded) "expanded" else "collapsed"}",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "[${category.count}]",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Assigned tags as compact chips (mock: #kotlin #material3), max 3 then +N
        val tags = categoryTagNames[category.id].orEmpty()
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(start = (46 + depth * 20).dp, end = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tags.take(3).forEach { tagName ->
                    Text(
                        "#$tagName",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                if (tags.size > 3) {
                    Text(
                        "+${tags.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (expanded) {
            children.forEach { child -> CategoryTreeRow(child, all, categoryTagNames, onOpen, depth + 1) }
        }
    }
}
