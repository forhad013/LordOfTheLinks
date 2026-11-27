package com.greenrobotdev.lordofthelinks.bookmarks.screens.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenrobotdev.lordofthelinks.bookmarks.domain.hostOf
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.reminderLabel
import com.greenrobotdev.lordofthelinks.design.components.layout.AppLazyColumn
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import com.greenrobotdev.lordofthelinks.model.Link
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch

/**
 * The Reminders tab (design: reminders_tab): every link with a reminder, grouped into
 * urgency sections (Overdue, Today, Tomorrow, This week, Later), soonest first.
 * Tapping a row opens the link; swiping right clears the reminder (Undo in the snackbar).
 */
@OptIn(ExperimentalTime::class)
@Composable
fun RemindersScreen() {
    val viewModel: RemindersViewModel = viewModel { RemindersViewModel() }
    val state by viewModel.models.collectAsState()
    val uriHandler = LocalUriHandler.current
    val nowMs = remember { Clock.System.now().toEpochMilliseconds() }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppIcon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        "Reminders",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }

            if (!state.isLoading && state.links.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 20.dp, bottomEnd = 44.dp, bottomStart = 20.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIcon(
                                Icons.Filled.Alarm,
                                contentDescription = null,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        Text(
                            "No reminders yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Set a reminder while saving a link, or from a link's details.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Sorted soonest first, so each urgency section is one consecutive run
            state.links.groupBy { reminderSection(it.reminderAt!!, nowMs) }.forEach { (section, sectionLinks) ->
                item(key = "section-$section") {
                    Text(
                        section.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (section == "Overdue") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp).semantics { heading() }.animateItem(),
                    )
                }
                items(sectionLinks, key = { it.id }) { link ->
                    // Same fresh-state trick as the Feed's SwipeableFeedCard: a saveable swipe state
                    // would still be "swiped" after Undo and clear the reminder again.
                    val threshold = SwipeToDismissBoxDefaults.positionalThreshold
                    val dismissState = remember {
                        SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, threshold)
                    }
                    val clear = {
                        viewModel.take(RemindersEvent.Clear(link.id))
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            if (snackbar.showSnackbar("Reminder removed", "Undo", duration = SnackbarDuration.Short) ==
                                SnackbarResult.ActionPerformed
                            ) {
                                viewModel.take(RemindersEvent.Restore(link))
                            }
                        }
                    }
                    // Reminder gone from the flow: reset so a returning row (Undo) starts settled
                    LaunchedEffect(link.reminderAt) {
                        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) dismissState.reset()
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        modifier = Modifier.clip(RoundedCornerShape(24.dp)).animateItem(),
                        enableDismissFromEndToStart = false,
                        onDismiss = { direction ->
                            if (direction == SwipeToDismissBoxValue.StartToEnd) clear()
                        },
                        backgroundContent = {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(
                                    Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    "Clear reminder",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        },
                    ) {
                        ReminderRow(
                            link = link,
                            overdue = section == "Overdue",
                            onOpen = { runCatching { uriHandler.openUri(link.url) } },
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

/** One reminder (mock: reminders_tab): tinted alarm badge, title, host, time chip. Tap opens the link. */
@Composable
private fun ReminderRow(
    link: Link,
    overdue: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val tintContainer = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Open link", onClick = onOpen)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Alarm badge: the mock's rounded-square tinted icon block
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tintContainer),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(
                Icons.Filled.Alarm,
                contentDescription = null,
                color = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                link.title.ifBlank { link.url },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                hostOf(link.url),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Time chip, right-aligned: the mock's pill with the reminder time
        Text(
            reminderLabel(link.reminderAt),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 2,
            textAlign = TextAlign.End,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(tintContainer.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
