# Implementation plan — Performance improvements

## Task summary

`GestureBitmapView` sometimes shows lag (frames arriving late) and sudden movements (single-frame jumps) while
dragging, zooming or rotating. The issue asks to find the causes in the code and fix them while keeping the public
API and the saved-state format.

Source: GitHub issue #7
Base branch: develop

Work started before this plan was written: Groups 1 to 4 below are already implemented, uncommitted, on branch
`feature/7-performance-improvements`. They are recorded as done so that execution resumes at Group 5. They were
implemented outside the `iru-code` pipeline, so Group 5 re-validates them with the repository's gates.

Decisions taken with the user:

- Fling keeps the existing fixed-duration `ValueAnimator`, with its distance halved so that it starts at the finger
  velocity. `OverScroller` is not introduced (smaller change, existing callbacks and tests keep their timing).
- `setDrawable` is not changed. Large bitmaps are handled by documentation (`drawable-nodpi`, decoding at display
  size) and by enabling mipmaps. No size cap inside the library, so `DisplayType.NONE` behaviour is unchanged.
- Instrumented tests run on the `Pixel_9_Pro` emulator. Frame-timing measurement on a physical device is a manual
  follow-up recorded in the pull request, not part of this plan's automated work.

Decisions taken on the user's behalf:

- No UI decomposition or screenshot matrix is planned: the change alters gesture behaviour and timing of one existing
  component and adds no screen, component or visual state.
- Optional clean-ups from the issue are left out: applying translate/scale/rotate directly on the matrix (no
  measurable gain) and bound-reached notifications (they already fire only on the transition into a bound).

Open clarifications: none

## Acceptance criteria coverage

| # | Criterion | Covered by tasks |
|---|---|---|
| 1 | "No `postInvalidate()` calls from the UI thread and no allocations in `onDraw`." | Task 1.1, Task 1.2 |
| 2 | "Dragging, pinching and rotating the sample image show no single-frame position jumps, including when zooming out past the view size and when lifting or adding fingers." | Task 2.1, Task 2.2, Task 4.1, Task 5.1, Task 6 |
| 3 | "Releasing the bitmap inside the overscroll margin always animates back, on every gesture." | Task 2.3, Task 5.1 |
| 4 | "Fling continues at the finger speed with no visible kick." | Task 3.1, Task 5.1 |
| 5 | "Frame timing for drag and pinch in the sample app improves against the baseline of step 0, with the numbers reported in the PR." | Task 9 (deferred by user decision: recorded in the PR as a manual follow-up on a physical device) |
| 6 | "Existing public API and saved-state format are kept." | Task 7.4 |

## Current code state

Android library, Kotlin, Gradle Kotlin DSL. `lib/` is the library (`com.android.library`), `app/` is its sample.

- `lib/src/main/java/com/irurueta/android/gesturebitmap/GestureBitmapView.kt`: custom `View`. Holds `baseMatrix`,
  `paramsMatrix` and `displayMatrix`; dispatches touch events to `GestureDetector`, `ScaleGestureDetector` and
  `RotationGestureDetector`; limits scroll in `limitScroll`; animates with `translateAnimator` and `scaleAnimator`.
- `lib/src/main/java/com/irurueta/android/gesturebitmap/RotationGestureDetector.kt`: tracks two pointers and reports
  the angle variation between consecutive move events.
- `lib/src/main/java/com/irurueta/android/gesturebitmap/MetricTransformationParameters.kt`: converts a `Matrix` to
  and from scale, rotation and translation. Not changed.
- Unit tests (Robolectric, MockK): `lib/src/test/java/com/irurueta/android/gesturebitmap/GestureBitmapViewTest.kt`
  and `RotationGestureDetectorTest.kt`. Instrumented tests:
  `lib/src/androidTest/java/com/irurueta/android/gesturebitmap/test/GestureBitmapViewTest.kt`.
- Docs: Antora site under `docs/modules/ROOT/pages` (`gestures.adoc`, `usage.adoc`, `sample-app.adoc`, `ui/`).
- `.ui-quality.yml`: `accessibility: enforce`.

State on the branch after Groups 1 to 4 (uncommitted):

- `GestureBitmapView` uses `invalidate()`, draws with the `displayMatrix` field, enables mipmaps in `setBitmap`,
  computes scroll limits from the current displayed rectangle (`minAllowedStart`, `maxAllowedStart`, `limitDelta`,
  with a `snap` flag), runs `limitScaleAndTranslation()` on `ACTION_UP` and `ACTION_CANCEL`, and uses one scale
  animator that also restores rotation and translation when returning to minimum scale.
- Removed private members: `startRect*` fields, `updateStartDisplayedRectangleCoordinates`,
  `rotateAndTranslateAnimator`, `smoothRotateAndTranslateTo`, `updateRotationAndTranslation`.
- 154 unit tests pass and Android Lint reports no issues. Instrumented tests compile but have not been run.

## Implementation steps

### Group 1 — Rendering path

Parallelizable: yes

- [x] Task 1. Remove deferred redraws and per-frame allocations in `GestureBitmapView` _(android)_
  - [x] Task 1.1. Replace every `postInvalidate()` with `invalidate()` in `GestureBitmapView.kt`
  - [x] Task 1.2. Draw with the private `displayMatrix` field in `onDraw` instead of the copying
        `displayTransformationMatrix` getter
  - [x] Task 1.3. Call `bitmap.setHasMipMap(true)` in `setBitmap` for non-recycled bitmaps; stub it in the unit
        tests' bitmap mocks
  - [x] Task 1.4. Move `app/src/main/res/drawable/image.jpeg` to `app/src/main/res/drawable-nodpi/image.jpeg`

### Group 2 — Scroll limiting and release

Parallelizable: no — Task 2 edits the same methods and test file as Task 1

- [x] Task 2. Rework scroll limiting in `GestureBitmapView` _(android)_
  - [x] Task 2.1. Compute limits per axis from the current displayed rectangle (`minAllowedStart`,
        `maxAllowedStart`); remove the `startRect*` fields captured on `ACTION_DOWN`, which also removes the
        `startRectBottom`/`startRectRight` mix-up
  - [x] Task 2.2. Add `limitDelta(current, delta, minValue, maxValue, snap)`: with `snap = false` (finger drag) a
        bitmap outside its limits only moves towards them and never more than requested; with `snap = true`
        (animated scroll) it always ends within limits. Keep `displayMatrix` up to date in `scrollBy`
  - [x] Task 2.3. In `limitScaleAndTranslation`, run the bounce-back whenever no animator is running, skip the
        animation when no correction is needed, and call it on `ACTION_CANCEL` as well as `ACTION_UP`
  - [x] Task 2.4. Unit tests in `GestureBitmapViewTest`: scroll outside limits never moves more than requested;
        bitmap that fits horizontally but not vertically stays horizontally centred; repeated release inside the
        margin always returns to limits (including cancel); release within limits starts no animation

### Group 3 — Animations

Parallelizable: no — Task 3 builds on the limiting from Task 2 and edits the same files

- [x] Task 3. Correct fling and scale animations in `GestureBitmapView` _(android)_
  - [x] Task 3.1. In `onFling`, divide the distance by `FLING_INITIAL_SPEED_FACTOR` (2) so the decelerating
        animation starts at the finger velocity
  - [x] Task 3.2. In `smoothScaleTo`, use `DecelerateInterpolator`; when the target is at or below `minScale`,
        animate scale, rotation and translation together (`updateAllParameters`) and notify
        `OnRotateAndTranslateAnimationCompletedListener` at the end of the same animation
  - [x] Task 3.3. Remove the now unused rotate-and-translate animator and its helpers
  - [x] Task 3.4. Unit tests: fling distance equals velocity × duration / 2; returning to minimum scale resets
        rotation and translation with a single animator and notifies all three listeners once

### Group 4 — Gesture detection

Parallelizable: no — Task 4.3 and Task 4.4 edit `GestureBitmapView.kt` and its test file, like Groups 1 to 3

- [x] Task 4. Make pointer tracking robust _(android)_
  - [x] Task 4.1. `RotationGestureDetector`: ignore fingers beyond the second; on `ACTION_POINTER_UP` find which
        pointer was lifted, promote or replace tracked pointers and restart tracking (`onPointerUp`,
        `startTracking`); guard `findPointerIndex` returning -1; clear both ids on `ACTION_UP`
  - [x] Task 4.2. `RotationGestureDetectorTest`: first finger lifted then new finger down; tracked finger lifted
        with three fingers reports no rotation change; missing pointer on move makes no action
  - [x] Task 4.3. `GestureBitmapView.onScale`: refresh `parameters` from `paramsMatrix` before reading the scale
  - [x] Task 4.4. `GestureBitmapView.onTouchEvent`: call `parent?.requestDisallowInterceptTouchEvent(true)` on
        `ACTION_POINTER_DOWN`, with a unit test using a recording parent

### Group 5 — Instrumented tests

Parallelizable: yes

- [x] Task 5. Align instrumented tests with the new behaviour _(android)_
  - [x] Task 5.1. Review `lib/src/androidTest/java/com/irurueta/android/gesturebitmap/test/GestureBitmapViewTest.kt`
        against the behaviour changes of Groups 2 to 4 (bounce-back on every release, halved fling distance, single
        animation at minimum scale, small bitmaps returning to the centre) and correct any assertion that encodes
        the old behaviour. Do not weaken assertions that still hold
  - [x] Task 5.2. Add one instrumented test that drags the bitmap into the overscroll margin twice in a row and
        checks it returns to its limits both times (regression for the skipped bounce-back)
  - [x] Task 5.3. Run the instrumented tests on the `Pixel_9_Pro` emulator by delegating to the `iru-gate-runner`
        agent: `Agent({description: "Run instrumented tests", subagent_type: "iru-gate-runner", prompt: "Invoke
        Skill({skill: \"iru-android-test\"}) for the lib module's instrumented tests on the Pixel_9_Pro emulator
        ..."})`. If the emulator cannot be started in this environment, report it as not run instead of marking
        the task done
        (Task 5 done: no old-behaviour assertions found; added dragIntoOverscrollMargin_whenRepeated_returnsToLimitsEveryTime; 27/27 instrumented tests passed on Pixel_9_Pro emulator-5554, 3 @RequiresRealDevice tests ran too. Unit gates deferred to Group 7.)

### Group 6 — Manual behaviour check on the emulator

Parallelizable: yes

- [x] Task 6. Exercise the sample app on the `Pixel_9_Pro` emulator _(android)_
  - [x] Task 6.1. Install the debug sample app, then drag, fling, double tap and (where the emulator allows) pinch
        the image; confirm there is no jump when a gesture ends and that release always returns within limits.
        Record what could and could not be exercised (multi-touch rotation is limited on an emulator)
    - Verified on Pixel_9_Pro (emulator-5554): double tap zoom, slow drag, fast flings in all directions and repeated double taps; no position change between gesture end and settled frame, and the image always filled the view after release (no blank gaps). Pinch and rotation NOT exercised (adb input cannot inject multi-pointer events). No source changes.

### Group 7 — Validation of the work done in Groups 1 to 4

Parallelizable: no — gates run once over the whole change, after Groups 5 and 6

- [x] Task 7. Run the repository gates over the full change _(android)_
  - [x] Task 7.1. Unit tests for the `lib` module through the `iru-gate-runner` agent with
        `Skill({skill: "iru-android-test"})`; all tests must pass
  - [x] Task 7.2. Coverage through `iru-gate-runner` with `Skill({skill: "iru-android-coverage"})`; the new and
        changed code in `GestureBitmapView.kt` and `RotationGestureDetector.kt` must not lower coverage against
        `develop`; add unit tests for any new uncovered branch (`limitDelta` out-of-range branches, `onPointerUp`
        with no remaining pointer, `startTracking` with a missing pointer)
  - [x] Task 7.3. Code quality through `iru-gate-runner` with `Skill({skill: "iru-android-code-quality"})`, license
        headers with `Skill({skill: "iru-check-license"})`, and KDoc on every new or changed member
        (`Skill({skill: "iru-kotlin-kdoc"})`); fix what they report
  - [x] Task 7.4. Confirm the public API and saved state are unchanged: no public or protected member of
        `GestureBitmapView`, `RotationGestureDetector` or `MetricTransformationParameters` removed or re-typed
        (compare against `develop`), and `onSaveInstanceState`/`onRestoreInstanceState` keys untouched

### Group 8 — Documentation

Parallelizable: yes

- [x] Task 8. Update documentation
  - [x] Task 8.1. `CHANGELOG.md` Unreleased section, and `gestures.adoc`, `usage.adoc`, `sample-app.adoc` for
        the new limiting, fling, animation and large-bitmap guidance
  - [x] Task 8.2. Run `/iru-update-docs` to reconcile the Antora pages (including the `ui/` section) with the final
        code, then build the site with `/iru-build-docs` and fix any reported error

### Group 9 — Follow-up record

Parallelizable: yes

- [x] Task 9. Record the deferred device measurement
  - [x] Task 9.1. State in the pull request description that frame timing for drag and pinch was not measured on a
        physical device, and list the steps to do it (`adb shell dumpsys gfxinfo <package> framestats` before and
        after, on `develop` and on this branch)
