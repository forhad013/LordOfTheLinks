# Screenshots in Android v1 — integration plan

Source: `plan/LordOfLinks - screen shot Implementation Plan.md` (Nov 23, 2026).
Goal: save screenshots and find them by the text in them. Android only, offline, no backend.

**Hard constraint (owner, 2026-11-23): do not alter the existing database or structure.**
So: `LinkDatabase` stays at v6 with no new tables or migrations; `Link`, `SharedContent`, `LinkRepository`, the Feed, the Library and backup stay as they are. Screenshots get their **own module and their own Room database**. Existing files only get additions (a tab, a manifest filter, a DI line).

## What the source plan asks for vs. what we do

| Source plan item | Decision |
| --- | --- |
| Voyager/Decompose, SQLDelight | Ignore, we use Navigation 3 + Room |
| `Card` sealed class replacing `Link` | **No.** Separate `Screenshot` model, `Link` untouched |
| Link features (share, metadata, tags, favorites, import) | Already built, nothing to do |
| Shared `tags` table across links + screenshots | **Shared at the repository level, not the table.** Screenshots use the existing `TagRepository` / `CategoryRepository` (from `core/model`) and store `tagIds` + `categoryId` on their own row. See "Tags and categories" |
| One mixed feed + unified search | **No for v1.** Screenshots get their own tab with its own search |
| Screenshot model, storage, OCR | **v1** |
| Share an image to the app / pick from gallery | **v1** |
| Thumbnail files | Skip, Coil downsamples on load |
| `ContentObserver` auto-detect | v1: share from the screenshot preview only. v1.1: opt-in auto-save. See "Detecting new screenshots" |
| ML Kit Image Labeling | Defer (little value on screenshots) |
| Gemini Nano / cloud fallback | Defer to Phase 3. Never ship a Gemini key in `BuildConfig` |

## New module: `:screenshots`

KMP module like `bookmarks`/`settings` (`com.android.kotlin.multiplatform.library`), package `com.greenrobotdev.lordofthelinks.screenshots`. Depends on `:core:design`, `:core:utils`, Room, Koin, Coil. Also depends on `:core:model` for `TagRepository` / `CategoryRepository` (interfaces only, never on `:database`). Nothing existing depends on it except the app shell.

```
screenshots/src/commonMain/.../screenshots/
  model/Screenshot.kt            id, imagePath, extractedText, ocrState, isFavorite, createdAt, tagIds: List<String>, categoryId: String?
  data/ScreenshotEntity.kt       table "screenshots"
  data/ScreenshotDao.kt          observeAll, search(q), insert, update, delete
  data/ScreenshotDatabase.kt     separate file "screenshots.db", version 1
  data/ScreenshotRepository.kt   Flow<Result<T>>, same style as RoomLinkRepository
  import/ScreenshotImporter.kt   interface; no-op on desktop
  screens/list/                  grid + search field
  screens/detail/                image, text, tags, share, delete
  di/ScreenshotsModule.kt
screenshots/src/androidMain/...
  AndroidScreenshotImporter.kt   copy URI → filesDir/screenshots/<id>.jpg, run ML Kit OCR
```

Why a second database instead of a table in `LinkDatabase`: no migration touches users' existing links, and if screenshots go wrong we can drop the file without risk to links. Cost: no cross-search with links and no shared tags until we decide to merge (later, by choice).

## Tags and categories (shared, no database change)

- Don't split tags/categories into their own database: `link_tags` has foreign keys (with cascade delete) to `tags` and `links` inside `LinkDatabase`, and SQLite can't enforce foreign keys across database files. Moving them would also need a migration on existing data.
- Instead `:screenshots` calls the existing `TagRepository` (`getTags`, `addTag`) and `CategoryRepository` (`getCategories`, `addCategory`) through Koin, and stores ids on its own row. Same names and pickers for links and screenshots, zero change to `LinkDatabase`.
- Known gaps:
  - Deleting a tag/category doesn't cascade into `screenshots.db`. Fix: when loading screenshots, drop ids that no longer exist in `getTags()` / `getCategories()`.
  - `getTagsWithCounts()` / category counts only count links. Fine for v1; add a separate screenshot count later if wanted.

## Detecting new screenshots

Android has no "screenshot taken" broadcast for other apps (`registerScreenCaptureCallback` on Android 14+ only covers your own app). A screenshot is just a new image in MediaStore under `Screenshots/`.

**v1 — share only (no permission).** The user taps Share on the system screenshot preview → Lords of Links (step 3). Nothing for Play to review.

**v1.1 — opt-in "Auto-save screenshots" toggle in Settings (off by default).**
1. Ask for `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (≤ 32) only when the user turns the toggle on.
2. WorkManager job with `addContentUriTrigger(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true)`. It runs after new images appear, even with the app closed. The trigger fires once, so the worker re-enqueues itself (`enqueueUniqueWork(..., REPLACE)`).
3. Worker queries `DATE_ADDED > lastSeenAt AND RELATIVE_PATH LIKE '%Screenshots/%'`, imports each through `ScreenshotImporter`, then saves `lastSeenAt`.
4. Run the same query on every app open as catch-up (triggers can be dropped after reboot or when the system kills the job).
5. Show a "Saved screenshot · Undo" notification: auto-save will catch passwords, bank screens, private chats.

Catches:
- **Play policy:** `READ_MEDIA_IMAGES` is restricted. Broad photo access must be the app's core purpose and needs a declaration form, or Play expects the photo picker. Real rejection risk, so keep it out of the first v1 release.
- **Android 14 partial access** ("Allow limited access"): new screenshots are invisible. Detect it and prompt for full access.
- Trigger is batched: seconds to about a minute of delay, not instant.
- Not done: confirm current Play photo-permission policy wording; test trigger timing on device.

## Steps (one PR each)

### 1. Module + database
- Create `:screenshots`, add `include(":screenshots")` to `settings.gradle.kts`.
- `ScreenshotDatabase` v1 with `screenshots` table; `tagIds` as a JSON list column via its own converter, `categoryId` nullable text.
- Builder in androidMain using `context.getDatabasePath("screenshots.db")`.
- Tests: DAO insert/observe/search/delete.

### 2. Storage + OCR
- `com.google.mlkit:text-recognition` (bundled model, ~4 MB) in `libs.versions.toml`, androidMain only.
- Importer: copy image (cap long edge ~2048 px), insert row `PENDING`, OCR, update to `DONE`/`FAILED`. Re-run `PENDING` rows on app start.
- Delete row → delete file.
- Test: importer with fake OCR engine (state transitions).

### 3. Getting images in (additions only)
- `AndroidManifest.xml`: **add** `SEND` / `SEND_MULTIPLE` filters for `image/*`. Existing text filters untouched.
- `MainActivity`: **add** a branch for `image/*` intents that reads `EXTRA_STREAM` and calls the importer directly. `SharedContent` and the link share flow are not changed.
- Screenshots tab: "Add from gallery" via `PickMultipleVisualMedia` (no permission needed).

### 4. UI
- **Add** `ScreenshotsTab` to `RootScreens` and the bottom bar (this is the one place the shell changes, and it's an addition).
- Grid of `ContentCard` tiles (thumbnail, first OCR line, tags), `AppLazyVerticalGrid`, `AppIcon`. No raw `Card`.
- Detail: zoomable image, selectable extracted text + copy, tag and category chips (pick from / create in the shared `TagRepository` / `CategoryRepository`), share, delete. `PENDING` → "Reading text…"; `FAILED` → "Retry".
- Light + dark check, motion per `compose-motion` skill.

### 5. Search
- Search field on the Screenshots tab: `extractedText LIKE '%q%'`, plus screenshots whose `tagIds` contain a tag whose name matches `q` (resolve names through `TagRepository`). FTS later if needed.

### 6. Release gate
- Backup in Settings is **not** changed: say in the Screenshots tab (or release notes) that screenshots aren't in the backup yet.
- Play Data Safety: images stay on device, OCR on-device, nothing uploaded.
- Owner device test (`devDebug`): share from Photos, gallery pick, long screenshot OCR, search, delete, and upgrade over an existing install to confirm links are untouched.

## Deferred (v1.1+)
- Screenshots in backup (separate file or zip with images).
- Merge into the Feed / cross-search with links (needs a decision on shared tags).
- Auto-save new screenshots (see "Detecting new screenshots"), image labeling, "save URLs found in this screenshot as links".
- Gemini Nano tags/summaries (Phase 3).

## Rough size
Steps 1–3: ~3 days. Step 4: ~3–4 days. Steps 5–6: ~1–2 days. Fits one 2-week cycle.
