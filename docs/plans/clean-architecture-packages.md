# Clean architecture: packages and screen structure (final)

Scope: `bookmarks`, `settings`, `app`. Moves, renames and splits only, no behaviour change.
The base package `com.greenrobotdev.lordofthelinks` is not renamed. No new Gradle modules.

`B` = `bookmarks/src/*/kotlin/com/greenrobotdev/lordofthelinks/bookmarks`

## Rules

- Per screen: `XScreen` (UI), `XViewModel` (wiring), `XPresenter` (state logic), `XStateModels` (state, events).
- No use case layer. Business logic goes in a manager, created only when there is logic to move.
  No pass-through methods; presenters may call a repository for a simple read or write.
- `WebLinkHandler` holds pure URL rules. `LinkManager` holds rules that need repositories or providers.
- Outside sources sit behind a provider interface in `domain/provider`.
- UI never calls a repository, store or service directly.
- `domain` and `data` never import from `screens` or `components`.
- A composable used by more than one screen folder goes to `components`; otherwise it stays with its feature.
- Repository interfaces stay in `core/model`, Room implementations in `database`.

## Target layout (`B`)

```
domain/      WebLinkHandler, DeleteFlow, DateLabels
  category/  CategoryTree
  feed/      FeedFilters, FeedRules
  reminder/  ReminderTimes
  manager/   LinkManager, TagManager, BackupManager, ImportPlan
  provider/  LinkMetadataProvider
data/
  format/    LinksBackup, BookmarksHtml
  provider/  HtmlLinkMetadataProvider, HtmlTags
  store/     FeedFilterStore
platform/    ShareLink (+ actuals)
components/  link/, field/, picker/, reminder/
motion/      SharedThumb (unchanged)
screens/     add, categoryPicker, details, feed, library, manage, reminders, saved, tagPicker
```

## File map: `library` package (becomes `screens/feed` and `screens/library`)

Today `LibraryScreen` is the Feed tab and `LibraryTabScreen` is the Library tab. Do the Feed rename first,
so the `Library*` names are free.

### Feed

| Today | Declarations | To |
|---|---|---|
| `LibraryScreen` | `LibraryScreen` → `FeedScreen`, `TopSlot`, `CAUGHT_UP_SHOWN_MS` | `screens/feed/FeedScreen` |
| | `FirstLaunchEmptyState` | `screens/feed/components/FeedEmptyStates` |
| | `ModernLinkCard` | Delete, with its debug previews |
| `LibraryViewModel` | `LibraryViewModel` → `FeedViewModel` | `screens/feed/FeedViewModel` |
| | `LibraryPresenter` → `FeedPresenter` | `screens/feed/FeedPresenter` |
| | preview image backfill (inside the presenter) | `domain/manager/LinkManager` |
| `LibraryStateModels` | `LibraryState` → `FeedState`, `LibraryEvent` → `FeedEvent`, `PICK_UP_COUNT`, `celebrateCaughtUp` | `screens/feed/FeedStateModels` |
| | `filterFeed` (takes the screen state) | `screens/feed/FeedFiltering` |
| | `FeedFilters`, `OpenedFilter`, `LinkKind`, `SavedRange`, `Link.kind`, `SavedRange.startMs` | `domain/feed/FeedFilters` |
| | `resurfaceLabel` | `domain/feed/FeedRules` |
| `FeedFilterSheet` | whole file | `screens/feed/components/FeedFilterSheet` |
| `FeedFilterStore` | whole file | `data/store/FeedFilterStore` |
| `FeedComponents` (925 lines) | `CollapsingHeaderState`, `rememberCollapsingHeaderState`, `scaleAway`, `FeedHeader`, `InboxProgress` | `screens/feed/components/FeedHeader` |
| | `AllSavesHeader`, `FiltersButton`, `SelectedFilterChip` | `screens/feed/components/FeedFilterBar` |
| | `PickUpStrip`, `PickUpCard`, `MarkOpenedCheck` | `screens/feed/components/PickUpStrip` |
| | `ResurfaceCard`, `CardAction`, `AllCaughtUpCard`, `NothingNewCard` | `screens/feed/components/FeedStatusCards` |
| | `FeedLinkCard`, `TypeLook`, `typeLook`, `TypeThumb`, `ArchivedPill`, `MetaChip` | `components/link/FeedLinkCard` |
| | `SwipeableFeedCard` | `components/link/SwipeableFeedCard` |
| | `FeedSectionHeader` | `components/link/FeedSectionHeader` |
| | `feedSection` | `domain/feed/FeedRules` |
| | `savedAtLabel`, `reminderLabel` | `domain/DateLabels` |

`typeLook` becomes `internal`, because `PickUpCard` uses it too. `FeedFilters` is saved as JSON; keep its
property and enum names so saved filters still load.

### Library tab

| Today | Declarations | To |
|---|---|---|
| `LibraryTabScreen` | `LibraryTabScreen` → `LibraryScreen` | `screens/library/LibraryScreen` |
| | `SectionLabel`, `TagTile`, `SmartTile`, `RemindersTile` | `screens/library/components/LibraryTiles` |
| | `CategoryTreeRow` | `screens/library/components/CategoryTreeRow` |
| | `LibraryEmptyHint` | `screens/library/components/LibraryEmptyHint` |
| `LibraryTabViewModel` | `LibraryTabViewModel` → `LibraryViewModel` | `screens/library/LibraryViewModel` |
| | state logic | new `screens/library/LibraryPresenter` |
| | `LibraryTabState` → `LibraryState`, `LibraryTabEvent` → `LibraryEvent` | `screens/library/LibraryStateModels` |

In `app`: `screens/library/navigation` (Feed tab) → `screens/feed/navigation`, and
`screens/organize/navigation` (Library tab) → `screens/library/navigation`, renaming their classes to match.

## File map: other screens

| Folder | Change |
|---|---|
| `add` | `AddLinkUseCase` → `AddLinkPresenter`; save, duplicate and metadata logic → `LinkManager` |
| `add/components` | `CategoryField`, `ChoiceChip`, `TagInput`, `BrowseAllButton` → `components/field/`; the rest stay |
| `add/sheet` | New `AddLinkSheetPresenter`; `AddLinkSheetModels` → `AddLinkSheetStateModels`; `ImeNestedScroll` stays |
| `add/TagCategorySelectorComponents` | Delete (unused) |
| `details` | `LinkDetailUseCase` → `LinkDetailPresenter`; `ReminderPickerSheet` → `components/reminder/`; `reminderPickerStart` → `domain/reminder/`; `ShareLink` → `platform/` |
| `reminders` | `RemindersUseCase` → `RemindersPresenter`; `reminderSection` → `domain/reminder/` |
| `saved` | New `SavedListStateModels` (state, events, `SavedScope`) and `SavedListPresenter` |
| `categoryPicker` | See below |
| `tagPicker`, `manage` | Import updates only |

### `categoryPicker`

`CategoryPickerContent` is one composable of about 340 lines. It only arranges the parts after the split.

| Code | To |
|---|---|
| Search field, error card, recent chips (`RecentChip`) | `categoryPicker/components/CategorySearchHeader` |
| Tree, search results, inline create and rename, `PickerItem`, `buildPickerItems` | `categoryPicker/components/CategoryList` |
| Delete panel and `ParentChoiceCard` | `categoryPicker/components/CategoryDeletePanel` |
| Mode-dependent bottom bars | `categoryPicker/components/CategoryActionBar` |
| `SelectableRow`, `InlineTreeField`, `CreateRow`, `ImpactCard` | `components/picker/` |
| `DeletePhase`, `DELETE_UNDO_MS` | `domain/DeleteFlow` |
| `CategoryTree` | `domain/category/` |
| `CategoryPickerScreen` | Delete (unused) |

`Sheet`, `Content`, `ViewModel`, `Presenter` and `StateModels` stay at the folder root.

## File map: non-screen code

| From | To |
|---|---|
| `B/domain/LinkType`, `LinkValidation`, `SameLink` | `B/domain/WebLinkHandler` (one object) |
| `B/service/MetadataFetchService` | `B/data/provider/HtmlLinkMetadataProvider` |
| its metadata result and `HttpStatusException` | `B/domain/provider/`, with the new `LinkMetadataProvider` interface |
| `B/service/HtmlTags` | `B/data/provider/` |
| `B/backup/LinksBackupService`, `ExportedFile` | `B/domain/manager/BackupManager` |
| `B/backup/LinksBackup` (model, read, write, `BackupReadException`) | `B/data/format/` |
| `planImport` | `B/domain/manager/ImportPlan` |
| `B/export/BookmarksHtml` | `B/data/format/` |
| `B/components/LinkPreviewImage` | `B/components/link/` |
| `settings/.../TextFileOpener`, `TextFileSaver`, `AppVersion` (+ actuals) | `.../screens/settings/platform/` |
| `settings/.../SettingsUseCase` | `SettingsPresenter` |
| `toEntries` in `app/.../root/TabNavigationState` | Delete (unused) |

`TagManager` gets "reuse or create a tag by name". `CategoryManager` is created only if category rules
exist outside the repository. Register managers, providers and stores in a Koin `bookmarksModule`.
Inject `WelcomeTourPreference` instead of creating it in UI.

## Tests that move with their code

| Test | To |
|---|---|
| `FeedFilterStoreTest` | `data/store/` |
| `FeedSectionTest`, `ResurfaceTest` | `domain/feed/` |
| `CaughtUpTest` | `screens/feed/` |
| `IsValidLinkTest`, `SameLinkTest` | `domain/` |
| `CategoryTreeTest` | `domain/category/` |
| `ReminderPickerStartTest` | `domain/reminder/` |
| `LinksBackupTest`, `BookmarksHtmlTest` | `data/format/` (import planning tests → `domain/manager/`) |
| `ViewModelWiringTest` | Stays; update names |

## Order (one commit per step; build and tests pass after each)

| # | Step |
|---|---|
| 0 | `git revert e5cf04c` (tag draft, GRE-97) so tests pass; restore it when the design is ready |
| 1 | Delete unused code |
| 2 | Rename `*UseCase` files to `*Presenter` |
| 3 | Move `domain`, `data`, `platform`; add `WebLinkHandler` and the provider interface |
| 4 | Move shared components |
| 5 | Split `library` into `feed` and `library`, including the `app` navigation packages |
| 6 | Split `saved`; add the `AddLinkSheet` presenter |
| 7 | Split `CategoryPickerContent` |
| 8 | Add managers, each with a test; Koin module; welcome tour injection |
| 9 | Record the rules in `docs/architecture/AGP9_AND_PATTERNS.md` |

Check after each step:
`./gradlew :bookmarks:jvmTest :settings:jvmTest :app:jvmTest :app:androidApp:compileDevDebugKotlin`,
plus the wasm compile after steps that move `expect`/`actual` files.
Check on a device after steps 5 and 7: Feed, Library tab, category picker (sheet and Manage).

## Later, only if needed

- `:core:data` and `:core:domain` modules. `BackupManager` uses `data/format`, so settle that dependency first.
- Enable the commented-out `:konsistTest` module to enforce the rules.
