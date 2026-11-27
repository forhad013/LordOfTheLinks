# Reminders & Category Tags — completed work and remaining tasks

**Branch:** `claude/weekly-reminder-list-integration-4snu37`
**Date:** 2026-11-27

---

## What was built (already committed and pushed)

### 1. Link reminder system — core + detail screen + add screen

Users can set a time-based reminder on any link. Preset options: Later today (7 PM),
Tomorrow (9 AM), This weekend (Sat 10 AM), Next week (Mon 9 AM).

#### Database

| File | Change |
|---|---|
| `database/.../room/LinkEntity.kt` | Added `val reminderAt: Long? = null` column |
| `database/.../room/LinkDao.kt` | Added `setReminder(linkId, reminderAt)` and `clearReminder(linkId)` queries |
| `database/.../room/CategoryDao.kt` | `SmartListCounts` query now includes `reminders` count (`COUNT` of rows where `reminderAt IS NOT NULL`) |
| `database/.../room/Migrations.kt` | `MIGRATION_6_7`: adds `reminderAt` column to `links`, creates `category_tags` junction table with indexes |
| `database/.../room/LinkDatabase.kt` | Version bumped to 7, added `CategoryTagEntity` to entities, `categoryTagDao()` accessor |
| `database/.../repository/EntityMappers.kt` | Maps `reminderAt` between entity and model, maps `SmartListCounts.reminders` |
| `database/.../repository/RoomLinkRepository.kt` | Implements `setReminder()` / `clearReminder()` |

#### Core model

| File | Change |
|---|---|
| `core/model/.../model/Link.kt` | Added `reminderAt: Long? = null` |
| `core/model/.../model/SmartListCounts.kt` | Added `reminders: Int = 0` |
| `core/model/.../model/repository/LinkRepository.kt` | Added `setReminder(linkId, reminderAt)` and `clearReminder(linkId)` |

#### Detail screen (set/clear reminder on existing links)

| File | Change |
|---|---|
| `bookmarks/.../screens/details/ReminderPickerSheet.kt` | **NEW** — `ModalBottomSheet` with preset reminder options |
| `bookmarks/.../screens/details/LinkDetailScreen.kt` | Bell icon in action bar, reminder display row (alarm icon + label), opens `ReminderPickerSheet` |
| `bookmarks/.../screens/details/LinkDetailStateModels.kt` | Added `SetReminder(reminderAt)` and `ClearReminder` events |
| `bookmarks/.../screens/details/LinkDetailUseCase.kt` | Handles `SetReminder` / `ClearReminder` → calls `linkRepository` |

#### Add Link screen (set reminder while saving a new link)

| File | Change |
|---|---|
| `bookmarks/.../screens/add/AddLinkStateModels.kt` | Added `reminderAt: Long? = null` to `AddLinkState`, `SetReminder` / `ClearReminder` events |
| `bookmarks/.../screens/add/AddLinkUseCase.kt` | Event handlers, `reminderAt` in `linkFromState()`, `setReminder()` call during Submit |
| `bookmarks/.../screens/add/AddLinkScreen.kt` | Reminder row (notification icon + "Add reminder" label) between Note and Save, opens `ReminderPickerSheet` |

#### Library tab (Reminders tile)

| File | Change |
|---|---|
| `bookmarks/.../screens/library/LibraryTabScreen.kt` | `RemindersTile` composable — full-width row with bell icon, count badge, below the 2x2 smart tiles |
| `bookmarks/.../screens/library/LibraryTabViewModel.kt` | Added `remindersCount` to `LibraryTabState`, wired from `SmartListCounts.reminders` |

#### Smart list filter

| File | Change |
|---|---|
| `bookmarks/.../screens/saved/SavedScope.kt` | `SavedScope.Reminders` — filters `link.reminderAt != null` |
| `bookmarks/.../screens/saved/SavedListScreen.kt` | `SavedScope.Reminders` branch in `look()` — tertiary color + notification icon |
| `bookmarks/.../screens/saved/SavedListViewModel.kt` | Wired to filter by reminders |

#### Shared utilities

| File | Change |
|---|---|
| `bookmarks/.../screens/library/FeedComponents.kt` | `reminderLabel(epochMs)` — formats reminder time: "Overdue", "Today, 3:00 PM", "Tomorrow, 9:00 AM", "Wednesday, 9:00 AM", "15 Dec, 9:00 AM" |

#### Test & fakes

| File | Change |
|---|---|
| `core/testing/.../testing/FakeRepositories.kt` | `FakeLinkRepository`: `setReminder` / `clearReminder`. `FakeTagRepository`: `categoryTags` state flow + all 4 category-tag methods |
| `bookmarks/.../screens/saved/SavedScopeTest.kt` | `remindersShowsLinksWithReminderSet()` and `tagInheritedFromCategory()` tests |

### 2. Category tag system — database + repository + filter logic

Categories and sub-categories can have tags assigned independently from links. When
searching by tag, links inherit tags from their category (if a category is tagged
`#learning`, all links in it match a `#learning` search).

#### Database

| File | Change |
|---|---|
| `database/.../room/CategoryTagEntity.kt` | **NEW** — `@Entity(tableName = "category_tags")` junction table (categoryId + tagId composite PK) |
| `database/.../room/CategoryTagDao.kt` | **NEW** — `insert`, `delete`, `getTagsForCategory`, `getCategoryTagNames` queries |
| `database/.../room/LinkDatabase.kt` | Added `CategoryTagEntity` to entities, `categoryTagDao()` |
| `database/.../repository/RoomTagRepository.kt` | Added `CategoryTagDao` parameter, implements `assignTagToCategory`, `removeTagFromCategory`, `getTagsForCategory`, `getCategoryTagNames` |
| `database/.../di/DataModule.kt` | Updated `RoomTagRepository` construction to include `CategoryTagDao` |

#### Core model

| File | Change |
|---|---|
| `core/model/.../model/repository/TagRepository.kt` | Added `assignTagToCategory`, `removeTagFromCategory`, `getTagsForCategory`, `getCategoryTagNames` |

#### Filter logic (tag inheritance)

| File | Change |
|---|---|
| `bookmarks/.../screens/saved/SavedScope.kt` | `savedInScope()` accepts `categoryTagNames` map; `WithTag` scope checks both link tags and category tags |
| `bookmarks/.../screens/library/LibraryViewModel.kt` | Collects `getCategoryTagNames()` flow and passes to `filterFeed()` |
| `bookmarks/.../screens/library/LibraryStateModels.kt` | `categoryTagNames` field, tag inheritance in `filterFeed()` |

---

## Critical patterns for new code

### Clock / Instant imports

This project uses `kotlin.time.Clock` and `kotlin.time.Instant` (NOT `kotlinx.datetime`
versions). `kotlinx.datetime` is used only for `TimeZone`, `LocalDateTime`, `DatePeriod`, etc.

```kotlin
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant  // NOT kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
val nowMs = Clock.System.now().toEpochMilliseconds()
val dt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
```

### Design system rules (CLAUDE.md)

- Cards: use templates from `:core:design` (`FormCard`, `ContentCard`, etc.)
- Icons: always `AppIcon` — no custom icon backgrounds
- Scrolling: `AppLazyColumn` / `appVerticalScroll` from `:core:design`
- Colors: always `MaterialTheme.colorScheme` — never hardcode
- Inputs: `AppTextFieldEnhanced`, `AppTextFieldWithCharacterLimit`

### Repository pattern

Repositories return `Flow<Result<T>>`. Reactive collection with `.collect {}`, not `.first()`.

### ViewModel pattern

`MoleculeViewModel` with `@Composable override fun models(events: Flow<Event>): State`.
Events are sealed interfaces. State is a data class.
