package com.greenrobotdev.lordofthelinks.bookmarks.domain.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlinx.datetime.daysUntil
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val DAY_MS = 24 * 60 * 60 * 1000L

/** Heading of the resurfacing card: "You saved this 3 weeks ago", or a plain one when the date is unknown. */
fun resurfaceLabel(createdAt: Long?, nowMs: Long): String {
    if (createdAt == null || createdAt <= 0L) return "From your older saves"
    val days = ((nowMs - createdAt) / DAY_MS).toInt()
    fun ago(n: Int, unit: String) = "You saved this $n $unit${if (n == 1) "" else "s"} ago"
    return when {
        days < 14 -> ago(days, "day")
        days < 60 -> ago(days / 7, "week")
        days < 365 -> ago(days / 30, "month")
        else -> "You saved this over a year ago"
    }
}

/**
 * Feed section a save falls under: "Today", "Yesterday", "This week" (2–6 days ago) or "Earlier".
 * Undated saves go to "Earlier"; a future date from a wrong clock counts as "Today".
 */
@OptIn(ExperimentalTime::class)
internal fun feedSection(
    epochMs: Long?,
    nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (epochMs == null || epochMs <= 0L) return "Earlier"
    val then = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date
    val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
    return when (then.daysUntil(today)) {
        in Int.MIN_VALUE..0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> "This week"
        else -> "Earlier"
    }
}
