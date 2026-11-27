package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed.components

import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.bookmarks.domain.WebLinkHandler
import androidx.compose.material.icons.filled.Inbox
import com.greenrobotdev.lordofthelinks.design.motion.rememberReducedMotion
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import com.greenrobotdev.lordofthelinks.design.theme.LordsExtraColors

// Feed pieces (design: Feed)

/** The celebration, right after the user opened (or archived) the last unopened save. */
@Composable
internal fun AllCaughtUpCard(modifier: Modifier = Modifier) {
    // The check badge pops in as the card arrives (instant with animations off)
    val reduced = rememberReducedMotion()
    val pop = remember { Animatable(if (reduced) 1f else 0.4f) }
    LaunchedEffect(Unit) {
        if (!reduced) pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(LordsExtraColors.successContainer)
            .padding(horizontal = 18.dp, vertical = 20.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp))
                .background(LordsExtraColors.success),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Filled.Check, contentDescription = null, color = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "All caught up",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LordsExtraColors.onSuccessContainer,
            )
            Text(
                "You opened everything you saved. New saves show up here.",
                style = MaterialTheme.typography.bodySmall,
                color = LordsExtraColors.onSuccessContainer,
            )
        }
    }
}

/**
 * "You saved this 3 weeks ago": one forgotten save with Open, Archive and (when there are more)
 * a button that shows another.
 */
@Composable
internal fun ResurfaceCard(
    link: Link,
    label: String,
    categoryPath: String?,
    onOpen: () -> Unit,
    onArchive: () -> Unit,
    onShowAnother: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val fg = MaterialTheme.colorScheme.onSecondaryContainer
    val noPreview = link.title.isBlank() || link.title == link.url
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(Icons.Filled.History, contentDescription = null, color = fg, modifier = Modifier.size(18.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            if (noPreview) link.url else link.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = fg,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOfNotNull(WebLinkHandler.hostOf(link.url), categoryPath).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CardAction("Open", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, onOpen, Modifier.weight(1f))
            CardAction("Archive", MaterialTheme.colorScheme.surface, fg, onArchive, Modifier.weight(1f))
            if (onShowAnother != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onShowAnother)
                        .semantics { contentDescription = "Show another" },
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(Icons.Filled.Refresh, contentDescription = null, color = fg, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CardAction(text: String, container: Color, content: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = content)
    }
}

/** Nothing unopened, but not just cleared (after a restore, or on a later visit): calm, no celebration. */
@Composable
internal fun NothingNewCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Filled.Inbox, contentDescription = null, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Nothing new waiting", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Everything here is opened. Share a link from any app, or tap + to save one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
