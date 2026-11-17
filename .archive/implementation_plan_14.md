# Implementation plan — Update dependencies

## Task summary

Bump two dependencies declared in the Gradle version catalog:

- `irurueta-android-test-utils` 1.3.2 -> 1.3.3
- `irurueta-geometry` 1.5.0 -> 1.6.1

Approach chosen: change only the catalog versions, then build and run the unit tests. Source changes are made only if the
geometry bump breaks compilation or behavior. This is a plain version bump, so no new tests are needed.

Source: GitHub issue #14
Base branch: develop

Open clarifications: none

## Acceptance criteria coverage

| # | Criterion | Covered by tasks |
|---|---|---|
| 1 | "Update dependency of irurueta-android-test-utils to version 1.3.3" | Task 1.1 |
| 2 | "Update dependency of irurueta-geometry to version 1.6.1" | Task 1.2 |
| 3 | derived: the project compiles and its unit tests pass with the new versions | Task 2 |
| 4 | derived: no stale references to the old versions remain in docs or README | Task 3 |

## Current code state

- Project type: android-library (`lib/`) with a sample app (`app/`). Gradle Kotlin DSL, wrapper, version catalog.
- `gradle/libs.versions.toml` holds `iruruetaGeometry = "1.5.0"` and `iruruetaTestUtils = "1.3.2"`. The libraries
  `irurueta-geometry` and `irurueta-test-utils` reference them via `version.ref`.
- `com.irurueta.*` is used in `lib/src/main/java/com/irurueta/android/gesturebitmap/GestureBitmapView.kt` and in the
  unit tests (`lib/src/test/...`) and instrumented tests (`lib/src/androidTest/...`). A geometry API change could
  surface there.
- No hard-coded references to the old versions were found in docs or README. This is re-checked in Task 3.

## Implementation steps

### Group 1 — Bump versions (Parallelizable: yes)

- [x] Task 1. Update the version catalog _(android)_
  - [x] Task 1.1. In `gradle/libs.versions.toml` set `iruruetaTestUtils = "1.3.3"`
  - [x] Task 1.2. In `gradle/libs.versions.toml` set `iruruetaGeometry = "1.6.1"`
  - Version bump only (gradle/libs.versions.toml); no new tests, no Kotlin/KDoc changes; build and unit tests deferred to Group 2.

### Group 2 — Verify (Parallelizable: no — depends on Group 1)

- [x] Task 2. Build and run unit tests _(android)_
  - [x] Task 2.1. Compile `lib` and `app`. If 1.6.1 introduced breaking API changes, adapt
    `GestureBitmapView.kt` and the affected tests minimally.
  - [x] Task 2.2. Run the unit tests via `iru-gate-runner` (`iru-android-test`) and confirm they pass. - 219/219 :lib unit tests pass under JDK 21 (JDK 17 fails with UnsupportedClassVersionError, class file 65, from dependency); no source changes needed; lib/app/unit/androidTest sources compile clean.

### Group 3 — Docs and changelog (Parallelizable: no — follows verification)

- [x] Task 3. Check docs references
  - [x] Task 3.1. `grep` README.md and `docs/` for the old versions and update any matches. Add a CHANGELOG.md
    entry under the unreleased section if the repository convention does so.
    - Done: no old-version references in README/docs; added CHANGELOG.md entry under Unreleased > Changed. No code, no tests.
