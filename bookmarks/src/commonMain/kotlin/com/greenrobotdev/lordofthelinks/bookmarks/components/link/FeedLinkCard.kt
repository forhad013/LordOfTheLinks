package com.greenrobotdev.lordofthelinks.bookmarks.components.link

import com.greenrobotdev.lordofthelinks.bookmarks.domain.savedAtLabel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.bookmarks.domain.WebLinkHandler
import com.greenrobotdev.lordofthelinks.bookmarks.components.link.LinkPreviewImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.bookmarks.motion.sharedThumb
import com.greenrobotdev.lordofthelinks.bookmarks.motion.sharedTitle
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import com.greenrobotdev.lordofthelinks.design.theme.LordsExtraColors

// Feed pieces (design: Feed)

/**
 * One saved link. Tapping the card opens its details; the ↗ button opens the link itself.
 * A link whose title was never fetched shows its URL and "No preview yet".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeedLinkCard(
    link: Link,
    categoryPath: String?,
    onOpen: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = WebLinkHandler.hostOf(link.url)
    val noPreview = link.title.isBlank() || link.title == link.url
    val typeLabel = link.type.replaceFirstChar { it.uppercaseChar() }
    val meta = buildList {
        add(if (noPreview) host else "$typeLabel · $host")
        if (noPreview) add("No preview yet")
        // The TODAY / YESTERDAY header already says the day, so those cards show only the time
        savedAtLabel(link.createdAt, timeOnlyIfRecent = true)?.let(::add)
    }.joinToString(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Open details", onClick = onDetails)
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeThumb(type = if (noPreview) "none" else link.type, muted = link.isArchived, linkId = link.id, imageUrl = link.previewImage)

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!link.isOpened) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
                Text(
                    meta,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (link.isFavorite) {
                    AppIcon(Icons.Filled.Star, contentDescription = "Favorite", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                }
                if (link.isArchived) {
                    ArchivedPill()
                }
            }
            Text(
                if (noPreview) link.url else link.title,
                style = if (noPreview) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (link.isArchived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                // Moves into the Link details title (real titles only; the URL fallback differs there)
                modifier = if (noPreview) Modifier else Modifier.sharedTitle(link.id),
            )
            if (categoryPath != null || link.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categoryPath?.let {
                        MetaChip(it, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    link.tags.forEach {
                        MetaChip("#$it", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open link", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Thumbnail colors and icon per link type ("none" = no preview yet). */
internal data class TypeLook(val bg: Color, val fg: Color, val icon: ImageVector)

@Composable
internal fun typeLook(type: String): TypeLook = when (type) {
    "video" -> TypeLook(LordsExtraColors.mediaPlaceholder, MaterialTheme.colorScheme.secondary, Icons.Filled.PlayArrow)
    "social" -> TypeLook(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Icons.Outlined.ChatBubbleOutline)
    "article" -> TypeLook(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Icons.AutoMirrored.Outlined.Article)
    else -> TypeLook(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Filled.Link)
}

@Composable
private fun TypeThumb(type: String, muted: Boolean = false, linkId: String? = null, imageUrl: String? = null) {
    val look = typeLook(type)
    // Archived: same shape and icon, neutral colors
    val bg = if (muted) MaterialTheme.colorScheme.surfaceContainerHigh else look.bg
    val fg = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else look.fg
    Box(
        // Grows into the Link details badge when the card opens details
        modifier = (if (linkId != null) Modifier.sharedThumb(linkId) else Modifier)
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        LinkPreviewImage(
            imageUrl = imageUrl,
            modifier = Modifier.matchParentSize().graphicsLayer { if (muted && imageUrl != null) alpha = 0.5f },
            fallback = { AppIcon(look.icon, contentDescription = null, color = fg) },
        )
    }
}

@Composable
private fun ArchivedPill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Icons.Outlined.Archive, contentDescription = null, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
        Text("Archived", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun MetaChip(text: String, bg: Color, fg: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
