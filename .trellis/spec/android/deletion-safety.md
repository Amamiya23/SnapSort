# Deletion Safety

Deletion is the highest-risk workflow in SnapSort. Changes here must preserve
clear impact, explicit confirmation, and recoverable failure behavior.

## Mark Before Delete

Photos are marked for deletion in Room before any file deletion happens:

- Selection toggles call `TaskRepository.setMarkedForDeletion`.
- `PhotoEntity.markedForDeletion` stores the mark.
- `TaskRepository.getDeleteCandidates` returns marked JPG files and optional
  same-name RAW matches as `DeleteCandidate`.

Do not infer deletion candidates directly from the UI list. Use the repository
so JPG/RAW pairing and current marks remain consistent.

## Confirmation Requirements

Before starting deletion, the UI must show:

- JPG/JPEG count.
- RAW count.
- Total file count.
- An expandable file list.
- Copy that says Android will perform the system confirmation and that canceling
  confirmation does not change marks.

Current reference files:

- `HomeScreen.kt` gathers preview files through
  `HomeViewModel.getDeletePreviewFiles`.
- `DeleteConfirmationSheet.kt` displays counts and the expandable file list.
- `DeleteCopy.kt` owns the confirmation and result wording.
- `DeleteCopyTest.kt` protects safety-sensitive copy.

Avoid:

- "Clean up" or fear-based language.
- Vague destructive confirmations.
- Default-all-delete behavior.
- Hiding RAW impact behind only a count when the file list is available.

## Delete Execution

`DeleteViewModel` owns delete execution:

- `prepareDeleteRequest` loads candidates on `Dispatchers.IO`.
- It builds a URI list containing each marked JPG and its RAW match when
  available.
- It calls `MediaStore.createDeleteRequest` first, so Android presents system
  confirmation.
- If MediaStore request creation fails, it falls back to SAF deletion through
  `DocumentFile.fromSingleUri`.
- `onDeleteAccepted` calibrates by checking whether each URI can still be
  opened.
- `onDeleteRejected` keeps marks and shows a cancellation message.

Only deleted JPG URIs are passed to `TaskRepository.removeDeletedPhotos`.
That keeps failed JPG entries marked and visible for retry or manual unmarking.
RAW files are counted in success/failure results but do not independently key
database rows.

## Recovery Behavior

After deletion:

- Successfully deleted JPGs are removed from Room.
- Affected groups get a new cover photo if one remains.
- Empty groups are deleted.
- The recent task is deleted when no photos remain.
- Failures remain marked so the user can retry or cancel those marks.

User-facing result copy should continue to explain that failed items remain
marked. Keep this covered by tests in `DeleteCopyTest.kt`.

## Folder Permission

Folder selection uses `ActivityResultContracts.OpenDocumentTree` in
`SnapSortNavigation.kt` and immediately persists read/write URI permission with
`takePersistableUriPermission`.

Do not move folder permission handling into individual screens unless the app
navigation model changes. The app shell owns activity result launchers and
routes into scan/delete flows.

