<p align="center">
  <img src="app/androidApp/src/main/ic_launcher-playstore.png" alt="Lords of Links logo" width="128" />
</p>

<h1 align="center">Lords of Links</h1>

<p align="center">
  <b>Save it once, find it later.</b><br/>
  An offline-first link saver for Android, built with Kotlin Multiplatform and Compose Multiplatform.
</p>

Share a link from any app and Lords of Links fetches its title, description and preview image, then files it under your tags and nested categories. Old links come back to the top of your feed so they don't get buried.

> Status: Android MVP (milestone 1) in active development. Web app, Chrome extension and sync come in milestone 2.

## Features

- **Save from anywhere**: Android share sheet or paste a URL; the title, description and `og:image` are fetched on save
- **Organize**: tags, nested categories, favorites, archive
- **Feed**: filters and smart lists, plus resurfacing of links you saved and forgot
- **Your data stays yours**: fully offline, JSON backup / restore, export to the browser bookmarks HTML format
- **Material 3 Expressive** UI with a custom design system, motion, light and dark themes, and a first-run tour

## Tech stack

| Area | Choice |
| --- | --- |
| Language / UI | Kotlin 2.2, Compose Multiplatform, Material 3 Expressive |
| Architecture | MVVM + use cases, [Molecule](https://github.com/cashapp/molecule) presenters, unidirectional state |
| Navigation | Navigation 3 with one back stack per tab and adaptive list-detail panes |
| Data | Room KMP (SQLite) with migrations; repositories return `Flow<Result<T>>` |
| Networking | Ktor |
| DI | Koin |
| Images | Coil |
| Build | AGP 9, Gradle 9 convention plugins (`build-logic/`), version catalog |
| Quality | Unit tests, Kover coverage, detekt |
| CI/CD | GitHub Actions: tests and a signed AAB on every push to `main`; the Play Store publish waits for a manual approval |

Targets: **Android** (primary), JVM desktop (runs, used for fast tests). iOS and web are planned.

## Architecture

```
app/            App shell: DI, root + tab navigation, welcome tour
  androidApp/   Android entry point, flavors, share-intent handling
bookmarks/      Feature: feed, add link, details, tags, categories
settings/       Feature: theme, backup / restore, export
database/       Room entities, DAOs, migrations, repository implementations
core/model/     Domain models + repository interfaces (no Room, so web can reuse it)
core/design/    Design system: cards, icons, inputs, scrolling, theme
core/utils/     Molecule ViewModel base
build-logic/    Convention plugins shared by every module
```

Feature modules depend only on `core/model` interfaces, never on Room. Only the app entry points see the `database` module, so the storage can be swapped (for example for sync) without touching screens.

More detail: [docs/architecture/AGP9_AND_PATTERNS.md](docs/architecture/AGP9_AND_PATTERNS.md).

## Build variants

Three flavors install side by side on one device, each with its own labelled icon:

| Flavor | Application id | Use |
| --- | --- | --- |
| `dev` | `com.greenrobotdev.lordofthelinks.android.dev` | Day-to-day development |
| `qa` | `com.greenrobotdev.lordofthelinks.android.qa` | Testing builds |
| `prod` | `com.greenrobotdev.lordofthelinks.android` | Play Store |

## Getting started

Requires JDK 17+ and Android Studio (AGP 9 support).

```bash
./gradlew :app:androidApp:installDevDebug
```

```bash
./gradlew :app:jvmTest :bookmarks:jvmTest :database:jvmTest :settings:jvmTest
```

```bash
./gradlew detekt koverHtmlReport
```

## Roadmap

1. **Android MVP, offline** (now)
2. **Accounts + sync**, then the web app (Kotlin/Wasm) and a Chrome extension, on an AWS serverless backend (Lambda, API Gateway, DynamoDB, Cognito) defined with CDK
3. **Smart layer**: screenshot capture with on-device OCR, on-device AI tag suggestions (Gemini Nano), semantic search, reminders
4. **iOS and desktop**

Plans live in [docs/plans/](docs/plans/), for example [screenshots + OCR](docs/plans/screenshots-android-v1.md).
