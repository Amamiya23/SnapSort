# GitHub Release Update Check Design

## Architecture

- Add a small update-check data component under `data/update/`.
- Inject it through `SnapSortDependencies`, matching the existing lightweight service-locator pattern.
- Extend `SettingsViewModel` to own update UI state and call the update repository from `viewModelScope`.
- Keep composables passive: Settings renders state, triggers `checkForUpdates()`, and launches a browser intent for the release page.

## GitHub API

- Endpoint: `https://api.github.com/repos/Amamiya23/SnapSort/releases/latest`.
- Headers:
  - `Accept: application/vnd.github+json`
  - `X-GitHub-Api-Version: 2026-03-10`
- Use no token because the target repository is public.
- Parse only the small field set needed by the app:
  - `tag_name`
  - `html_url`
  - `name`
  - `draft`
  - `prerelease`
  - `assets`
- Latest release endpoint should already exclude draft releases. The repository will still reject malformed or prerelease-only responses if needed by local policy.

## Version Comparison

- Primary comparison uses `BuildConfig.VERSION_CODE` vs a numeric value inferred from the release:
  1. Prefer a positive integer asset/version metadata only if such metadata is added later.
  2. For this task, infer a comparable release code from `tag_name` when it matches SnapSort release naming:
     - `vYYYY.MM.DD-HHMM` -> `YYYYMMDDHHMM`
     - plain integer tag -> integer
     - semantic tags like `v2.1` cannot be reliably ordered against timestamp `versionCode`; treat them as update candidates when tag differs from installed `VERSION_NAME`.
- Installed timestamp `versionCode` values are Unix seconds, while `vYYYY.MM.DD-HHMM` is not numerically equivalent. To avoid false "latest" decisions, comparison falls back to tag equality when no reliable numeric code exists.
- Unit tests cover timestamp tags, semantic tags, equal tags, malformed tags, and newer/older numeric codes.

## UI State

`SettingsViewModel` exposes an update state:

- `Idle(currentVersionName)`
- `Checking(currentVersionName)`
- `UpToDate(currentVersionName)`
- `Available(currentVersionName, latestVersionName, releaseUrl)`
- `Failed(currentVersionName, message)`

Settings adds an "应用更新" section:

- Row title: `检查更新`
- Description/value changes with state.
- A separate text button appears when an update is available: `前往下载`

## Failure Handling

- Network and JSON parsing run on `Dispatchers.IO`.
- Repository returns a sealed result instead of throwing into UI.
- User-facing errors stay short and actionable.
- Browser launch failures are converted into a visible Settings failure message.

## Compatibility

- Add `android.permission.INTERNET` to the manifest.
- No database or DataStore migration.
- No new Gradle dependency: use `HttpURLConnection` and `org.json.JSONObject` from platform APIs.

## Rollback

- Remove the Settings section, `SettingsViewModel` update state, `data/update` package, manifest permission, and dependency provider.
