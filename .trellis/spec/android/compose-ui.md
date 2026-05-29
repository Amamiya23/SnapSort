# Compose UI

## Product Direction

Follow the design context in `AGENTS.md`:

- Restrained, refined, efficient.
- Photo-first and low-anxiety.
- Similar in restraint to Apple Photos, not a flashy gallery or aggressive
  storage cleaner.
- Accent color communicates primary action, current state, danger, or progress;
  it is not decoration.
- Destructive actions must be clear, recoverable, and not color-only.

If source code and `AGENTS.md` disagree on a broad visual direction, stop and
confirm before reshaping the interface.

## Screen Structure

Current screen pattern:

- Public screen composable at the top of the file.
- Default `viewModel(...)` factory pulls dependencies from
  `SnapSortDependencies`.
- State is collected with `collectAsState`.
- Navigation and Android activity result launchers stay in
  `SnapSortNavigation.kt`.
- Screen-local UI pieces are private composables in the same file unless reused.

Reference files:

- `HomeScreen.kt`
- `ScanProgressScreen.kt`
- `PhotoSelectionScreen.kt`
- `SettingsScreen.kt`
- `DeleteResultScreen.kt`

Use `Scaffold` with `TopAppBar` for full screens. Keep loading states explicit;
`HomeUiState.Loading` intentionally prevents a flash of empty state during the
initial Room query.

## Navigation

`SnapSortNavigation.kt` owns routes and app-level launchers:

- Home route is `home`.
- Group selection route is `group_selection/{groupId}`.
- Folder selection uses `OpenDocumentTree`.
- Delete authorization uses `StartIntentSenderForResult`.

The app currently disables NavHost enter/exit/pop transitions. Do not re-add
route animations unless the task explicitly asks for motion and verifies that
the photo workflow still feels controlled.

## Theme And Visual System

Material 3 theme lives in `ui/theme`:

- `SnapSortTheme` chooses light/dark color schemes or Android dynamic color
  when the user selects dynamic mode.
- `SnapSortDefaultDynamicColor` is `false`; dynamic color requires opt-in and
  Android S or newer.
- The default palette is slate/charcoal with red reserved for destructive state.

Reference tests:

- `ThemeColorPolicyTest.kt`

Avoid generic purple themes, decorative gradients, and ornament-heavy UI.

## Copy And Localization

User-facing copy is currently Chinese. Reusable or safety-sensitive strings
belong in `ui/copy` with unit tests:

- Delete confirmation/result copy: `DeleteCopy.kt`, `DeleteCopyTest.kt`.
- Settings labels/descriptions: `SettingsCopy.kt`, `SettingsCopyTest.kt`.
- Local time formatting: `TimeCopy.kt`, `TimeCopyTest.kt`.

Keep destructive copy specific:

- Say whether the system confirmation is next.
- Say whether canceling keeps marks.
- Say whether failed files remain marked.
- Avoid words implying permanent deletion when Android moves items through the
  platform delete flow.

## Photo Review Interaction

Photo selection uses `ZoomableImage.kt` and `PhotoSelectionScreen.kt`:

- Coil loads the original image size through `Size.ORIGINAL`.
- Hardware bitmaps are disabled for smooth `graphicsLayer` zoom/pan.
- Double-tap restores a zoomed image; it does not zoom in from 1x.
- Vertical swipe progress follows the finger with damped translation and a
  bottom-edge progress indicator.
- Explicit buttons and final delete confirmation remain part of the workflow.

Do not make hidden gestures the only way to mark, unmark, or delete.

## Accessibility

Follow the current accessibility patterns:

- Interactive icons have `contentDescription`.
- Radio-like rows use `selectable(..., role = Role.RadioButton)`.
- Deletion state is communicated with text/counts, not color alone.
- Text should tolerate Android font scaling and avoid overlapping controls.
- Keep screen-reader labels aligned with the action, especially for back,
  settings, folder selection, delete, and result actions.

