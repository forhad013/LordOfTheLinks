package com.greenrobotdev.lordofthelinks.bookmarks.components.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon

/**
 * One tree/search result row (mock: 50dp tall). A 40dp gutter holds the chevron; each level
 * indents the label 20dp. The selected row gets a 14dp-rounded primaryContainer fill that
 * starts at the label's level.
 */
@Composable
internal fun SelectableRow(
    text: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    expandable: Boolean = false,
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
    muted: Boolean = false,
    isDeleting: Boolean = false,
    isSwiping: Boolean = false,
    disabled: Boolean = false,
    /** Gutter content for non-expandable rows (e.g. the tag picker's check mark). */
    leading: (@Composable () -> Unit)? = null,
) {
    val labelColor = when {
        isDeleting -> MaterialTheme.colorScheme.onErrorContainer
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        muted -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .then(
                when {
                    isSwiping -> Modifier.alpha(0.45f).scale(0.96f)
                    disabled -> Modifier.alpha(0.38f)
                    else -> Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !disabled,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width((40 + depth * 20).dp).fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (expandable) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "$text, ${if (expanded) "expanded" else "collapsed"}, ${count ?: 0} links",
                        color = if (isDeleting) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                leading?.let { Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { it() } }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        isDeleting -> MaterialTheme.colorScheme.errorContainer
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = when {
                    isDeleting -> FontWeight.ExtraBold
                    selected -> FontWeight.Bold
                    muted -> FontWeight.Medium
                    else -> FontWeight.SemiBold
                },
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textDecoration = if (isSwiping) TextDecoration.LineThrough else TextDecoration.None,
            )
            count?.let {
                Text(
                    "[$it]",
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        isDeleting -> MaterialTheme.colorScheme.onErrorContainer
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
