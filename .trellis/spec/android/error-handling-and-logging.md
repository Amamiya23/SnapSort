# Error Handling And Logging

## Error Model

SnapSort does not currently use a global error type hierarchy. Failure handling
is local to the workflow:

- Scanning emits `ScanEvent.Failed(message)` for unrecoverable folder access.
- Scan progress UI stores `errorMessage` in `ScanProgressUiState`.
- Deletion stores pending intent, error message, result, and snackbar message in
  `DeleteAuthorizationState`.
- Delete failures are structured as `DeleteFailure(fileName, reason)`.

Use structured local state when adding user-recoverable failures. Keep raw
exceptions at Android IO boundaries and convert them into user-facing state or
fallback behavior.

## Fallback Rules

Existing fallback patterns:

- `FolderScanner.queryFolderFiles` tries `DocumentsContract` first and falls
  back to `DocumentFile.listFiles()` on query failure.
- `FolderScanner.readCaptureTime` tries EXIF `TAG_DATETIME_ORIGINAL` and falls
  back to file modified time when EXIF is missing or unreadable.
- `DeleteViewModel.prepareDeleteRequest` tries MediaStore delete request
  creation first and falls back to SAF deletion when request creation fails.
- `DeleteViewModel.calibrateAfterDelete` verifies actual deletion by checking
  whether each URI can still be opened.

Catch broad exceptions only at these kinds of Android IO boundaries, where the
next step is an explicit fallback or user-facing failure. Do not hide exceptions
inside pure `core` logic.

## Coroutine Dispatchers

Use the current dispatcher split:

- ContentResolver, SAF, EXIF, and delete calibration work: `Dispatchers.IO`.
- CPU-only mapping/grouping and UI list derivation: `Dispatchers.Default` when
  work is potentially non-trivial.
- ViewModel state changes are launched from `viewModelScope`.

Reference files:

- `FolderScanner.kt`
- `ScanProgressViewModel.kt`
- `HomeViewModel.kt`
- `DeleteViewModel.kt`

## Logging

Logging is sparse and Android-local:

- `DeleteViewModel` uses `android.util.Log`.
- Tag pattern: a private companion constant, currently `SnapSortDelete`.
- Existing logs are for technical delete fallback/failure paths, not normal
  user actions.

When adding logs:

- Log only information needed to diagnose Android platform or SAF behavior.
- Prefer counts, file names, stage names, and exception objects over noisy
  lifecycle logs.
- Be careful with media URIs and paths. Do not add broad logging of folder
  contents or user photo metadata.

## User-Facing Messages

Keep user-facing failures short, actionable, and consistent with the workflow:

- Scanning failure should explain what cannot be read.
- Delete cancellation should say deletion was canceled.
- Delete partial failure should explain that failed files remain marked and can
  be retried or unmarked.

Tests protect several safety-sensitive messages in `DeleteCopyTest.kt` and
`SettingsCopyTest.kt`. Add or update tests when changing those contracts.

