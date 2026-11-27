package com.greenrobotdev.lordofthelinks.bookmarks.screens.feed.components

import com.greenrobotdev.lordofthelinks.bookmarks.screens.saved.title
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
