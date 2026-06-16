# Implementation Plan

## Checklist

1. Add update-check model/repository under `app/src/main/java/com/snapsort/app/data/update/`.
2. Add pure version comparison helper and unit tests under `app/src/test/java/com/snapsort/app/data/update/`.
3. Add `SnapSortDependencies.updateRepository(context)`.
4. Extend `SettingsViewModel` with update state, check action, and release-open failure handling.
5. Update `SettingsScreen` with the "应用更新" section and browser intent launch.
6. Add `INTERNET` permission to `AndroidManifest.xml`.
7. Verify with unit tests and Kotlin compilation.

## Validation

```bash
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:testDebugUnitTest
/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle :app:compileDebugKotlin
```

## Risk Points

- Version tags may not always encode a comparable build code. The comparison helper must be conservative and covered by tests.
- Settings screen already has a sizeable file; keep added UI small and local.
- `HttpURLConnection` must set timeouts and close streams.
- Browser intent launch can fail on unusual devices; Settings must surface a short failure message.
