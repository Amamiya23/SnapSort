# Android Development Guidelines

SnapSort is a single-module Android application, not a backend service. This
spec layer replaces the default backend scaffold created by `trellis init` and
documents the real Kotlin, Compose, Room, DataStore, SAF, and MediaStore
patterns used by the app.

## Guidelines Index

| Guide | Purpose |
|-------|---------|
| [Architecture](./architecture.md) | Module layout, dependency direction, feature placement, and naming patterns |
| [Data And Storage](./data-and-storage.md) | Room, DataStore, SAF folder scanning, EXIF parsing, and settings persistence |
| [Deletion Safety](./deletion-safety.md) | Mark/delete flow, RAW pairing impact, confirmation UI, MediaStore/SAF deletion behavior |
| [Compose UI](./compose-ui.md) | Compose screen structure, state collection, navigation, theme, copy, and accessibility |
| [Error Handling And Logging](./error-handling-and-logging.md) | User-facing failures, fallback behavior, coroutine dispatchers, and log usage |
| [Quality Guidelines](./quality-guidelines.md) | Test expectations, verification commands, code review checks, and risky changes |

## Pre-Development Checklist

Always read:

- [Architecture](./architecture.md)
- [Quality Guidelines](./quality-guidelines.md)
- `.trellis/spec/guides/index.md`

Also read the topic guide that matches the change:

- Data model, scanning, settings, or persistence changes: [Data And Storage](./data-and-storage.md)
- Delete marking, delete confirmation, MediaStore, SAF, or RAW pairing changes: [Deletion Safety](./deletion-safety.md)
- Compose screens, navigation, theme, copy, gestures, or accessibility changes: [Compose UI](./compose-ui.md)
- New failure paths, fallback behavior, or logging changes: [Error Handling And Logging](./error-handling-and-logging.md)

## Quality Check

Before marking an implementation task complete:

- Read [Quality Guidelines](./quality-guidelines.md) and the topic guide for
  the area changed.
- Run `:app:testDebugUnitTest` for behavior, copy, theme, or core logic changes.
- Run `:app:compileDebugKotlin` for every code change.
- For delete, scan, Room, settings, or navigation changes, trace the workflow
  across UI, ViewModel, repository/scanner, and persisted state before
  committing.
- Confirm deletion-related changes still preserve JPG count, RAW count, total
  file count, expandable file list, and failed-item recovery behavior.
- Confirm no template placeholders or stale spec links were introduced when
  changing `.trellis/spec/`.

## Project Principles

- The app optimizes for low-anxiety photo review. Deletion impact must be
  visible before action, including JPG count, RAW count, total files, and an
  expandable file list.
- Keep local photo/file details out of the main workflow unless the user is
  confirming, recovering, or reviewing a failure.
- Prefer explicit buttons and clear state over hidden gestures. Swipe gestures
  can accelerate selection, but must not be the only way to mark or unmark a
  photo.
- Follow `AGENTS.md` for product design context: restrained, refined,
  efficient, photo-first, and compatible with WCAG AA expectations.
