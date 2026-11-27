package com.greenrobotdev.lordofthelinks.bookmarks.domain.reminder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class ReminderPickerStartTest {
    private val hour = 3600_000L

    @Test
    fun opensOnTheFutureReminderOtherwiseAnHourFromNow() {
        assertEquals(LocalDateTime(1970, 1, 1, 5, 0), reminderPickerStart(5 * hour, hour, TimeZone.UTC))
        assertEquals(LocalDateTime(1970, 1, 1, 3, 0), reminderPickerStart(hour, 2 * hour, TimeZone.UTC))
        assertEquals(LocalDateTime(1970, 1, 1, 3, 0), reminderPickerStart(null, 2 * hour, TimeZone.UTC))
    }
}
