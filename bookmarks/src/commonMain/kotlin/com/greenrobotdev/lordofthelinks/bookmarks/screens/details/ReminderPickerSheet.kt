package com.greenrobotdev.lordofthelinks.bookmarks.screens.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenrobotdev.lordofthelinks.bookmarks.screens.library.reminderLabel
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ReminderPickerSheet(
    currentReminderAt: Long?,
    onSet: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val zone = remember { TimeZone.currentSystemDefault() }
    val nowMs = remember { Clock.System.now().toEpochMilliseconds() }
    val nowDt = remember { Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone) }
    val today = remember { nowDt.date }
    fun at(date: LocalDate, hour: Int) = LocalDateTime(date, LocalTime(hour, 0)).toInstant(zone).toEpochMilliseconds()
    // Both pickers open on the previously chosen date and time
    val start = remember(currentReminderAt) { reminderPickerStart(currentReminderAt, nowMs, zone) }
    val currentHour = remember { nowDt.hour }

    // Custom flow: pick a date first, then a time; both dialogs share the sheet's lifecycle
    var showDatePicker by remember { mutableStateOf(false) }
    var pickedDateMs by remember { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Remind me",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (currentHour < 19) {
                val evening = at(today, 19)
                ReminderOption(
                    icon = Icons.Default.Nightlight,
                    label = "Later today",
                    detail = "7:00 PM",
                    onClick = { onSet(evening); onDismiss() },
                )
            }

            val tomorrowMs = at(today.plus(DatePeriod(days = 1)), 9)
            ReminderOption(
                icon = Icons.Default.Today,
                label = "Tomorrow",
                detail = "9:00 AM",
                onClick = { onSet(tomorrowMs); onDismiss() },
            )

            val daysUntilSaturday = (6 - today.dayOfWeek.isoDayNumber).let { if (it <= 0) 7 + it else it }
            val weekendMs = at(today.plus(DatePeriod(days = daysUntilSaturday)), 10)
            ReminderOption(
                icon = Icons.Default.Weekend,
                label = "This weekend",
                detail = "Saturday, 10:00 AM",
                onClick = { onSet(weekendMs); onDismiss() },
            )

            val nextWeekMs = at(today.plus(DatePeriod(days = 8 - today.dayOfWeek.isoDayNumber)), 9)
            ReminderOption(
                icon = Icons.Default.CalendarMonth,
                label = "Next week",
                detail = "Monday, 9:00 AM",
                onClick = { onSet(nextWeekMs); onDismiss() },
            )

            // Custom: any date (today or later) + any time
            ReminderOption(
                icon = Icons.Default.Alarm,
                label = "Pick date & time",
                detail = when {
                    currentReminderAt == null -> "Choose any day"
                    currentReminderAt > nowMs -> "Set for ${reminderLabel(currentReminderAt, nowMs, zone)}"
                    else -> "Was set for ${reminderLabel(currentReminderAt, nowMs, zone)}"
                },
                onClick = { showDatePicker = true },
            )

            if (currentReminderAt != null) {
                ReminderOption(
                    icon = Icons.Default.Delete,
                    label = "Remove reminder",
                    detail = "",
                    isDestructive = true,
                    onClick = { onClear(); onDismiss() },
                )
            }
        }
    }

    if (showDatePicker) {
        // The date picker works in UTC midnights
        val todayUtcMs = today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = start.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMs
                override fun isSelectableYear(year: Int): Boolean = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDateMs = dateState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (showTimePicker && pickedDateMs != null) {
        val timeState = rememberTimePickerState(initialHour = start.hour, initialMinute = start.minute, is24Hour = false)
        // The date picker's value is the UTC midnight of the picked day
        val pickedDate = Instant.fromEpochMilliseconds(pickedDateMs!!).toLocalDateTime(TimeZone.UTC).date
        val epochMs = LocalDateTime(pickedDate, LocalTime(timeState.hour, timeState.minute))
            .toInstant(zone).toEpochMilliseconds()
        val isFuture = epochMs > Clock.System.now().toEpochMilliseconds()
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    enabled = isFuture,
                    onClick = {
                        // Checked again: the time can pass while the dialog is open
                        if (epochMs > Clock.System.now().toEpochMilliseconds()) {
                            showTimePicker = false
                            onSet(epochMs)
                            onDismiss()
                        }
                    },
                ) { Text("Set reminder") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Pick a time", fontWeight = FontWeight.Bold) },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timeState)
                if (!isFuture) {
                    Text(
                        "Pick a time in the future",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/** The previous reminder if it is still ahead, otherwise one hour from now. */
@OptIn(ExperimentalTime::class)
internal fun reminderPickerStart(currentReminderAt: Long?, nowMs: Long, zone: TimeZone): LocalDateTime {
    val ms = currentReminderAt?.takeIf { it > nowMs } ?: (nowMs + 3600_000L)
    return Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone)
}

@Composable
private fun ReminderOption(
    icon: ImageVector,
    label: String,
    detail: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            icon,
            contentDescription = null,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
