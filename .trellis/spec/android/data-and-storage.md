# Data And Storage

## Room Database

Room is the persistent task database:

- Entities live in `app/src/main/java/com/snapsort/app/data/db/SnapSortEntities.kt`.
- DAO queries and write operations live in `SnapSortDao.kt`.
- The database singleton lives in `SnapSortDatabase.kt`.
- The database name is `snapsort.db`.
- `exportSchema = true`; kapt writes schemas to `app/schemas` per
  `app/build.gradle.kts`.

The current model stores one recent task, identified by
`RECENT_TASK_ID = "recent"`:

- `TaskEntity` stores folder URI/name, scan timestamp, burst threshold, and sort
  direction.
- `PhotoGroupEntity` belongs to a task and cascades on task deletion.
- `PhotoEntity` belongs to a task and group, is keyed by `jpgUri`, and stores
  optional RAW match metadata.

When changing entities:

- Increment the Room database version in `SnapSortDatabase.kt`.
- Add an explicit migration or deliberately document why destructive migration
  is acceptable for the app state being changed.
- Preserve `exportSchema = true` unless the build strategy changes.
- Treat enum strings persisted with `.name` as compatibility contracts.
  Examples: `TaskEntity.sortDirection`, `PhotoGroupEntity.kind`,
  `PhotoEntity.captureTimeSource`.

## DAO And Repository Pattern

DAO methods should stay small and query-focused:

- Observed UI data is exposed as `Flow`, such as `observeTask`,
  `observeGroups`, `observePhotos`, and `observePhotosForGroup`.
- Mutations are `suspend` functions.
- Multi-step database replacement uses a DAO-level `@Transaction`.
  `replaceRecentTask` deletes and reinserts the task, groups, and photos in one
  transaction.

Repository methods own application-level rules:

- `TaskRepository.saveRecentTask` maps scanned core groups into Room entities.
- It preserves existing delete marks by reading `getMarkedPhotoUris()` before
  replacing the recent task.
- It stores group and photo positions with `mapIndexed`.
- It guards empty mutation inputs before calling DAO methods, as in
  `clearDeleteMarks` and `removeDeletedPhotos`.
- `removeDeletedPhotos` updates group cover photos, deletes empty groups, and
  removes the task when no photos remain.

Avoid:

- Calling DAO methods directly from UI code.
- Recomputing database entity IDs in multiple layers.
- Storing localized display strings in Room entities. Store stable values such
  as enum names and timestamps, then format in UI/copy helpers.

## Folder Scanning

`FolderScanner.scan` is the scanning entry point:

- Input: SAF tree `Uri` plus `ScanSettings`.
- Output: `Flow<ScanEvent>`.
- Threading: the flow uses `flowOn(Dispatchers.IO)`.
- Progress is emitted as `ScanEvent.Progress`; completion as
  `ScanEvent.Complete`; unrecoverable folder access as `ScanEvent.Failed`.

The scanner follows this order:

1. Resolve the tree URI with `DocumentFile.fromTreeUri`.
2. Query child documents through `DocumentsContract` for efficient metadata.
3. Fall back to `DocumentFile.listFiles()` if the direct query fails.
4. Filter files through `isJpgFile` and `isSupportedRawFile`.
5. Match RAW files by exact case-insensitive base name via `matchRawFiles`.
6. Read capture time from EXIF `TAG_DATETIME_ORIGINAL`.
7. Fall back to document modified time when EXIF is missing or unreadable.
8. Group photos with `groupPhotos`.

Keep scan progress throttled. The current scanner emits at the first file, last
file, every 32 files, or after 120 ms since the last progress update.

## Settings DataStore

Preferences DataStore is used for user settings:

- The DataStore extension is `Context.userSettingsDataStore` in
  `UserSettingsRepository.kt`.
- Defaults are held in `UserSettings`.
- Mutations are explicit setter methods on `UserSettingsRepository`.
- `ThemeMode` and `SortDirection` are stored by enum `.name`.

Special compatibility rule:

- `Long.MAX_VALUE` was used as a legacy "do not split loose groups" sentinel.
  The repository normalizes this into `autoSplitLooseGroups = false` and the
  default loose threshold. Keep that behavior when touching loose-group
  settings or scan settings.

## Core Data Rules

Core photo rules are tested and should remain deterministic:

- JPG detection is case-insensitive and accepts `jpg` and `jpeg`.
- Supported RAW extensions are defined in `RawMatcher.kt`.
- RAW matching is exact base-name matching, case-insensitive, with no fuzzy
  suffix matching.
- Grouping sorts by capture time, lowercase file name, then JPG URI for stable
  ordering.
- Burst grouping uses adjacent time intervals.
- Loose grouping can split by fixed local-time buckets.
- Newest-first reverses both group order and photos inside each group while
  preserving each group's oldest-to-newest time range.

Reference tests:

- `PhotoGrouperTest.kt`
- `RawMatcherTest.kt`

