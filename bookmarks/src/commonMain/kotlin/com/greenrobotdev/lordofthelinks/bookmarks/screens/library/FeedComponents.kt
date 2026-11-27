package com.greenrobotdev.lordofthelinks.bookmarks.screens.library

import com.greenrobotdev.lordofthelinks.bookmarks.components.link.typeLook
import com.greenrobotdev.lordofthelinks.bookmarks.domain.savedAtLabel
import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import com.greenrobotdev.lordofthelinks.bookmarks.domain.WebLinkHandler
import androidx.compose.material.icons.filled.Inbox
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
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

/**
 * How far the Feed header has collapsed, driven by the list's scroll: scrolling down collapses it
 * before the list moves, and it only expands again once the list is back at the top.
 */
@Stable
internal class CollapsingHeaderState(private val rangePx: Float) {
    private var offsetPx by mutableFloatStateOf(0f)

    /** 0 = expanded, 1 = collapsed. Read it in layout/draw lambdas so scrolling skips recomposition. */
    val fraction: Float get() = if (rangePx == 0f) 0f else -offsetPx / rangePx

    val nestedScroll = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
            if (available.y < 0f) Offset(0f, consume(available.y)) else Offset.Zero

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
            if (available.y > 0f) Offset(0f, consume(available.y)) else Offset.Zero
    }

    private fun consume(dy: Float): Float {
        val next = (offsetPx + dy).coerceIn(-rangePx, 0f)
        val used = next - offsetPx
        offsetPx = next
        return used
    }
}

@Composable
internal fun rememberCollapsingHeaderState(range: Dp = 56.dp): CollapsingHeaderState {
    val px = with(LocalDensity.current) { range.toPx() }
    return remember(px) { CollapsingHeaderState(px) }
}

/**
 * Lays the content out at [scale] of its size (the freed space goes to the list) and draws it scaled
 * from [origin]. Both happen in layout/draw, so a changing scale doesn't recompose.
 */
private fun Modifier.scaleAway(scale: () -> Float, origin: TransformOrigin, fade: Boolean = false) =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val s = scale()
        val height = (placeable.height * s).roundToInt()
        layout(placeable.width, height) {
            placeable.placeWithLayer(0, (height - placeable.height) / 2) {
                scaleX = s
                scaleY = s
                transformOrigin = origin
                if (fade) alpha = s
            }
        }
    }

/**
 * "Lords of Links" / "Your saves" title at the top of the Feed, with the inbox progress ring. As
 * [collapse] goes 0 → 1, the overline fades out and the title and ring shrink.
 */
@Composable
internal fun FeedHeader(opened: Int, total: Int, celebrate: Boolean, modifier: Modifier = Modifier, collapse: () -> Float = { 0f }) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "Lords of Links",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.scaleAway({ 1f - collapse() }, TransformOrigin(0f, 0.5f), fade = true),
            )
            Text(
                "Your saves",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.scaleAway({ 1f - 0.3f * collapse() }, TransformOrigin(0f, 0.5f)),
            )
        }
        if (total > 0) {
            InboxProgress(opened, total, celebrate, Modifier.scaleAway({ 1f - 0.25f * collapse() }, TransformOrigin(1f, 0.5f)))
        }
    }
}

/**
 * Ring that fills as saves get opened: "9 left · 3 of 12 opened". At zero it turns green "Done"
 * only right after the user cleared the last one ([celebrate]); otherwise it's a calm gray
 * "0 new · 19 opened" (e.g. after a restore, where everything was already opened).
 */
@Composable
private fun InboxProgress(opened: Int, total: Int, celebrate: Boolean, modifier: Modifier = Modifier) {
    val done = opened >= total
    val fraction by animateFloatAsState(opened.toFloat() / total, label = "inbox progress")
    val track = MaterialTheme.colorScheme.primaryContainer
    val fill = when {
        done && celebrate -> LordsExtraColors.success
        done -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (done) "Nothing new, all $total saves opened" else "${total - opened} unopened, $opened of $total opened"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(Modifier.size(40.dp)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(fill, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column {
            Text(
                when { done && celebrate -> "Done"; done -> "0 new"; else -> "${total - opened} left" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                when { done && celebrate -> "all opened"; done -> "$total opened"; else -> "$opened of $total opened" },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
