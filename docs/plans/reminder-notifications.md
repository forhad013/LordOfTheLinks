# Reminder notifications (Android)

## Problem
A reminder time is saved and shown in the app, but nothing alerts the user when it arrives.

## Approach
WorkManager: one unique one-time work per link (`reminder-<linkId>`), delayed until `reminderAt`.
It survives restarts and needs no exact-alarm permission. A notification can be some minutes late.
Use `AlarmManager` instead only if exact-minute delivery is required.

## Steps
1. Add `androidx.work:work-runtime-ktx` (version catalog, Android source set only).
2. Keep scheduling in sync from one place: in the Android app, collect `LinkRepository.getLinks()`
   and enqueue (`REPLACE`) for future `reminderAt`, cancel the rest. This covers set, change,
   clear, delete, undo and restore without touching each caller. Only reschedule links whose
   time changed; cancel by a shared tag for links that disappeared.
3. Worker: reload the link; do nothing if it is gone or `reminderAt` changed. Otherwise post.
4. Notification: one channel "Reminders"; title = link title, text = host; tap opens Link Detail.
5. Ask for `POST_NOTIFICATIONS` (API 33+) when the user first sets a reminder. If denied, still
   save the reminder and say notifications are off.
6. Decide what a fired reminder does in the list (recommended: stays under Overdue until cleared).
7. Backup: `reminderAt` is not in the backup format, so restore loses reminders. Add it as an
   optional field so old backups still import.

## Tests
- Unit: which links to schedule or cancel for a given previous and new list.
- Unit: backup round trip keeps `reminderAt`; an old backup without it imports.
- Device: fire with app closed, after reboot, permission denied, change and delete before firing.

## Out of scope
Desktop and web notifications, repeating reminders, snooze.
