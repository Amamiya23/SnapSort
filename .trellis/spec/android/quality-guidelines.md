# Quality Guidelines

## Verification Commands

This repository currently has no checked-in `gradlew`. Use the local Gradle
binary documented in `README.md`:

```bash
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:testDebugUnitTest
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:compileDebugKotlin
```

For release/build verification:

```bash
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:assembleDebug
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:assembleRelease
```

Release signing reads `keystore.properties`, which is intentionally local and
not committed.

## Build Versioning Command

### 1. Scope / Trigger

Use this contract when changing SnapSort APK packaging commands, Android
`versionCode`, Android `versionName`, or local release helpers. The goal is to
avoid manual Gradle edits while keeping direct Gradle builds usable.

### 2. Signatures

```bash
scripts/build-apk.sh [--release|--debug] [--name VERSION_NAME]
VERSION_CODE=<positive-int> VERSION_NAME=<name> GRADLE_BIN=<path> scripts/build-apk.sh
```

Gradle accepts the matching project properties:

```bash
-PVERSION_CODE=<positive-int>
-PVERSION_NAME=<name>
```

### 3. Contracts

- `--release` builds `:app:assembleRelease`; this is the default.
- `--debug` builds `:app:assembleDebug`.
- `--name` overrides generated `versionName`.
- `VERSION_CODE` overrides generated `versionCode`.
- `VERSION_NAME` overrides generated `versionName`.
- `GRADLE_BIN` overrides Gradle executable discovery.
- Direct Gradle builds without project properties must keep using the
  defaults in `app/build.gradle.kts`.

### 4. Validation & Error Matrix

- Missing `--name` value -> exit with usage error.
- Unknown option -> exit with usage error.
- Non-integer, zero, negative, or oversized `VERSION_CODE` -> exit before
  invoking Gradle.
- Missing Gradle executable -> exit with a clear `GRADLE_BIN` instruction.
- Invalid `-PVERSION_CODE` passed directly to Gradle -> fail during Gradle
  configuration with a positive-integer error.

### 5. Good/Base/Bad Cases

- Good: `scripts/build-apk.sh --name v2.1` builds release with generated
  `versionCode` and explicit `versionName`.
- Base: `scripts/build-apk.sh --debug` builds debug with generated metadata.
- Bad: manually editing `versionCode` and `versionName` in
  `app/build.gradle.kts` for every local package build.

### 6. Tests Required

- Run `bash -n scripts/build-apk.sh` after script edits.
- Run `:app:compileDebugKotlin` after Gradle Kotlin DSL edits.
- Run `scripts/build-apk.sh --debug --name <test-name>` and inspect generated
  debug manifest or output metadata for injected version fields.
- For release-helper changes, run `scripts/build-apk.sh --release --name
  <test-name>` and confirm `app/build/outputs/apk/release/app-release.apk`
  exists.

### 7. Wrong vs Correct

#### Wrong

```kotlin
versionCode = 4
versionName = "v2.1"
```

#### Correct

```kotlin
versionCode = appVersionCode.get()
versionName = appVersionName.get()
```

## Test Expectations

Unit tests are JUnit4 tests under `app/src/test/java`.

Add or update tests for:

- Pure grouping/sorting/time-bucket behavior in `PhotoGrouperTest.kt`.
- JPG/RAW extension or matching behavior in `RawMatcherTest.kt`.
- Destructive or recovery copy in `DeleteCopyTest.kt`.
- Settings labels and safety descriptions in `SettingsCopyTest.kt`.
- Time formatting behavior in `TimeCopyTest.kt`.
- Theme policy changes in `ThemeColorPolicyTest.kt`.

For time-zone-sensitive logic, follow `PhotoGrouperTest.kt` and
`TimeCopyTest.kt`: set the default `TimeZone` in `@Before` and restore it in
`@After`.

## Code Review Checklist

Before finishing a change, check:

- Does the code follow the package boundaries in `architecture.md`?
- Did pure domain logic stay in `core` and remain Android-free?
- Did any Room entity change include version/migration/schema consideration?
- Did any enum persisted by `.name` keep compatibility?
- Does deletion UI still show JPG count, RAW count, total count, and file list?
- Do failed delete items remain marked?
- Are user-facing copy changes covered by tests when they affect safety,
  settings semantics, or recovery?
- Are ContentResolver, SAF, EXIF, and delete operations off the main thread?
- Did you preserve existing user work in the git tree?

## Common Risk Areas

- Changing grouping sort order can alter task review order and delete marks.
  Update `PhotoGrouperTest.kt`.
- Changing RAW extension support can alter delete impact. Update
  `RawMatcherTest.kt` and re-check delete preview counts.
- Changing settings defaults can affect scan behavior immediately on first app
  launch. Update copy/tests and migration compatibility if needed.
- Changing delete calibration can either remove undeleted photos from Room or
  leave deleted photos visible. Review `DeleteViewModel.kt` and
  `TaskRepository.removeDeletedPhotos` together.
- Changing navigation transitions can make photo review feel less controlled.
  Keep transitions disabled unless motion is the explicit feature.

## Style

- Prefer small data classes, sealed UI states, and pure helper functions.
- Keep comments sparse and useful; do not narrate obvious assignments.
- Keep reusable copy in `ui/copy`.
- Use Material icons already available in the project for Compose actions.
- Avoid adding new dependencies unless the task clearly needs them and the
  Gradle change is verified.
