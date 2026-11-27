package com.greenrobotdev.lordofthelinks.bookmarks.domain.reminder

import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** The previous reminder if it is still ahead, otherwise one hour from now. */
@OptIn(ExperimentalTime::class)
internal fun reminderPickerStart(currentReminderAt: Long?, nowMs: Long, zone: TimeZone): LocalDateTime {
    val ms = currentReminderAt?.takeIf { it > nowMs } ?: (nowMs + 3600_000L)
    return Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone)
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
