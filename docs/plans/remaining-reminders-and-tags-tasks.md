# Remaining tasks — Reminders & Category Tags

**Branch:** `claude/weekly-reminder-list-integration-4snu37`
**Depends on:** `docs/plans/reminders-and-category-tags.md` (completed work)
**Date:** 2026-11-27

---

## Task 1: Custom date/time picker in ReminderPickerSheet

**Goal:** Add a "Pick date & time" option below the existing presets so users can set
any specific reminder time.

**File:** `bookmarks/src/commonMain/kotlin/.../screens/details/ReminderPickerSheet.kt`

**Current state:** The sheet has 4 presets (Later today, Tomorrow, This weekend, Next week)
and a "Remove reminder" option. No custom picker.

**Implementation:**

1. Add a 5th option row: icon `Icons.Default.Alarm`, label "Pick date & time"
2. On tap, set a local state `showDatePicker = true`
3. Show Material 3 `DatePickerDialog` (from `androidx.compose.material3`)
   - Constrain to today or later: `selectableDates` that rejects past dates
   - On confirm, store the selected date and show `TimePickerDialog`
4. Show `TimePicker` (Material 3) after date is selected
   - On confirm, combine date + time → epoch ms via `LocalDateTime(date, time).atStartOfDayIn(zone)` math
   - Call `onSet(epochMs)` with the combined value
5. Both pickers share the bottom sheet — no need to dismiss and reopen

**Key imports:**
```kotlin
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
```

**Converting to epoch ms:**
```kotlin
val selectedDate = LocalDate(year, month, dayOfMonth) // from DatePicker
val selectedTime = LocalTime(hour, minute)             // from TimePicker
val zone = TimeZone.currentSystemDefault()
val epochMs = LocalDateTime(selectedDate, selectedTime)
    .let { selectedDate.atStartOfDayIn(zone).toEpochMilliseconds() + hour * 3600_000L + minute * 60_000L }
```

**Both screens benefit:** `AddLinkScreen` and `LinkDetailScreen` both use `ReminderPickerSheet`,
so this is a single change.

---

## Task 2: Separate Reminders tab (4th bottom nav tab)

**Goal:** Add a dedicated "Reminders" tab in the bottom navigation bar, alongside Feed,
Library, and Settings. Shows links with reminders grouped by urgency.

### 2a. Root navigation — add the tab

**Files to modify:**

| File | Change |
|---|---|
| `app/.../screens/root/RootScreens.kt` | Add `RemindersTab` to sealed class |
| `app/.../screens/root/RootScreen.kt` | Add tab to `APP_TABS` list (icon: `Icons.Filled.Notifications`, title: "Reminders"), add `when` branch, add serializer registration in `config` |
| `app/.../screens/root/RootNavigationState.kt` | Add `RemindersTab` to `tabs` list |

**In `RootScreens.kt`:**
```kotlin
@Serializable
data object RemindersTab : RootScreens()
```

**In `RootScreen.kt` — `APP_TABS`:**
```kotlin
val APP_TABS = listOf(
    TabDefinition(RootScreens.FeedTab, Icons.Filled.RssFeed, "Feed"),
    TabDefinition(RootScreens.LibraryTab, Icons.Filled.MenuBook, "Library"),
    TabDefinition(RootScreens.RemindersTab, Icons.Filled.Notifications, "Reminders"),
    TabDefinition(RootScreens.SettingsTab, Icons.Filled.Settings, "Settings")
)
```

**In `RootScreen.kt` — Scaffold content `when` block:**
```kotlin
RootScreens.RemindersTab -> RemindersTabNavigation()
```

**In `config` serializers module:**
```kotlin
subclass(RootScreens.RemindersTab::class, serializer<RootScreens.RemindersTab>())
```

### 2b. Reminders tab navigation

**New file:** `app/src/commonMain/kotlin/.../screens/reminders/navigation/RemindersTabNavigation.kt`

Follow the same pattern as `LibraryTabNavigation.kt`:
- Own `TabNavigator` + `TabNavigationState`
- `NavDisplay` with shared transitions (single-pane) or list-detail (two-pane)
- Routes: `RemindersBase` (the list), `LinkDetail(linkId)` (reuse existing detail screen)

**New file:** `app/src/commonMain/kotlin/.../screens/reminders/navigation/RemindersNavRoutes.kt`
```kotlin
@Serializable
sealed class RemindersNavRoutes : NavKey {
    @Serializable data object RemindersBase : RemindersNavRoutes()
    @Serializable data class LinkDetail(val linkId: String) : RemindersNavRoutes()
}
```

### 2c. Reminders screen — grouped list

**New files in `bookmarks/` module:**

| File | Purpose |
|---|---|
| `bookmarks/.../screens/reminders/RemindersScreen.kt` | UI: grouped list of reminder links |
| `bookmarks/.../screens/reminders/RemindersViewModel.kt` | `MoleculeViewModel` collecting links with reminders |

**Grouping logic:** Sort by `reminderAt` ascending, group into sections:
- **Overdue** — `reminderAt < now`
- **Today** — `reminderAt` is today (same date as now)
- **Tomorrow** — next day
- **This week** — within 7 days
- **Later** — everything else

**ViewModel collects:**
```kotlin
linkRepository.getLinks().collect { result ->
    val reminders = result.getOrElse { emptyList() }
        .filter { it.reminderAt != null }
        .sortedBy { it.reminderAt }
    // Group by section using reminderLabel() logic
}
```

**Each item shows:** link title, host, reminder time (via `reminderLabel()`), swipe to clear reminder.

**Use `AppLazyColumn`** (design system rule). Section headers use `SectionLabel` pattern.

### 2d. Optional: dedicated DAO query

For efficiency, add to `LinkDao.kt`:
```kotlin
@Query("SELECT * FROM links WHERE reminderAt IS NOT NULL ORDER BY reminderAt ASC")
fun getLinksWithReminders(): Flow<List<LinkEntity>>
```
And wire through `LinkRepository` → `RoomLinkRepository`. This avoids loading all links
just to filter. Not strictly required — the existing `getLinks()` works for small datasets.

### 2e. Library tab Reminders tile navigation

Currently `RemindersTile` in `LibraryTabScreen.kt` calls `onOpenScope(SavedScope.Reminders)`
which opens the smart list. Update this to navigate to the Reminders tab instead:
- Pass `onSwitchToRemindersTab` callback from `RootScreen`
- Or keep as a smart list that also works (both paths are valid)

---

## Task 3: Show tag names on category rows

**Goal:** Each category row in the Library tab should display its assigned tags as compact
chips below the category name.

**Files to modify:**

| File | Change |
|---|---|
| `bookmarks/.../screens/library/LibraryTabViewModel.kt` | Collect `tagRepository.getCategoryTagNames()` → store in `LibraryTabState` |
| `bookmarks/.../screens/library/LibraryTabScreen.kt` | Pass `categoryTagNames` to `CategoryTreeRow`, render tag chips |

**In `LibraryTabState`:**
```kotlin
data class LibraryTabState(
    // ... existing fields ...
    val categoryTagNames: Map<String, Set<String>> = emptyMap()
)
```

**In `LibraryTabViewModel` — add a LaunchedEffect:**
```kotlin
LaunchedEffect(Unit) {
    tagRepository.getCategoryTagNames().collect { result ->
        result.onSuccess { state = state.copy(categoryTagNames = it) }
    }
}
```

**In `CategoryTreeRow` — add tags display:**
```kotlin
@Composable
private fun CategoryTreeRow(
    category: Category,
    all: List<Category>,
    categoryTagNames: Map<String, Set<String>>,  // NEW param
    onOpen: (String) -> Unit,
    depth: Int = 0,
) {
    // ... existing Row ...
    // After the category name Text, add:
    val tags = categoryTagNames[category.id].orEmpty()
    if (tags.isNotEmpty()) {
        Row(
            modifier = Modifier.padding(start = (46 + depth * 20).dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.take(3).forEach { tagName ->
                Text(
                    "#$tagName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (tags.size > 3) {
                Text("+${tags.size - 3}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

**Update all `CategoryTreeRow` call sites** to pass `categoryTagNames = state.categoryTagNames`.

---

## Task 4: Category edit — add/remove tags

**Goal:** In the Manage screen's category editing, users can assign tags to categories.

**Files to modify:**

| File | Change |
|---|---|
| `bookmarks/.../screens/categoryPicker/CategoryPickerViewModel.kt` | Add `categoryTags` state, wire `tagRepository` methods |
| `bookmarks/.../screens/categoryPicker/CategoryPickerStateModels.kt` | Add `ToggleCategoryTag(categoryId, tagId)`, `AddCategoryTagByName(categoryId, name)` events, `categoryTagNames: Map<String, Set<String>>` state |
| `bookmarks/.../screens/categoryPicker/CategoryPickerContent.kt` | Show tag chips on each category row, tap to add/remove |
| `bookmarks/.../screens/manage/ManageScreen.kt` | May need to pass tag editing capability |

**UI approach:**
- Each category row in the picker/manage content gets a small "+" button or tap area
- Tapping opens a mini tag selector (reuse `TagInput` + `SelectedTagChip` from add link)
- Or: long-press a category row to show a tag editing sheet
- Selected tags show as compact chips below the category name (same as Task 3 display)

**Repository calls:**
```kotlin
// Assign
tagRepository.assignTagToCategory(categoryId, tagId)
// Remove
tagRepository.removeTagFromCategory(categoryId, tagId)
// Read (reactive)
tagRepository.getTagsForCategory(categoryId) // Flow<Result<List<Tag>>>
tagRepository.getCategoryTagNames()           // Flow<Result<Map<String, Set<String>>>>
```

All 4 methods are already implemented in `RoomTagRepository` and `FakeTagRepository`.

---

## Task 5: Sliding tab animation (Navigation 3.0 compatible)

See separate plan: `docs/plans/sliding-tab-animation.md`

---

## Suggested build order

1. **Task 1** (custom time picker) — smallest, self-contained, single file
2. **Task 3** (show tags on categories) — read-only display, no new events needed
3. **Task 4** (category tag editing) — depends on Task 3 for display
4. **Task 2** (Reminders tab) — largest, needs new navigation + screen + viewmodel
5. **Task 5** (sliding animation) — visual polish, independent of features

Tasks 1-4 can each be tested with a local build. Task 5 is best done last since it
touches root navigation.
