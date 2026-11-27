package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed.components

import com.greenrobotdev.lordofthelinks.bookmarks.components.link.typeLook
import com.greenrobotdev.lordofthelinks.bookmarks.domain.savedAtLabel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.bookmarks.domain.WebLinkHandler
import com.greenrobotdev.lordofthelinks.design.motion.rememberReducedMotion
import com.greenrobotdev.lordofthelinks.design.motion.AppMotion
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import com.greenrobotdev.lordofthelinks.bookmarks.components.link.LinkPreviewImage
import com.greenrobotdev.lordofthelinks.design.components.layout.AppLazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon

// Feed pieces (design: Feed)

/**
 * "Pick up where you left off": the newest unopened saves as a sideways row of large cards.
 * Tapping one opens the link, which marks it opened, so it leaves the row. "Mark as opened" does the
 * same without opening it.
 */
@Composable
internal fun PickUpStrip(
    links: List<Link>,
    unopenedCount: Int,
    onOpen: (Link) -> Unit,
    onMarkOpened: (Link) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Pick up where you left off",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f).padding(start = 4.dp).semantics { heading() },
            )
            Text(
                "$unopenedCount unopened",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppLazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(links, key = { it.id }) { link ->
                PickUpCard(link, onOpen = { onOpen(link) }, onMarkOpened = { onMarkOpened(link) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun PickUpCard(link: Link, onOpen: () -> Unit, onMarkOpened: () -> Unit, modifier: Modifier = Modifier) {
    val noPreview = link.title.isBlank() || link.title == link.url
    val look = typeLook(if (noPreview) "none" else link.type)
    val meta = listOfNotNull(WebLinkHandler.hostOf(link.url), savedAtLabel(link.createdAt)).joinToString(" · ")
    Column(
        modifier = modifier
            .width(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClickLabel = "Open link", onClick = onOpen),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(96.dp).background(look.bg),
            contentAlignment = Alignment.Center,
        ) {
            LinkPreviewImage(
                imageUrl = link.previewImage,
                modifier = Modifier.matchParentSize(),
                fallback = { AppIcon(look.icon, contentDescription = null, color = look.fg, modifier = Modifier.size(28.dp)) },
            )
            MarkOpenedCheck(onMarked = onMarkOpened, modifier = Modifier.align(Alignment.TopEnd))
        }
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                meta,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (noPreview) link.url else link.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                minLines = 2, // cards in the row line up
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The pick-up card's "Mark as opened" check: gray while unopened. A tap fills it with the brand
 * color, the check pops (a bouncy spring) with a haptic, and only then [onMarked] runs, so the card
 * leaves after the user has seen it land. With animations off it changes at once.
 */
@Composable
private fun MarkOpenedCheck(onMarked: () -> Unit, modifier: Modifier = Modifier) {
    var done by remember { mutableStateOf(false) }
    val reduced = rememberReducedMotion()
    val haptics = LocalHapticFeedback.current
    val marked by rememberUpdatedState(onMarked)
    val fill by animateColorAsState(
        if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        AppMotion.effectsFast(), label = "check fill",
    )
    val tint by animateColorAsState(
        if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        AppMotion.effectsFast(), label = "check tint",
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(done) {
        if (!done) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        if (!reduced) {
            // Hero moment, so a bouncy spring rather than the calm motion-scheme default
            pop.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            delay(150)
        }
        marked()
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = !done, role = Role.Button, onClickLabel = "Mark as opened") { done = true }
            .semantics { contentDescription = if (done) "Marked as opened" else "Mark as opened" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(CircleShape)
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(Icons.Filled.Check, contentDescription = null, color = tint, modifier = Modifier.size(18.dp))
        }
    }
}
