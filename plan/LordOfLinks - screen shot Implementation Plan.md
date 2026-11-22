# LordOfLinks - Implementation Plan

Nov 23, 2026 - @forhad

## App Overview

LordOfLinks is a personal knowledge capture app that saves links and screenshots in one searchable library. Every screenshot is auto-tagged using on-device OCR, and an on-device AI suggests labels so nothing gets lost.

**Core value proposition:** Save it once, find it by what it says — not where you put it.

**Two content types, one search:**

- **Links** — URLs saved from the share sheet or pasted in-app, with title, favicon, tags and optional notes
- **Screenshots** — images captured or imported, with text extracted by ML Kit OCR and auto-generated tags

**What this app is not:** This is the capture-and-find half of your toolset. Routine planning, meal planning and calendar integration live in the separate companion app.

## Architecture and Tech Stack

| Layer | Technology | Why |
| --- | --- | --- |
| UI | Compose Multiplatform (KMP) | Shared UI across Android/iOS/Desktop, already in use |
| Navigation | Voyager or Decompose | KMP-compatible navigation with back-stack |
| Local DB | Room (Android) / SQLDelight (shared) | Structured storage for cards + extracted text |
| Image storage | Local filesystem | Screenshots stored on-device; only metadata synced |
| OCR | ML Kit Text Recognition | Works offline on all Android devices, no API cost |
| Image labelling | ML Kit Image Labeling | Auto-generates tags from screenshot content |
| On-device AI | Gemini Nano via AICore | Smart tag suggestions, runs locally on Pixel 8+ / Samsung S24+ |
| AI fallback | Gemini API (cloud) | For devices without on-device AI support |
| Networking | Ktor | KMP-compatible HTTP client for cloud AI calls |
| DI | Koin | Lightweight, KMP-friendly dependency injection |

**Storage strategy:** Images stay on-device (cloud upload is expensive and unnecessary). Only extracted text and tags go into the local database, keeping it small and fast. A future optional cloud sync would transfer metadata only — never raw images.

## Data Model

A unified `Card` sealed class so links and screenshots share one feed, one search, and one tag system.

```kotlin
sealed class Card {
    abstract val id: String
    abstract val tags: List<String>
    abstract val createdAt: Long
    abstract val isFavorite: Boolean
    abstract val projectTag: String?   // optional grouping
}

data class LinkCard(
    override val id: String,
    val url: String,
    val title: String,
    val favicon: String?,
    val description: String?,
    val notes: String?,
    override val tags: List<String>,
    override val createdAt: Long,
    override val isFavorite: Boolean = false,
    override val projectTag: String? = null
) : Card()

data class ScreenshotCard(
    override val id: String,
    val imagePath: String,           // local file path
    val thumbnailPath: String,       // compressed preview
    val extractedText: String,       // ML Kit OCR output
    val aiDescription: String?,      // Gemini Nano summary
    override val tags: List<String>, // auto + manual tags
    override val createdAt: Long,
    override val isFavorite: Boolean = false,
    override val projectTag: String? = null
) : Card()
```

**Database tables (Room):**

| Table | Key columns | Notes |
| --- | --- | --- |
| `links` | id, url, title, favicon, description, notes, tags (JSON), createdAt, isFavorite, projectTag | Full-text search on title + description + notes |
| `screenshots` | id, imagePath, thumbnailPath, extractedText, aiDescription, tags (JSON), createdAt, isFavorite, projectTag | Full-text search on extractedText + aiDescription |
| `tags` | name, color, usageCount | Shared tag registry across both types |

Tags are stored as a JSON array in each card row and also tracked in a `tags` table for autocomplete and usage stats.

## Link Management Features

**Saving links:**

1. **Share sheet integration** — user shares a URL from any app; LordOfLinks receives it via Android intent filter, auto-fetches the page title and favicon using Ktor, and saves the `LinkCard`
2. **In-app paste** — paste or type a URL manually; same metadata fetch
3. **Bulk import** — import bookmarks from a browser export file (HTML bookmark format)

**Metadata extraction (runs on save):**

1. Fetch the URL with Ktor
2. Parse `<title>`, `<meta name="description">`, and favicon from the HTML `<head>`
3. Store title, description, and favicon URL (or download the favicon as a local file)
4. If Gemini Nano is available, generate 2–3 suggested tags from the title + description

**Organizing:**

- **Tags** — add/remove tags manually; AI-suggested tags shown as chips the user can accept or dismiss
- **Projects** — optional `projectTag` groups cards across both links and screenshots (e.g. "thesis", "job-search")
- **Favorites** — star any card for quick access
- **Sort and filter** — by date, by tag, by project, favorites only

**Actions on a link:**

- Open in browser
- Copy URL
- Edit tags / notes / project
- Share to other apps
- Delete

## Screen Capture and OCR Tagging

**Capture flow:**

1. User takes a screenshot (system screenshot or in-app camera capture)
2. App detects new screenshots via a `ContentObserver` on `MediaStore.Images` (or the user imports manually from gallery)
3. App copies the image to its own local storage directory and generates a compressed thumbnail
4. OCR and tagging pipeline runs automatically

**OCR pipeline (ML Kit Text Recognition):**

```kotlin
// Works offline on all Android devices, no API key needed
val recognizer = TextRecognition.getClient(
    TextRecognizerOptions.DEFAULT_OPTIONS
)

fun extractText(imagePath: String): Flow<String> = flow {
    val bitmap = BitmapFactory.decodeFile(imagePath)
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    val result = recognizer.process(inputImage).await()
    emit(result.text)
}
```

**Auto-tagging pipeline (runs after OCR):**

1. **ML Kit Image Labeling** — identifies objects/scenes in the image (e.g. "receipt", "code", "map", "text document") and adds them as tags
2. **Keyword extraction** — pull key terms from the OCR text (URLs, email addresses, phone numbers, dates) and suggest as tags
3. **Gemini Nano** (if available) — generate a one-line `aiDescription` summarizing the screenshot content and suggest 2–3 semantic tags

**Storage breakdown:**

| What | Where | Size impact |
| --- | --- | --- |
| Original image | App-local filesystem | \~1–5 MB per screenshot |
| Thumbnail | App-local filesystem | \~50–100 KB |
| Extracted text | Room DB `extractedText` column | \~1–10 KB (plain text) |
| Tags | Room DB `tags` JSON column | Negligible |
| AI description | Room DB `aiDescription` column | \~100–500 bytes |

Total DB overhead per screenshot is under 15 KB. The heavy storage is the image files themselves, which stay local and are never uploaded.

## On-Device AI Integration

Gemini Nano runs locally through Android's AICore service. It handles smart tagging and content summarization without network calls or API costs.

**Supported devices:** Pixel 8 and newer, Samsung Galaxy S24 and newer (devices with AICore + Gemini Nano downloaded).

**Capability check and fallback:**

```kotlin
suspend fun getAiProvider(context: Context): AiProvider {
    return try {
        val model = GenerativeModel(
            modelName = "gemini-nano",
            context = context
        )
        // Test with a no-op to verify availability
        AiProvider.OnDevice(model)
    } catch (e: Exception) {
        // Fall back to cloud Gemini API
        AiProvider.Cloud(geminiApiKey = BuildConfig.GEMINI_API_KEY)
    }
}

sealed class AiProvider {
    data class OnDevice(val model: GenerativeModel) : AiProvider()
    data class Cloud(val geminiApiKey: String) : AiProvider()
}
```

**What the AI does in this app:**

| Task | Input | Output | When |
| --- | --- | --- | --- |
| Tag suggestions for links | Page title + description | 2–3 tag strings | On link save |
| Tag suggestions for screenshots | OCR text + image labels | 2–3 tag strings | After OCR pipeline |
| Screenshot summary | OCR text (first 500 chars) | One-line `aiDescription` | After OCR pipeline |
| Smart search | User query + card metadata | Ranked card IDs | On search (stretch goal) |

**Prompt template (tag suggestion):**

```
Given this content, suggest 2-3 short tags (1-2 words each) for categorization.
Content: {title}. {description or extractedText}
Tags:
```

**Cost model:** On-device = free, unlimited. Cloud fallback = Gemini API free tier covers \~15 requests/minute, more than enough for personal use. No user data leaves the device unless the cloud fallback is triggered.

## UI and Navigation

**Three-tab bottom navigation:**

- **Links** — list of saved `LinkCard`s, sorted by date (newest first). Each row shows favicon, title, domain, tags, and a favorite star. Tap to open in browser; long-press for edit/delete menu.
- **Captures** — grid of saved `ScreenshotCard`s showing thumbnails. Each cell shows the thumbnail, top 2 tags, and the first line of extracted text. Tap to view full image + all extracted text + tags.
- **Search** — unified full-text search across both links and screenshots. Results show a mixed feed of `LinkCard` and `ScreenshotCard` items ranked by relevance. Search hits extracted OCR text, link titles, descriptions, notes, AI descriptions, and tags.

**Key screens:**

| Screen | What it shows |
| --- | --- |
| Link detail | Full metadata, notes editor, tag chips (editable), open/copy/share actions |
| Screenshot detail | Full-size image (pinch-to-zoom), extracted text panel, AI description, tag chips (editable) |
| Tag manager | All tags with usage counts, edit/merge/delete tags, color picker |
| Add link | URL input field + auto-fetched preview; tag and project pickers |
| Settings | Cloud AI fallback toggle, storage usage, export/import, theme |

**Design system:** Material 3 with dynamic color (Material You on supported devices). Dark mode supported. Compose Multiplatform components throughout.

## Build Roadmap

**Phase 1 — Link management (Week 1–2)**

- [ ] Set up KMP project with Compose Multiplatform, Room, Koin
- [ ] Create `Card` sealed class and `LinkCard` data model
- [ ] Build Room database with `links` and `tags` tables + FTS
- [ ] Implement share-sheet intent receiver for URLs
- [ ] Build metadata fetcher (Ktor: title, description, favicon)
- [ ] Links tab UI: list with favicon, title, domain, tags
- [ ] Add link screen with auto-preview
- [ ] Link detail screen with notes editor and tag chips
- [ ] Tag manager screen (create, edit, delete, color)
- [ ] Favorites and sort/filter

**Phase 2 — Screenshot capture + OCR (Week 3–4)**

- [ ] Add `ScreenshotCard` data model and `screenshots` table + FTS
- [ ] Implement `ContentObserver` for auto-detecting new screenshots
- [ ] Manual import from gallery
- [ ] Integrate ML Kit Text Recognition (OCR pipeline)
- [ ] Integrate ML Kit Image Labeling (auto-tag pipeline)
- [ ] Thumbnail generation
- [ ] Captures tab UI: grid layout with thumbnails
- [ ] Screenshot detail screen: full image, extracted text, tags

**Phase 3 — On-device AI + search (Week 5–6)**

- [ ] Integrate Gemini Nano via AICore with cloud fallback
- [ ] AI tag suggestions on link save and screenshot capture
- [ ] AI description generation for screenshots
- [ ] Unified search tab: full-text across links + screenshots
- [ ] Search result ranking and highlighting

**Phase 4 — Polish and ship (Week 7–8)**

- [ ] Settings screen: storage usage, AI toggle, theme, export/import
- [ ] Bookmark bulk import (browser HTML format)
- [ ] Material 3 dynamic color and dark mode pass
- [ ] Edge cases: large images, no-network fallback, permission flows
- [ ] Testing and Play Store submission

**Stretch goals (post-launch):**

- [ ] Widget for quick link/screenshot capture
- [ ] Cross-device metadata sync (metadata only, not images)
- [ ] Browser extension for saving links from desktop
- [ ] Connection point with the companion routine/meal-planning app (suggest links for leisure time)
