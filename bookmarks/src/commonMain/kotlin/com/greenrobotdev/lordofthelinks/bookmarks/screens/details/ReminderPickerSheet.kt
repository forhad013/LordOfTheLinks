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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.greenrobotdev.lordofthelinks.design.foundation.icon.AppIcon
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.datetime.DatePeriod
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
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
    val currentHour = remember { nowDt.hour }

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
                val evening = LocalDateTime(today, LocalTime(19, 0))
                    .let { today.atStartOfDayIn(zone).toEpochMilliseconds() + 19 * 3600 * 1000L }
                ReminderOption(
                    icon = Icons.Default.Nightlight,
                    label = "Later today",
                    detail = "7:00 PM",
                    onClick = { onSet(evening); onDismiss() },
                )
            }

            val tomorrowMs = today.plus(DatePeriod(days = 1))
                .atStartOfDayIn(zone).toEpochMilliseconds() + 9 * 3600 * 1000L
            ReminderOption(
                icon = Icons.Default.Today,
                label = "Tomorrow",
                detail = "9:00 AM",
                onClick = { onSet(tomorrowMs); onDismiss() },
            )

            val daysUntilSaturday = (6 - today.dayOfWeek.isoDayNumber).let { if (it <= 0) 7 + it else it }
            val weekendMs = today.plus(DatePeriod(days = daysUntilSaturday))
                .atStartOfDayIn(zone).toEpochMilliseconds() + 10 * 3600 * 1000L
            ReminderOption(
                icon = Icons.Default.Weekend,
                label = "This weekend",
                detail = "Saturday, 10:00 AM",
                onClick = { onSet(weekendMs); onDismiss() },
            )

            val nextWeekMs = today.plus(DatePeriod(days = 8 - today.dayOfWeek.isoDayNumber))
                .atStartOfDayIn(zone).toEpochMilliseconds() + 9 * 3600 * 1000L
            ReminderOption(
                icon = Icons.Default.CalendarMonth,
                label = "Next week",
                detail = "Monday, 9:00 AM",
                onClick = { onSet(nextWeekMs); onDismiss() },
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
