# Architecture

## Project Shape

SnapSort is a single Gradle module Android app:

- Root Gradle project: `settings.gradle.kts` includes only `:app`.
- Android plugin and dependencies live in `app/build.gradle.kts`.
- App package namespace and source root: `com.snapsort.app` under
  `app/src/main/java/com/snapsort/app`.
- Unit tests mirror package paths under `app/src/test/java/com/snapsort/app`.

The app currently uses Kotlin, Jetpack Compose, Material 3, Navigation Compose,
Room, Preferences DataStore, SAF/DocumentFile, ExifInterface, MediaStore delete
requests, and Coil.

## Source Layout

Use the existing package boundaries when adding code:

- `MainActivity.kt`: Compose entry point, edge-to-edge setup, theme mode
  selection from settings.
- `SnapSortDependencies.kt`: small service locator. It creates repositories,
  scanner, and settings repository from `applicationContext`. There is no DI
  framework in this project.
- `core/`: pure Kotlin photo-domain logic and models. Examples:
  `PhotoGrouper.kt`, `RawMatcher.kt`, `PhotoModels.kt`.
- `data/db/`: Room entities, DAO, and database singleton. Examples:
  `SnapSortEntities.kt`, `SnapSortDao.kt`, `SnapSortDatabase.kt`.
- `data/repository/`: app data operations and mapping between core models and
  persisted entities. Example: `TaskRepository.kt`.
- `data/scanner/`: Android folder scanning, EXIF reads, SAF queries, and scan
  progress events. Example: `FolderScanner.kt`.
- `data/settings/`: Preferences DataStore settings and defaults. Example:
  `UserSettingsRepository.kt`.
- `ui/<feature>/`: feature screens and ViewModels, such as `home`, `scan`,
  `selection`, `delete`, and `settings`.
- `ui/components/`: reusable UI components, such as `DeleteConfirmationSheet.kt`
  and `ZoomableImage.kt`.
- `ui/copy/`: reusable or safety-sensitive UI copy helpers. These are covered
  by unit tests.
- `ui/navigation/`: app-level route wiring and Android activity result
  launchers. Example: `SnapSortNavigation.kt`.
- `ui/theme/`: Material 3 color, typography, and dynamic color policy.
- `ui/transition/`: small transition data contracts, such as
  `PhotoOpenTransitionSpec.kt`.

## Dependency Direction

Keep dependencies flowing inward:

- `core` must stay framework-free. Do not import Android, Room, Compose,
  DataStore, or ContentResolver APIs into `core`.
- `data` may depend on `core` and Android framework APIs.
- `ui` may depend on `data`, `core`, and Compose APIs.
- Composables should not call DAOs or ContentResolver directly. Route through a
  ViewModel and repository/scanner layer.

Source examples:

- `PhotoGrouper.kt` and `RawMatcher.kt` are pure functions with unit tests.
- `FolderScanner.kt` owns SAF and EXIF work and returns `Flow<ScanEvent>`.
- `TaskRepository.kt` maps `core.PhotoGroup` and `core.ScannedPhoto` into Room
  entities and exposes delete candidates to the UI layer.
- `HomeViewModel.kt`, `ScanProgressViewModel.kt`, `PhotoSelectionViewModel.kt`,
  and `SettingsViewModel.kt` expose immutable `StateFlow` values to screens.

## Feature Placement

Use these placement rules for new work:

- New grouping, matching, time-bucketing, sorting, or extension-detection logic
  belongs in `core/` with unit tests.
- New filesystem, EXIF, SAF, MediaStore, or ContentResolver behavior belongs in
  `data/scanner/` or a repository, not in a composable.
- New persisted task state belongs in Room under `data/db/` and should be
  surfaced through `TaskRepository`.
- New user preferences belong in `UserSettingsRepository` with explicit
  defaults and setter methods.
- New screen-local display transformations belong in the relevant ViewModel.
- Reusable UI copy and destructive-action wording belong in `ui/copy/` with
  tests when wording affects safety or recovery.
- Reusable visual controls belong in `ui/components/`; one-off screen pieces can
  stay as private composables inside that screen file.

## ViewModel Pattern

Follow the current ViewModel style:

- Constructor-inject repositories/scanners/settings repositories.
- Provide an inner `Factory : ViewModelProvider.Factory` for Compose `viewModel`.
- Keep mutable state private (`MutableStateFlow`) and expose `StateFlow`.
- Use `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`
  for observed repository/settings flows.
- Use sealed UI states when a screen has distinct loading, empty, and active
  states. `HomeUiState.Loading`, `Empty`, and `Active` are the local example.

Avoid:

- Putting business rules in composables.
- Adding Android dependencies to `core`.
- Reading or writing Room/DataStore directly from screens.
- Creating a second dependency container before deciding whether
  `SnapSortDependencies.kt` should be evolved.

