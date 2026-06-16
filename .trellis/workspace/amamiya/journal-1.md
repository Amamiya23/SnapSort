# Journal - amamiya (Part 1)

> AI development session journal
> Started: 2026-05-29

---



## Session 1: Bootstrap Trellis Android specs

**Date**: 2026-05-29
**Task**: Bootstrap Trellis Android specs
**Branch**: `main`

### Summary

Initialized Trellis files, replaced the default backend scaffold with Android-specific SnapSort specs, validated the spec layer, and archived the bootstrap guidelines task.

### Main Changes

- Added project-local Trellis agent, hook, workflow, and script files.
- Replaced the default backend spec scaffold with `.trellis/spec/android/`.
- Documented SnapSort architecture, data/storage, deletion safety, Compose UI,
  error/logging, and quality conventions with source-backed examples.
- Updated and archived the `00-bootstrap-guidelines` task.

### Git Commits

| Hash | Message |
|------|---------|
| `0ec0bbe` | (see git log) |

### Testing

- [OK] `python3 ./.trellis/scripts/get_context.py --mode packages`
- [OK] `python3 ./.trellis/scripts/task.py validate .trellis/tasks/00-bootstrap-guidelines`
- [OK] `:app:testDebugUnitTest`
- [OK] `:app:compileDebugKotlin`

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 2: Automatic APK versioning script

**Date**: 2026-05-29
**Task**: Automatic APK versioning script
**Branch**: `main`

### Summary

Added a build helper that generates APK version metadata automatically, documented usage, verified debug/release builds, and captured the command contract in Android quality guidelines.

### Main Changes

- Added `scripts/build-apk.sh` to package release/debug APKs with generated `VERSION_CODE` and `VERSION_NAME`.
- Updated `app/build.gradle.kts` to read optional Gradle version properties while preserving direct-build defaults.
- Documented script usage in `README.md`.
- Added the build-versioning command contract to Android quality guidelines.

### Git Commits

| Hash | Message |
|------|---------|
| `dbcf982` | (see git log) |

### Testing

- [OK] `bash -n scripts/build-apk.sh`
- [OK] `:app:compileDebugKotlin`
- [OK] `:app:testDebugUnitTest`
- [OK] `scripts/build-apk.sh --debug --name vtest-auto-version`
- [OK] `scripts/build-apk.sh --release --name vtest-auto-version-release`

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 3: Ignore local agent directories

**Date**: 2026-05-29
**Task**: Ignore local agent directories
**Branch**: `main`

### Summary

Ignored Claude, Codex, Trellis, and Agents local directories for the public repository while keeping local files available.

### Main Changes

- Added persisted nullable exposure fields for aperture, shutter speed, and ISO.
- Added shared EXIF reader used by scanning and selection-screen backfill.
- Updated photo selection top metadata to show file name, RAW state, and available exposure values without showing capture time.
- Added exposure formatting helpers and unit tests.

### Git Commits

| Hash | Message |
|------|---------|
| `5275f7d` | (see git log) |
| `4e90d81` | (see git log) |
| `6bd3361` | (see git log) |

### Testing

- [OK] `/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:compileDebugKotlin`
- [OK] `/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:testDebugUnitTest`
- [OK] `git diff --check`

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 4: Settings overscroll stretch

**Date**: 2026-05-29
**Task**: Settings overscroll stretch
**Branch**: `main`

### Summary

Implemented and tuned settings screen edge stretch overscroll to avoid whole-screen translation and better match home screen behavior.

### Main Changes

(Add details)

### Git Commits

| Hash | Message |
|------|---------|
| `ac8db4d` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete


## Session 5: Photo selection EXIF metadata

**Date**: 2026-06-16
**Task**: Photo selection EXIF metadata
**Branch**: `main`

### Summary

Added photo selection metadata display for file name, RAW status, and EXIF exposure values, with Room migration and background backfill for existing scans.

### Main Changes

(Add details)

### Git Commits

| Hash | Message |
|------|---------|
| `8478499` | (see git log) |

### Testing

- [OK] (Add test results)

### Status

[OK] **Completed**

### Next Steps

- None - task complete
