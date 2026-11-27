package com.greenrobotdev.lordofthelinks.bookmarks.screens.reminders

import com.greenrobotdev.lordofthelinks.model.Link
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

data class RemindersState(
    val isLoading: Boolean = true,
    val links: List<Link> = emptyList(),
)

sealed interface RemindersEvent {
    /** Swipe: drop the reminder, keep the link. */
    data class Clear(val linkId: String) : RemindersEvent
    /** Snackbar Undo: write the reminder back as it was. */
    data class Restore(val link: Link) : RemindersEvent
}

/**
 * Urgency section a reminder falls under, in display order: Overdue, Today, Tomorrow,
 * This week, Later. (This week = the next 7 days after tomorrow, matching the mockup.)
 */
@OptIn(ExperimentalTime::class)
internal fun reminderSection(
    reminderAt: Long,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val target = Instant.fromEpochMilliseconds(reminderAt).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
    return when {
        target < today -> "Overdue"
        target == today -> "Today"
        target == today.plus(DatePeriod(days = 1)) -> "Tomorrow"
        target <= today.plus(DatePeriod(days = 7)) -> "This week"
        else -> "Later"
    }
}
