package com.greenrobotdev.lordofthelinks.bookmarks.domain

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

// Feed pieces (design: Feed)

/**
 * When a link was saved, in the device's time zone:
 * "Just now" (under a minute), "Today, 14:32", "Yesterday, 09:15", "Monday, 18:04" (last 6 days),
 * "12 Nov, 18:04" (this year), "12 Nov 2025" (older). null when unknown.
 * [timeOnlyIfRecent]: "14:32" instead of "Today, 14:32" / "Yesterday, 09:15", for lists that head those days.
 * shortcut: 24-hour clock and English names; switch to platform formatters if the app gets localized.
 */
@OptIn(ExperimentalTime::class)
internal fun savedAtLabel(
    epochMs: Long?,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
    timeOnlyIfRecent: Boolean = false,
): String? {
    if (epochMs == null || epochMs <= 0L) return null
    if (nowMs - epochMs in 0 until 60_000L) return "Just now"
    val then = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
    val time = "${then.hour.toString().padStart(2, '0')}:${then.minute.toString().padStart(2, '0')}"
    val daysAgo = then.date.daysUntil(today)
    val month = then.month.name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
    val day = then.date.day
    return when {
        daysAgo in 0..1 && timeOnlyIfRecent -> time
        daysAgo == 0 -> "Today, $time"
        daysAgo == 1 -> "Yesterday, $time"
        daysAgo in 2..6 -> "${then.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercaseChar() }}, $time"
        then.year == today.year -> "$day $month, $time"
        else -> "$day $month ${then.year}" // also covers future dates from a wrong clock
    }
}

@OptIn(ExperimentalTime::class)
internal fun reminderLabel(
    epochMs: Long?,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (epochMs == null || epochMs <= 0L) return ""
    val target = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
    val hour = target.hour
    val minuteStr = target.minute.toString().padStart(2, '0')
    val amPm = if (hour < 12) "AM" else "PM"
    val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    val time = "$h12:$minuteStr $amPm"
    val daysUntil = today.daysUntil(target.date)
    val month = target.month.name.take(3).lowercase().replaceFirstChar { it.uppercaseChar() }
    val dayOfWeek = target.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercaseChar() }
    return when {
        daysUntil < 0 -> "Overdue · $time"
        daysUntil == 0 -> "Today, $time"
        daysUntil == 1 -> "Tomorrow, $time"
        daysUntil in 2..6 -> "$dayOfWeek, $time"
        target.year == today.year -> "${target.date.day} $month, $time"
        else -> "${target.date.day} $month ${target.year}, $time"
    }
}
