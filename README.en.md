# LifeLedger

[简体中文](README.md) | **English**

> Treat your life like a game: record it, sort it out, look back on every step you have taken.

A **fully offline** Android app for recording personal achievements. No account, no server, no analytics — all your data stays on your own phone.

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.12-4285F4)
![Room](https://img.shields.io/badge/Room-2.7.2-4285F4)
![minSdk](https://img.shields.io/badge/minSdk-24-orange)
![Offline](https://img.shields.io/badge/Offline-100%25-brightgreen)
![License](https://img.shields.io/badge/License-MIT-green)

> **Rather not build it yourself?** Download the APK from [Releases](https://github.com/mrlingan/LifeLedger/releases/latest) and install it. Android 7.0 and above.

Built and maintained by **[mrlingan](https://github.com/mrlingan)**.

---

## What is this

Most "tracking" apps end up as to-do lists: add an item, tick it off, forget it.

This project tries something a little different — it treats **what has already happened** as something to collect, instead of stacking up **what has not happened yet** in front of you. So:

- the home screen is a life dashboard: the first thing you see is **how much you have already completed**, not how much is left
- every achievement keeps its own details, with photos and videos attached, so it becomes something you can look back on
- a built-in codex of 109 achievements to collect and unlock
- completion dates can be set by hand — because *when* something happened should be your call

---

## Features

### 📊 Home · life dashboard

- Greeting and date, changing with the time of day
- Headline number: completed achievements (44sp hero) plus a completion ring
- **Recently unlocked**: a horizontally scrolling collection
- **Life stats**: total achievements, streak days, completions in the last 7 days
- One entry at the bottom for **All achievements**, showing how many you have recorded and how many are in progress
- The home screen is overview only — **no achievement list on it**, so it never grows longer as you record more

### 🏆 Achievements · kept apart from the home screen

- **All achievements**: its own list screen with All / Completed / In progress filters (the filter stays pinned at the top), and a one-tap completion toggle on the right of every row
- New achievement: pick one from the codex to prefill it, or write your own (title, description, icon)
- Edit and delete (deleting also cleans up that achievement's notes and media)
- Mark complete / undo, with a **manually chosen completion date** (system date picker)
- Detail screen: large icon hero, unlock timeline, related stats, notebook

### 📖 Achievement codex

- 109 preset achievements across 14 categories: growth, life, travel, study, love, entertainment, social, hobbies, skills, career, family, newbie village, health, finance
- Five rarity tiers — bronze / silver / gold / platinum / legendary — derived from how rare each achievement is
- Keyword search, category filter, sections by rarity
- Unlocked entries carry visual weight; locked ones keep their outline
- "Pick from the codex" prefills title, description and icon on the new-achievement screen
- **The codex shares one source of truth with the rest of the app**: undo a completion anywhere and the codex goes back to locked

### 📝 Notebook and media

- Several notes per achievement, each editable and deletable
- Notes can carry images, videos and **live photos**
- Live photos are parsed so the embedded video is stored separately — long-press the image to play it once
- Every media file is copied into the app's private folder, so nothing depends on the original in your gallery

### 💾 Backup

- One-tap export to a `.zip` containing all structured data plus the original images and videos
- Import with a second confirmation (restore overwrites everything)
- Media paths inside a backup are relative, so **restoring on a new phone works**

### ⚙️ Settings

- **Theme**: follow system / light / dark
- **App lock**: fingerprint, face or device PIN; re-locks 30 seconds after you leave the app
- **Language**: follow system / 简体中文 / English
- Backup and restore, current data size, about

### 🎨 Interface

- A complete custom design system (colour / type / spacing / radius / motion)
- **No default-looking Material components**: cards, buttons, text fields, dialogs, switches and filters are all hand-built
- Dark mode has its own palette, not an inverted one
- **Chinese and English**: follow the system language; all 109 codex titles, descriptions and stories are fully translated

---

## Screenshots

| Home | All achievements | Achievement detail |
|:---:|:---:|:---:|
| ![Home](docs/screenshots/en/home.png) | ![All achievements](docs/screenshots/en/all-achievements.png) | ![Achievement detail](docs/screenshots/en/detail.png) |
| Achievement codex | Settings | Dark mode |
| ![Codex](docs/screenshots/en/codex.png) | ![Settings](docs/screenshots/en/settings.png) | ![Dark mode](docs/screenshots/en/home-dark.png) |

> Screenshots are taken from a real device. Chinese screenshots are in [README.md](README.md).

---

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material 3 only as a host for a few components) |
| Architecture | MVVM + Repository |
| Local storage | Room 2.7.2 (database version 4, with 1→2→3→4 migrations) |
| Navigation | Navigation Compose 2.8.9 |
| Async | Coroutines + Flow |
| Images | System photo picker (no storage permission needed) |
| Security | AndroidX Biometric 1.1.0 |
| Build | AGP 9.3.0 · Gradle 9.5 · JDK 17 |
| Minimum | Android 7.0 (API 24) |

**No networking library is included** — this project is offline by design.

---

## Design system

The design system was built before any screen was written, and every screen takes its styling from it.

```
ui/theme/
├── Color.kt     semantic palette: neutral greys + a single teal accent + five rarity metals
├── Type.kt      10 type styles, numbers on their own scale with tabular figures (tnum)
├── Dimens.kt    spacing 4/8/12/16/24/32/48/64, radii 8/12/16/20
├── Shape.kt     shapes derived from the radius tokens
├── Motion.kt    motion at 150/200/250/300ms with three easing curves
└── Theme.kt     maps Material roles onto the semantic palette
```

A few rules that are deliberately followed:

- **no hard-coded colours** in screens (they all live in one file)
- **no dp values outside the system**
- hierarchy comes from surface contrast + 1dp outlines, **never shadows**
- motion is limited to 8dp shifts and fades, no bounce

Public components live in `ui/components/`, 16 of them: `AppTopBar`, `AppCard`, `AppButton`, `AppIconButton`, `AppTextField`, `AppDialog`, `AppSnackbar`, `AppChip`, `AppSwitch`, `AppSettingRow`, `SegmentedControl`, `AchievementCard`, `RarityBadge`, `StatusBadge`, `MetricNumber` / `StatTile`, `ProgressBar` / `ProgressRing` / `IndeterminateBar`, `SectionHeader` / `TimelineItem`, `EmptyState`, `AppearAnimation`.

---

## Getting started

### Install directly

Download the latest `LifeLedger-vX.Y.Z.apk` from [Releases](https://github.com/mrlingan/LifeLedger/releases/latest), allow installing apps from unknown sources in your phone's settings, and install it. No account, no permissions to grant.

> Builds are signed with APK Signature Scheme v2, which covers `minSdk 24` (Android 7.0) and above. The SHA-256 for verification is in each release's notes.

### Build from source

Requirements:

- Android Studio (latest stable recommended)
- JDK 17
- Android SDK Platform 35

```bash
git clone https://github.com/mrlingan/LifeLedger.git
cd LifeLedger
```

Open the project root in Android Studio → wait for Gradle sync → Run.

> **Note for users in mainland China**: the project already configures an Aliyun mirror in `settings.gradle.kts`, with the official repositories as a fallback. If sync is still slow, add `C:\Users\<you>\.gradle` and the project folder to your Windows Defender exclusions — real-time scanning is the main reason builds like this are slow.

### Before you publish your own build

- the app name in `app/src/main/res/values/strings.xml`
- the launcher icon (`res/mipmap-*`)
- if you maintain a fork, change `applicationId` (`com.Anchored.mylife`) in `app/build.gradle.kts` — **the system will treat it as a brand-new app, so old data will not migrate**; export a backup and import it instead

---

## Project structure

```
app/src/main/
├── java/com/Anchored/mylife/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── backup/        backup and restore (zip packing / unpacking / data migration)
│   │   ├── dao/           Room DAOs
│   │   ├── database/      entities, database, migrations, database provider
│   │   ├── media/         media file copying, live photo parsing
│   │   ├── preset/        preset achievement seeder
│   │   ├── repository/    repository layer and single entry point
│   │   └── settings/      app settings (SharedPreferences)
│   └── ui/
│       ├── components/    shared UI components
│       ├── theme/         design system
│       ├── AchievementNavHost.kt    navigation, theme, app lock
│       ├── HomeScreen.kt / HomeViewModel.kt        home overview (no list)
│       ├── AllAchievementsScreen.kt / AchievementListViewModel.kt   achievement list
│       ├── AchievementRow.kt        list row (the only implementation of that look)
│       ├── AddOptionsSheet.kt       "pick from the codex / write my own" sheet
│       ├── AchievementDetailScreen.kt / ViewModel
│       ├── AddAchievementScreen.kt
│       ├── PresetAchievementScreen.kt / ViewModel
│       ├── SettingsScreen.kt / ViewModel
│       ├── BackupScreen.kt / ViewModel
│       ├── MediaComponents.kt
│       ├── AppLockGate.kt
│       └── PresetText.kt / PresetStringRes.kt   codex text localisation
├── assets/
│   └── preset_achievements.json   109 preset achievements (Chinese source, used as stable ids)
└── res/
    ├── values/           English (default)
    └── values-zh/        Chinese

scripts/preset_i18n/      codex text generator (re-run after editing achievement content)
docs/screenshots/         UI screenshots for the README (zh/ and en/ sets)
```

---

## Data and privacy

**Completely offline.** The app does not request the network permission and contains no networking code.

| Data | Where it lives |
|---|---|
| Achievements, notes, media records, codex progress | Room database (app-private storage) |
| Images, videos, live photo copies | `files/media/` |
| Theme, app lock and other preferences | SharedPreferences |

Uninstalling the app deletes all of it, so **export a backup before switching phones**.

A backup file is a zip:

```
lifeledger_backup_20260912_1040.zip
├── backup.json         all structured data
└── media/              the original images and videos
```

Importing clears the current data first and then writes the backup in — media files are stored again by file name in the current device's private folder, which is why cross-device restores line up correctly.

---

## Built-in achievement library

The 109 achievements in the codex come from the open-source project **[EarthOnline-Achievement](https://github.com/LKlingkong/EarthOnline-Achievement)** (declared MIT in that project's README), including each achievement's title, description, full story, category and completion rate.

The source data is kept as-is, with two adjustments:

- **Rarity**: the original data has four tiers (normal / rare / epic / legendary) with some labels contradicting their own completion rates. This project derives five tiers from the rate instead (bronze / silver / gold / platinum / legendary), without touching the source numbers.
- **Data structure**: the original keeps a one-line description and the full story separately; this project does the same, using them for the list and the detail sheet respectively.

### Multi-language

The codex text stored in the database is the original Chinese, and it is used only as a **stable identifier** (ids, backups and import/export all rely on it). What the UI shows is decided by `res/values/preset_strings.xml` (English, default) and `res/values-zh/preset_strings.xml` (Chinese). Switching the system language therefore takes effect immediately, with no database migration and no changes to data you have already recorded.

After editing achievement content, re-run the generator to produce those string resources:

```bash
pwsh -File scripts/preset_i18n/generate_preset_i18n.ps1
```

The script reads `source.tsv` (Chinese source) and `en.json` (English translation) from the same folder and writes the result to `out/`; copy that over `app/src/main/res/values*/preset_strings.xml` and `app/src/main/java/com/Anchored/mylife/ui/PresetStringRes.kt`. Any entry missing from the resources simply falls back to the Chinese source text — nothing crashes.

---

## Roadmap

- [x] Create, edit, complete and delete achievements
- [x] Custom completion dates
- [x] Notes and media (images / videos / live photos)
- [x] 109-achievement codex with unlocking
- [x] Backup and restore
- [x] Design system and a full UI redraw
- [x] Dark mode and app lock
- [ ] Icon system: `iconEmoji` → `iconKey`, unified vector icons (with a database migration and mapping for existing data)
- [ ] Daily reminder (WorkManager)
- [ ] Statistics screen: yearly recap, growth curve, timeline
- [ ] Display and animation intensity settings
- [ ] Profile (nickname, avatar)

---

## Known limitations

- **Achievement icons are emoji today.** The data model stores emoji characters, which render differently depending on the manufacturer's system font; this will move to built-in vector icons (see the roadmap).
- Codex entries without an icon fall back to the first character of the title.
- The app lock relies on the device's fingerprint / face / PIN. If the device has no verification method set up, it cannot be enabled (deliberate, so you cannot lock yourself out).
- The database is not encrypted (no SQLCipher); sensitive data relies on the Android app sandbox.

---

## Contributing

Issues and pull requests are welcome. Before submitting:

1. Run `./gradlew assembleDebug` and make sure it builds
2. When adding UI, reuse the components in `ui/components/` and don't put colours or sizes directly in screens
3. When changing the database schema, **always add a migration** and make sure old data can be upgraded

---

## License

[MIT License](LICENSE) © 2026 mrlingan

You are free to use, modify and distribute this project, including commercially, as long as you keep the copyright notice.

---

## Credits

- [EarthOnline-Achievement](https://github.com/LKlingkong/EarthOnline-Achievement) — the source of the 109 preset achievements
- [AndroidX](https://developer.android.com/jetpack) — Room / Compose / Navigation / Biometric
- [Material Symbols](https://fonts.google.com/icons) — functional UI icons

---

**Everyone is the protagonist of their own life.**
