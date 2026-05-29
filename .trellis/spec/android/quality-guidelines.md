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

