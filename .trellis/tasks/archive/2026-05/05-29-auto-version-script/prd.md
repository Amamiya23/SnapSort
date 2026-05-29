# Add automatic app version script

## Goal

Add a small build helper so SnapSort can package APKs with an automatically generated Android version code and version name, avoiding manual edits to `app/build.gradle.kts` before each package build.

## What I already know

* Current Android version metadata is hard-coded in `app/build.gradle.kts`:
  * `versionCode = 3`
  * `versionName = "v2.0"`
* The repository does not include `gradlew`; README build commands use the local Gradle binary at `/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle`.
* The likely packaging tasks are `:app:assembleDebug` and `:app:assembleRelease`.

## Assumptions

* The script should avoid rewriting `app/build.gradle.kts` on every build.
* Automatically generated `versionCode` should be monotonic for repeated local package builds.
* Automatically generated `versionName` should be readable and can be overridden when a named release is needed.

## Requirements

* Gradle should accept version metadata from project properties:
  * `VERSION_CODE`
  * `VERSION_NAME`
* Defaults in `app/build.gradle.kts` should remain usable when building without the helper script.
* Add a script that packages the app and passes generated version metadata to Gradle.
* The script should support release builds by default and debug builds when requested.
* The script should print the version code/name it is building with.
* Document the new command in README.

## Acceptance Criteria

* [x] Running the script triggers the expected Gradle assemble task with generated version metadata.
* [x] `app/build.gradle.kts` still compiles when no version properties are passed.
* [x] The README explains how to build with automatic versioning and how to override the version name.

## Definition of Done

* Tests added/updated where behavior is covered by project code.
* Kotlin compile check passes for changed Gradle configuration.
* Docs updated for the new script.

## Out of Scope

* Publishing to Play Store or any app distribution service.
* Changing signing key handling.
* Adding CI release automation.

## Technical Notes

* Files inspected:
  * `app/build.gradle.kts`
  * `README.md`
  * root build/settings files
* Implementation should be shell-script level and avoid new dependencies.
* Verified `scripts/build-apk.sh --debug --name vtest-auto-version` writes generated version metadata into debug manifests.
* Verified `scripts/build-apk.sh --release --name vtest-auto-version-release` writes generated version metadata into release manifests and produces `app/build/outputs/apk/release/app-release.apk`.
