package com.greenrobotdev.lordofthelinks.bookmarks.components.link

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.model.Link
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon

// Feed pieces (design: Feed)

/**
 * Feed card with swipe actions: right archives (the card leaves the list; Undo is offered by the
 * caller), left asks to delete (the card snaps back while the caller confirms). Archived links
 * (shown via Filters) can't be archived again.
 */
@Composable
internal fun SwipeableFeedCard(
    link: Link,
    onArchive: () -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Plain remember, not rememberSwipeToDismissBoxState: that one is saveable, and the list keeps saved
    // state per item key, so a card archived by a swipe came back from Undo still "swiped right", and
    // SwipeToDismissBox archived it again. A fresh state per appearance always starts settled.
    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
    val state = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold) }
    val scope = rememberCoroutineScope()
    // SwipeToDismissBox re-runs onDismiss whenever this lambda changes while the card is still swiped
    // away, so it must stay the same instance: a new one on each recomposition (Undo causes one)
    // archived the link again and again.
    val archive by rememberUpdatedState(onArchive)
    val requestDelete by rememberUpdatedState(onRequestDelete)
    val onDismiss: (SwipeToDismissBoxValue) -> Unit = remember(state, scope) {
        { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> archive()
                SwipeToDismissBoxValue.EndToStart -> {
                    requestDelete()
                    scope.launch { state.reset() } // stays put until the delete is confirmed
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    }
    // Archived but still listed (Filters include archived, or Undo while it was leaving): bring it back
    LaunchedEffect(link.isArchived) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) state.reset()
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier.clip(RoundedCornerShape(24.dp)),
        enableDismissFromStartToEnd = !link.isArchived,
        onDismiss = onDismiss,
        backgroundContent = {
            val archive = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val delete = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val bg = when {
                archive -> MaterialTheme.colorScheme.inverseSurface
                delete -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            val fg = if (archive) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onError
            Row(
                modifier = Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, if (delete) Alignment.End else Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (archive || delete) {
                    AppIcon(
                        if (archive) Icons.Outlined.Archive else Icons.Outlined.Delete,
                        contentDescription = null,
                        color = fg,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        if (archive) "Archive" else "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = fg,
                    )
                }
            }
        },
    ) { content() }
}
