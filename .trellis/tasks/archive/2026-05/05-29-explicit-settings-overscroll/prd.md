# Explicit settings overscroll

## Goal

Make the settings screen's main scroll container use an explicit Compose overscroll effect so vertical edge feedback is visible and consistent instead of relying on weak or device-specific defaults.

## What I already know

* The settings screen body is a `LazyColumn` in `app/src/main/java/com/snapsort/app/ui/settings/SettingsScreen.kt`.
* The current implementation does not explicitly configure overscroll on that list.
* Repo-wide search did not find any global overscroll disable/customization code.
* This task came from user feedback that the settings page does not show Android's elastic edge feedback when scrolling past the bounds.

## Assumptions (temporary)

* Only the settings screen needs to change in this task.
* Using Compose's built-in overscroll APIs is preferred over custom physics or view interop.
* The expected behavior should remain compatible with the existing restrained visual style.

## Open Questions

* None at the moment.

## Requirements

* Explicitly attach an overscroll effect to the settings screen's main `LazyColumn`.
* Keep the change scoped to the settings page.
* Do not change unrelated layout, copy, or gesture behavior.

## Acceptance Criteria

* [ ] `SettingsScreen` no longer relies on implicit default overscroll behavior for its main list.
* [ ] The screen still compiles with the app's current Compose and Material versions.
* [ ] The diff stays limited to the settings implementation unless verification requires a related adjustment.

## Definition of Done (team quality bar)

* Relevant Kotlin code updated
* `:app:compileDebugKotlin` passes
* Specs reviewed for update need

## Out of Scope (explicit)

* Enabling overscroll on other screens
* Redesigning settings page layout or theming
* Adding custom bounce animations beyond Compose's built-in effect

## Technical Notes

* Primary file: `app/src/main/java/com/snapsort/app/ui/settings/SettingsScreen.kt`
* Verification target: `./gradlew :app:compileDebugKotlin`
