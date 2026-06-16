# Design: 筛选界面显示照片名称与曝光信息

## Architecture

Data should continue to flow through the existing boundaries:

1. `PhotoExifReader` reads JPG EXIF metadata.
2. `FolderScanner` uses `PhotoExifReader` while scanning JPG files.
2. `core.ScannedPhoto` carries stable, nullable exposure metadata.
3. `TaskRepository` maps scanned metadata into Room.
4. `PhotoSelectionViewModel` formats values for display.
5. `PhotoSelectionScreen` renders a compact metadata surface.

Compose must not open image streams or read EXIF directly.

## Data Model

Add nullable exposure fields to `ScannedPhoto` and `PhotoEntity`:

- `aperture: Double?`
- `shutterSpeedSeconds: Double?`
- `iso: Int?`

Room change:

- Bump `SnapSortDatabase` from version 1 to 2.
- Add a migration that adds nullable columns to `photos`.
- Existing rows get `NULL` exposure values. The next rescan populates them.
- If an existing row has no exposure metadata, the selection ViewModel may
  ask `PhotoExifReader` to read the current image in the background and update
  the row when exposure metadata is found. This keeps upgraded recent tasks
  useful without forcing a rescan.
- Export schema version 2 after verification.

## EXIF Reading

Extend scanner EXIF parsing so the image stream is opened once per JPG for both capture time and exposure metadata.

Expected tags:

- Aperture: prefer `TAG_F_NUMBER`; fallback can be considered only if already available cleanly through `ExifInterface`.
- Shutter: use exposure time seconds from `TAG_EXPOSURE_TIME`.
- ISO: use `TAG_PHOTOGRAPHIC_SENSITIVITY` and fallback to `TAG_ISO_SPEED_RATINGS` if needed by the AndroidX API in this project.

Failures return null fields and keep scan progress moving.

## Formatting

Formatting belongs in `PhotoSelectionViewModel` or a small `ui/copy` helper if unit tests are valuable.

Display conventions:

- Aperture: `f/2.8`, trim unnecessary trailing zeroes.
- Shutter:
  - `1/500` for common fractions under one second.
  - `0.5s` or similar for slower values that do not map cleanly to a simple reciprocal.
  - `2s` for whole-second exposures.
- ISO: `ISO 400`.

The UI item should expose display-ready strings or a small display model so the composable remains simple.

## UI Direction

Use the existing `TopAppBar` metadata area as the anchor because it already appears/disappears with review controls and keeps photo gestures intact. The metadata should remain visible while the overlay controls are visible.

Required structure:

- First line: group title and index, unchanged.
- Second line: file name plus RAW status, visually primary among metadata but smaller than title.
- Third line: one compact inline row containing aperture, shutter, and ISO. Missing exposure values are omitted from this row. If all exposure values are missing, omit the row.

Keep the style restrained:

- No decorative gradients, loud badges, or card-inside-card treatment.
- Use `onSurfaceVariant` and small typography for metadata.
- Make long file names ellipsize.
- Keep touch targets and bottom controls unchanged.

## Compatibility And Rollback

If the migration creates issues, rollback is limited to removing added columns and UI fields before release because the feature has no destructive side effects. Existing delete state must not be affected by nullable metadata columns.

## Risks

- EXIF rational formatting can produce noisy values if not normalized.
- Top app bar can become cramped on narrow screens or large font sizes.
- Scanner performance may be affected if EXIF reads are duplicated; avoid opening streams twice.
