# Implementation Plan: 筛选界面显示照片名称与曝光信息

## Checklist

1. Verify current ExifInterface APIs available in the project before coding tag reads if needed.
2. Add nullable exposure metadata to `ScannedPhoto`.
3. Extend `FolderScanner` to read capture time and exposure metadata in one EXIF pass.
4. Add nullable exposure columns to `PhotoEntity`.
5. Bump Room version and add migration from 1 to 2.
6. Update `TaskRepository` mapping.
7. Extend `PhotoSelectionItem` with file name and formatted exposure display.
8. Render a compact, polished metadata layout in `PhotoSelectionScreen`.
9. Add or update unit tests for exposure formatting and any migration-testable pure helpers.
10. Run:
    - `/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:testDebugUnitTest`
    - `/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:compileDebugKotlin`

## Review Gates

- Confirm Room migration preserves old rows and compiles with exported schemas.
- Confirm scanner still handles unreadable EXIF as non-fatal.
- Confirm delete marking and navigation controls are untouched.
- Inspect UI for long filename behavior and missing EXIF behavior.

## Risky Files

- `app/src/main/java/com/snapsort/app/data/db/SnapSortDatabase.kt`
- `app/src/main/java/com/snapsort/app/data/db/SnapSortEntities.kt`
- `app/src/main/java/com/snapsort/app/data/scanner/FolderScanner.kt`
- `app/src/main/java/com/snapsort/app/ui/selection/PhotoSelectionScreen.kt`

## Rollback Point

After migration/entity changes compile, verify the UI separately. If UI density is poor, keep data changes and revise only the metadata presentation.
