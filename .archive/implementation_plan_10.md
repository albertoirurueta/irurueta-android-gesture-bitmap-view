# Implementation plan — Skip emulator-flaky gesture tests with @RequiresRealDevice

## Task summary

Two instrumented tests in `GestureBitmapViewTest` fail intermittently on the CI API 35 `google_apis` x86_64 emulator
with `IllegalArgumentException: Targeted input event injection ... was not directed at a window owned by uid ...`,
because a system window can overlap the activity during `InstrumentationTestHelper.drag` / `pinch`. The failure is
emulator flakiness, not a code regression. The sibling gesture tests (`doubleTapGesture_modifiesScaleAndNotifies`,
`scrollAndFlingGestures_whenReachesBounds_notifies`, `rotateGesture_modifiesRotationAngle`) already carry
`@RequiresRealDevice`; these two were missed.

Approach chosen: annotate both tests with `@RequiresRealDevice`, as the issue specifies. Retrying injection or
dismissing system dialogs is not reliable because the overlapping window is outside the test's control.

Source: GitHub issue #10
Base branch: develop

Open clarifications: none

## Acceptance criteria coverage

| # | Criterion | Covered by tasks |
|---|---|---|
| 1 | "Both tests are annotated with `@RequiresRealDevice` (from `com.irurueta.android.testutils.RequiresRealDevice`)." | Task 1, Task 2 |
| 2 | "The `Develop` workflow passes `:lib:connectedDebugAndroidTest` on the emulator, with both tests reported as skipped." | Task 3 |
| 3 | "Both tests still pass when run on a real device." | Task 3 |

## Current code state

- File: `lib/src/androidTest/java/com/irurueta/android/gesturebitmap/test/GestureBitmapViewTest.kt`
  (Android library `lib/`, View-based `GestureBitmapView`, Gradle Kotlin DSL).
- `ConditionalIgnoreRule` (line 28, rule at line 46) and the `RequiresRealDevice` import (line 30) are already present.
- `@RequiresRealDevice` is already used on other tests (lines 1081, 1155, 1402), placed directly above `@Test`.
- `dragIntoOverscrollMargin_whenRepeated_returnsToLimitsEveryTime` is at ~line 1285 and `pinchGesture_modifiesScale`
  at ~line 1339, both currently with only `@Test`.

## Implementation steps

### Group 1 — Annotate the flaky tests (Parallelizable: yes)

- [x] Task 1. Annotate `dragIntoOverscrollMargin_whenRepeated_returnsToLimitsEveryTime` _(android)_
  - Add `@RequiresRealDevice` on the line above `@Test` in `GestureBitmapViewTest.kt`:
    ```kotlin
    @RequiresRealDevice
    @Test
    fun dragIntoOverscrollMargin_whenRepeated_returnsToLimitsEveryTime() {
    ```
- [x] Task 2. Annotate `pinchGesture_modifiesScale` _(android)_
  - Add `@RequiresRealDevice` on the line above `@Test` in `GestureBitmapViewTest.kt`:
    ```kotlin
    @RequiresRealDevice
    @Test
    fun pinchGesture_modifiesScale() {
    ```
- [x] Task 3. Verify _(android)_
  - [x] Task 3.1. Compile the androidTest sources (`./gradlew :lib:compileDebugAndroidTestKotlin`), delegated to the
    `iru-gate-runner` agent so build output stays out of the main context. _(Compiled OK, exit 0.)_
  - [x] Task 3.2. Confirm the two tests are skipped on the emulator in the `Develop` workflow, and that they still
    pass on a real device (manual / CI check; no emulator or device is run by this plan).
    _(Deferred by user decision: verified by the pull request's CI run and the reviewer's real-device check, not
    locally.)_
  - No docs update is needed (test-only change).
