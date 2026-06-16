# GitHub release update check

## Goal

Add a manual update-check entry to SnapSort so users can check the latest GitHub Release from inside the app and open the release page to download the APK when a newer version exists.

## Confirmed Facts

- The app is a single-module Android Compose app.
- User-facing copy is Chinese.
- Settings UI already uses section headers and row-based controls.
- The git remote is `Amamiya23/SnapSort`.
- The app currently has no network layer and no `INTERNET` permission.
- App version metadata comes from Android `BuildConfig.VERSION_CODE` / `VERSION_NAME`; release builds normally use timestamp-like `versionCode` and `v...` `versionName`.
- GitHub REST API supports `GET /repos/{owner}/{repo}/releases/latest` for the latest published release. Public resources can be accessed without authentication. The response includes `tag_name`, `html_url`, `draft`, `prerelease`, and `assets`.

## Requirements

- Add a settings entry for checking updates manually.
- Fetch the latest published GitHub Release for `Amamiya23/SnapSort`.
- Compare the latest release with the installed app using version metadata that remains reliable for timestamp-style release builds.
- Show clear, non-alarming status copy for:
  - idle / current app version
  - checking
  - already latest
  - update available
  - network/API failure
- When an update is available, provide an explicit action that opens the GitHub release page in the browser for download.
- Do not download, install, or request package-install permissions inside SnapSort.
- Do not require GitHub authentication or collect credentials.
- Keep failures recoverable: the user can retry from Settings.

## Acceptance Criteria

- [x] Settings shows an update-check row with the installed version.
- [x] Tapping the row starts a check and shows an in-progress state.
- [x] If GitHub returns a newer release, Settings shows the release tag and an action to open the release page.
- [x] Opening the update action launches the release `html_url` with an Android browser intent.
- [x] If the installed build is current or newer, Settings says it is already up to date.
- [x] Network errors, malformed responses, HTTP errors, and missing browser handlers do not crash the app.
- [x] Version comparison is covered by unit tests.
- [x] The app compiles with the new network permission and code.

## Out Of Scope

- Automatic background update checks.
- In-app APK download, APK signature validation, or installation flow.
- GitHub API authentication, release notes rendering, or asset selection UI.
