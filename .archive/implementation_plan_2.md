# Implementation plan — Fling on rotation

## Task summary

When a two-finger rotation gesture ends, `GestureBitmapView` stops rotating immediately, while a fast scroll keeps
moving (fling). The ticket asks for a similar behavior for rotation: if the fingers are released while still rotating,
the bitmap keeps rotating for a short while, decelerating until it stops.

Choices made on the user's behalf (challenge them during review):
- `RotationGestureDetector` measures angular velocity (rad/s) from the last few `ACTION_MOVE` events and notifies the
  end of a rotation gesture (a tracked finger is lifted or the gesture is cancelled) through a new listener method
  with a **default empty implementation**, so existing `OnRotationGestureListener` implementations keep compiling.
- The view animates the remaining rotation with a new `rotateAnimator` using a `DecelerateInterpolator` over
  `animationDurationMillis`, exactly mirroring how scroll fling works (same initial-speed factor
  `FLING_INITIAL_SPEED_FACTOR`, so the animation starts at the velocity of the fingers). The rotation pivot is the last
  pivot (midpoint between fingers) reported by the detector.
- A new boolean `rotationFlingEnabled` (default `true`, like scroll fling) switches the behavior. It is exposed as a
  property, an XML attribute (`app:rotationFlingEnabled`) and persisted in the saved instance state. Fling only applies
  when `rotationEnabled` is true.
- A minimum angular velocity threshold (constant) avoids a fling when the fingers were practically still on release.
- Any new touch (`ACTION_DOWN`), scroll, scale, rotation, programmatic transformation change, or other animation
  cancels the rotation fling (it is part of `cancelAllAnimators()`).
- When the rotation fling ends without being cancelled, the existing limit correction (`limitScaleAndTranslation()`)
  runs so the bitmap is brought back within scroll/scale limits. If the scale is out of its limits on release, the scale
  correction animation takes precedence and cancels the fling (the view must end within limits).

Decisions taken by the user after the first implementation pass:
- **Rotation is kept at minimum zoom** (Group 4): the end-of-gesture limit correction must no longer reset rotation
  to 0 when the bitmap returns to (or is at) the minimum scale, so a rotation fling at the default zoom is not undone.
  Only scale and translation are corrected. Double tap, which is an explicit "jump" gesture, keeps its current
  reset behavior when it cycles back to the minimum scale.
- **Android Lint**: the 16 warnings reported in `app/` and `gradle/libs.versions.toml` (NewerVersionAvailable,
  AndroidGradlePluginVersion, GradleDependency, MonochromeLauncherIcon, ObsoleteSdkInt) all predate this work and are
  left as they are by user decision; they are not a follow-up of this ticket. The quality gate passes when no new
  issue is introduced against that baseline.

Source: GitHub issue #2
Base branch: develop

Open clarifications: none

## Acceptance criteria coverage

| # | Criterion | Covered by tasks |
|---|---|---|
| 1 | "A similar behaviour could be added to rotation. Currently, when rotation finishes, it stops suddenly" | Task 1, Task 2, Task 3 |
| 2 | derived: releasing the fingers while rotating fast keeps rotating with decreasing speed until it stops, in the direction of the gesture | Task 1.2, Task 2.2, Task 2.5 |
| 3 | derived: no fling when rotation is disabled, fling is disabled, or release speed is below the threshold | Task 2.1, Task 2.5 |
| 4 | derived: a new touch or any other transformation/animation cancels the fling, and the bitmap ends within scroll/scale limits | Task 2.3, Task 2.4, Task 2.5 |
| 5 | derived: `rotationFlingEnabled` is configurable by property and XML attribute and survives save/restore | Task 2.1, Task 2.6 |
| 6 | derived: documentation, README and changelog describe the new behavior and option | Task 3, Task 4, Task 6 |
| 7 | derived: (user decision) a rotation fling at minimum zoom is not undone: correcting to the minimum scale at the end of a gesture keeps the rotation, while double tap still resets it | Task 5, Task 6 |

## Current code state

Android library `lib/` (View-based, Kotlin 2.3, Gradle Kotlin DSL + version catalog, Robolectric/mockk unit tests,
instrumented tests under `lib/src/androidTest`); sample app in `app/`. No UI screens/components are added (a behavior
change in an existing custom view), so no UI decomposition or screenshot work is planned.

- `lib/src/main/java/com/irurueta/android/gesturebitmap/RotationGestureDetector.kt`
  - `onTouchEvent`: `ACTION_MOVE` updates `currentAngle`, `deltaAngle = currentAngle - prevAngle`, pivot and calls
    `listener.onRotation(this, deltaAngle, pivotX, pivotY)`. `ACTION_POINTER_UP` → `onPointerUp(event)` (re-tracks
    remaining fingers), `ACTION_UP`/`ACTION_CANCEL` reset pointer ids. No timestamps/velocity are kept.
  - `interface OnRotationGestureListener { fun onRotation(...): Boolean }`.
  - Note: angles come from `atan2`, so `deltaAngle` can jump by ~2π when the finger line crosses ±π. Velocity must use a
    wrapped delta (normalized to `[-π, π]`).
- `lib/src/main/java/com/irurueta/android/gesturebitmap/GestureBitmapView.kt` (~2500 lines)
  - Rotation listener in `init` (~line 1972): `cancelAllAnimators()` → `updateRotation(newRotation, pivotX, pivotY)`.
  - Animators: `scaleAnimator`, `translateAnimator` (fields ~line 113–118); `cancelAllAnimators()` (~1681),
    `isAnyAnimatorRunning()` (~1674), `cancelTranslateAnimator()` / `cancelScaleAnimator()`.
  - Scroll fling: `onFling` (~2060) → `smoothScrollBy(diffX, diffY)` → `smoothTranslateTo` (~1452, `DecelerateInterpolator`,
    `animationDurationMillis`, notifies `scrollAnimationCompletedListener`). `FLING_INITIAL_SPEED_FACTOR = 2.0f`
    (~2204).
  - `onTouchEvent` (~600–655): `ACTION_DOWN` cancels animators; on `ACTION_UP`/`ACTION_CANCEL`, if a gesture changed the
    transformation, `limitScaleAndTranslation()` (~804) runs: scale out of range → `smoothScaleTo`; else if no animator
    is running → `smoothScrollBy(0, 0, skipIfNotNeeded = true)`.
  - Config pattern to copy (from `scrollTensionEnabled`, added in #1): public var (~471), save/restore keys in
    `onSaveInstanceState` (~711) / `onRestoreInstanceState` (~763) with `*_KEY` constants (~2317), XML attribute
    `lib/src/main/res/values/attrs.xml` (line ~32) read in the `TypedArray` block (~1955). `rotationEnabled` (~202) is the
    sibling flag.
  - `updateRotation(rotationAngle, pivotX, pivotY)` (~1290) sets a new absolute rotation around a pivot.
- Tests: `lib/src/test/.../RotationGestureDetectorTest.kt`, `lib/src/test/.../GestureBitmapViewTest.kt` (Robolectric;
  scroll fling tests around `gestureFling_*`, line ~3284); instrumented `lib/src/androidTest/.../GestureBitmapViewTest.kt`
  (rotation tests are `@RequiresRealDevice`).
- Docs: Antora `docs/modules/ROOT/pages/gestures.adoc` (Rotation and Fling sections), `configuration.adoc` (property
  table ~line 108 and XML attribute table ~line 199), `index.adoc` feature list; `README.md` (~line 159) and
  `CHANGELOG.md` (Unreleased section).
- Precedent: `.archive/implementation_plan_1.md` (tension on overscroll) — same file set and grouping shape.

## Implementation steps

### Group 1 — Angular velocity in the rotation detector (Parallelizable: yes)

- [x] Task 1. Measure angular velocity and notify the end of a rotation gesture _(android)_
  - Done: RotationGestureDetector.kt (angularVelocity EMA, onRotationEnd default method), RotationGestureDetectorTest (37 tests pass, 100% line / 97% branch unit coverage), no new Lint issues; also lib/module.md + Dokka include in lib/build.gradle.kts, dokkaGenerate passes.
  - [x] Task 1.1. In `RotationGestureDetector.kt` add a public read-only `angularVelocity: Float` (radians/second,
    positive = clockwise as `atan2` angles grow), computed on each two-finger `ACTION_MOVE` from the wrapped angle
    delta divided by the `event.eventTime` difference. Smooth over a short window (e.g. keep the last ~100 ms of
    samples or an exponential moving average) so one noisy event does not decide the fling; reset it to `0` in
    `startTracking`, and when `eventTime` does not advance.
    ```kotlin
    private fun wrap(delta: Float): Float = when {
        delta > PI -> delta - 2 * PI
        delta < -PI -> delta + 2 * PI
        else -> delta
    }.toFloat()
    ```
  - [x] Task 1.2. Add to `OnRotationGestureListener` a method with a default body (keeps source compatibility):
    ```kotlin
    fun onRotationEnd(
        gestureDetector: RotationGestureDetector,
        angularVelocity: Float, pivotX: Float, pivotY: Float
    ) {
        // optional
    }
    ```
    Invoke it (once per gesture) when two tracked fingers stop rotating: from `onPointerUp` when a tracked finger is
    lifted while both were tracked, and from `ACTION_UP` / `ACTION_CANCEL` if both were still tracked. Do **not** call
    it when no rotation movement was ever reported in the gesture. The velocity passed is `angularVelocity` captured
    before it is reset. Cancel (`ACTION_CANCEL`) reports velocity `0`.
  - [x] Task 1.3. Extend `RotationGestureDetectorTest.kt`: velocity sign and magnitude for a clockwise and a
    counter-clockwise synthetic rotation, wrap-around across ±π, zero velocity for a held gesture, `onRotationEnd` fired
    once on pointer up / up / cancel, not fired without movement, listener without override still works.
  - [x] Task 1.4. Run `RotationGestureDetectorTest` delegated to the `iru-gate-runner` agent
    (`Skill: iru-android-test`, selector `RotationGestureDetectorTest`) so build output stays out of the main context.

### Group 2 — Rotation fling in the view (Parallelizable: no — every task edits `GestureBitmapView.kt` and `GestureBitmapViewTest.kt`, and depends on Group 1's listener method)

- [x] Task 2. Implement rotation fling in `GestureBitmapView` _(android)_ — GestureBitmapView.kt, attrs.xml, GestureBitmapViewTest.kt (12 new tests); 98.5% line / 88.2% branch unit coverage (GestureBitmapView), 99.2% line (RotationGestureDetector); Lint 0 issues (unchanged baseline); KDoc complete, dokkaGenerate passes; license headers compliant.
  - [x] Task 2.1. Add configuration in `GestureBitmapView.kt`: `var rotationFlingEnabled = true` (KDoc: only applies when
    `rotationEnabled`), `DEFAULT_ROTATION_FLING_ENABLED`-style constants only if consistent with neighbours, a private
    `MIN_ROTATION_FLING_VELOCITY` constant (rad/s), and a `ROTATION_FLING_ENABLED_KEY` bundle key.
  - [x] Task 2.2. Add a private `rotateAnimator: ValueAnimator?` and `smoothRotateBy(angle, pivotX, pivotY)`: animates
    the rotation from the current angle to `current + angle` calling `updateRotation(..., pivotX, pivotY)` and
    invalidating, with `DecelerateInterpolator` and `animationDurationMillis`, mirroring `smoothTranslateTo` (including
    the cancelled flag). On a non-cancelled end, call `limitScaleAndTranslation()` (needs `isAnyAnimatorRunning()` false
    at that point, so ensure the animator is no longer reported as running inside `onAnimationEnd`, or clear the field
    first).
  - [x] Task 2.3. Include `rotateAnimator` in `cancelAllAnimators()` (new `cancelRotateAnimator()`) and in
    `isAnyAnimatorRunning()`.
  - [x] Task 2.4. In the rotation listener of `init`, override `onRotationEnd`: if `rotationEnabled &&
    rotationFlingEnabled && abs(angularVelocity) >= MIN_ROTATION_FLING_VELOCITY`, compute
    `angle = angularVelocity * (animationDurationMillis / 1000f) / FLING_INITIAL_SPEED_FACTOR` and call
    `smoothRotateBy(angle, pivotX, pivotY)`. Make sure the end-of-gesture `ACTION_UP` → `limitScaleAndTranslation()`
    does not cancel it when the scale is within limits (a running animator already skips the scroll correction); when
    the scale is outside its limits the scale correction wins (documented behavior).
  - [x] Task 2.5. Add unit tests to `GestureBitmapViewTest.kt` (Robolectric, following the `gestureFling_*` style):
    fling starts after a fast rotation release and ends with a larger rotation in the same direction; no fling when
    `rotationFlingEnabled = false`, `rotationEnabled = false`, or below-threshold velocity; a new `ACTION_DOWN`/scroll/
    scale cancels it; after it ends the view is within scroll limits; `onSave/RestoreInstanceState` round-trips
    `rotationFlingEnabled`. Keep any instrumented test `@RequiresRealDevice`-annotated if one is added in
    `lib/src/androidTest/.../GestureBitmapViewTest.kt`.
  - [x] Task 2.6. Add the `rotationFlingEnabled` boolean attribute to `lib/src/main/res/values/attrs.xml`, read it in
    the `TypedArray` block next to `scrollTensionEnabled`, and save/restore it in `onSaveInstanceState` /
    `onRestoreInstanceState` (default `true` when absent from an old bundle).
  - [x] Task 2.7. Run the library unit tests delegated to the `iru-gate-runner` agent (`iru-android-test`, selector
    `GestureBitmapViewTest,RotationGestureDetectorTest`), then coverage (`iru-android-coverage`, scope
    `GestureBitmapView,RotationGestureDetector`, ≥ 80% line coverage on touched classes), code quality
    (`iru-android-code-quality`, no new issues vs. baseline), KDoc audit (`iru-android-dokka` on both classes) and
    license headers (`iru-check-license`) — each as its own `iru-gate-runner` call.

### Group 3 — Documentation (Parallelizable: yes — different files, no shared edits)

- [x] Task 3. Update README and changelog
  - [x] Task 3.1. `README.md` (~line 159): add `rotationFlingEnabled` to the configuration list next to
    `rotationEnabled`/`scrollTension…`.
  - [x] Task 3.2. `CHANGELOG.md` Unreleased → Added: "Rotation fling: when a rotation gesture ends while still rotating,
    the bitmap keeps rotating, decelerating until it stops; configurable with `rotationFlingEnabled` /
    `app:rotationFlingEnabled` (default `true`)."
- [x] Task 4. Update Antora docs — gestures, configuration, index, usage pages updated; Antora build, Pagefind, nav check and Mermaid check pass (docs only: no code, tests or coverage involved).
  - [x] Task 4.1. `docs/modules/ROOT/pages/gestures.adoc`: add a "Rotation fling" paragraph under the Rotation section
    (starts at the angular velocity of the fingers, decelerates over `animationDurationMillis`, around the last pivot,
    cancelled by any new touch, followed by the usual limit correction; scale correction takes precedence).
  - [x] Task 4.2. `docs/modules/ROOT/pages/configuration.adoc`: add `rotationFlingEnabled` to the property table and
    `app:rotationFlingEnabled` to the XML attribute table; `index.adoc` feature list: add "Rotation fling".
  - [x] Task 4.3. Run `/iru-update-docs` for the uncommitted changes to verify the Antora pages and build
    (`iru-build-docs`), skipping the User interface section (no screens changed).

### Group 4 — Keep rotation when correcting to minimum scale (Parallelizable: no — Task 6 documents the behavior Task 5 implements, and Task 5's code and tests share `GestureBitmapView.kt` / `GestureBitmapViewTest.kt`)

- [x] Task 5. Keep rotation in the end-of-gesture minimum-scale correction _(android)_
  - Done: root cause was single-precision matrix drift (scale 0.99999994 < 1.0 after rotating) triggering smoothResetTo; fixed with `SCALE_TOLERANCE` (1e-4) in `limitScaleAndTranslation()` and `resetRotation` param on `smoothScaleTo` (false from `limitScale`, true from double tap). `GestureBitmapView.kt`, `GestureBitmapViewTest.kt`. Tests: 187 pass; new `gestureRotationEnd_whenMinimumScale_flingKeepsRotation`, `gestureDoubleTap_whenReturningToMinScale_resetsRotationAndTranslation`; updated `onTouchEvent_whenActionUpAndScaleTooSmall_...` (now keeps rotation, listener not notified); rotation fling helper no longer zooms to 2.0. Coverage 99.7% line / 89.6% branch; no new Lint issues (:lib 0); dokka ok; license compliant.
  - [x] Task 5.1. Find out exactly why a rotation fling at the default scale (1.0 = `minScale`) ends in
    `smoothResetTo` (e.g. accumulated floating-point error making `parameters.scale` slightly lower than `minScale`
    after rotating, or a small pinch during the rotation). Add a unit test in `GestureBitmapViewTest.kt` reproducing it:
    rotate with a fling at scale 1.0, let the animations end, and assert the rotation angle is not 0.
  - [x] Task 5.2. In `GestureBitmapView.kt`, change the minimum-scale path used by the end-of-gesture limit correction
    (`limitScaleAndTranslation()` → `limitScale()` → `smoothScaleTo()` → `smoothResetTo()`) so it preserves the current
    rotation angle: animate only the scale to `minScale` (around the same pivot as today) and then bring translation
    back within scroll limits with the existing `smoothScrollBy(0.0f, 0.0f, skipIfNotNeeded = true)`. Suggested shape:
    a `resetRotation: Boolean` parameter on `smoothScaleTo` / `smoothResetTo` (or a separate path), `false` from
    `limitScale`, `true` from the double-tap handler, so double tap keeps its current behavior. Translation is reset to
    0 only when rotation is also reset; when rotation is kept, the scroll-limit correction decides the translation.
  - [x] Task 5.3. If 5.1 shows floating-point drift is the trigger, also treat a scale within a small tolerance of
    `minScale`/`maxScale` as within limits in `limitScaleAndTranslation()` (private constant, documented), so a plain
    rotation at minimum zoom does not start a scale animation at all.
  - [x] Task 5.4. `rotateAndTranslateAnimationCompletedListener` is notified when rotation and translation were reset
    together: keep notifying it only from the path that actually resets rotation, and update its KDoc and the KDoc of
    `smoothScaleTo` / `smoothResetTo` accordingly.
  - [x] Task 5.5. Update `GestureBitmapViewTest.kt`: the rotation fling tests must also pass at scale 1.0 (remove the
    zoom-to-2.0 workaround or add a scale-1.0 variant); a pinch below `minScale` after a rotation returns to `minScale`
    keeping the rotation and ends within scroll limits; double tap back to minimum scale still resets rotation and
    translation and notifies `rotateAndTranslateAnimationCompletedListener`; existing tests that assumed the
    end-of-gesture reset of rotation are updated to the new behavior (list them in the progress note).
  - [x] Task 5.6. Run the gates delegated to `iru-gate-runner`, each as its own call: `iru-android-test`
    (`GestureBitmapViewTest,RotationGestureDetectorTest`), `iru-android-coverage` (`GestureBitmapView`, ≥ 80% line),
    `iru-android-code-quality` (no new issues against the 16-warning baseline in `app/`), `iru-android-dokka`
    (`GestureBitmapView`) and `iru-check-license`.
- [x] Task 6. Document the new minimum-scale behavior
  - Done: gestures.adoc, usage.adoc, CHANGELOG.md updated; README.md and configuration.adoc needed no change.
  - [x] Task 6.1. `docs/modules/ROOT/pages/gestures.adoc`: in the Rotation fling paragraph and the section describing
    the return to minimum scale, state that a gesture ending below the minimum scale animates back to it keeping the
    rotation (translation is brought back within limits), while double tap back to the minimum scale resets rotation
    and translation. Remove the "scale correction takes precedence / undoes the fling" wording.
  - [x] Task 6.2. `CHANGELOG.md` Unreleased → Changed: "Returning to the minimum scale at the end of a gesture keeps
    the rotation instead of resetting it; double tap keeps resetting rotation and translation." Check `README.md`
    and `configuration.adoc` (`rotateAndTranslateAnimationCompletedListener`) for wording that needs the same change.
