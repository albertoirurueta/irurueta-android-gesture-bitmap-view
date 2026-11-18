# Implementation plan — Support for tension when overscrolling

## Task summary

While a finger drags the bitmap past an edge, `GestureBitmapView` lets it follow the finger 1:1 until the scroll margin
is reached, then stops abruptly; on release it bounces back. The ticket asks for a smoother, rubber-band feel by adding
increasing tension (resistance) the further the user overscrolls.

Choices made on the user's behalf (challenge them during review):
- Tension is a stateless, per-event damping: inside the allowed range the bitmap moves 1:1. Beyond the un-margined
  bound, each finger delta is multiplied by `(1 - overscroll / margin)`, so the bitmap slows progressively and
  asymptotically approaches the margin without ever hitting a hard wall. No new gesture state is needed.
- A new boolean `scrollTensionEnabled` (default `true`) switches the behavior, so the previous hard-clamp behavior
  stays available. Existing margins keep meaning "maximum overshoot in pixels".
- Fling/animated scrolls (`snap = true`) and the release bounce-back are unchanged.

Source: GitHub issue #1
Base branch: develop

Open clarifications: none

## Acceptance criteria coverage

| # | Criterion | Covered by tasks |
|---|---|---|
| 1 | "A smoother implementation could be made by adding increasing tension as the user overscrolls." | Task 1, Task 2 |
| 2 | derived: overscroll resistance increases monotonically and never exceeds the configured margin | Task 1.2, Task 1.3 |
| 3 | derived: scrolling inside the limits and release/fling bounce-back behave as before | Task 1.3 |
| 4 | derived: tension can be disabled/configured via property, XML attribute and survives state save/restore | Task 1.1, Task 2 |
| 5 | derived: documentation describes the tension behavior and the new option | Task 3 |

## Current code state

- `lib/src/main/java/com/irurueta/android/gesturebitmap/GestureBitmapView.kt` (single view class, ~2400 lines).
- `onScroll` (gesture listener) → `scrollBy(dx, dy)` → `limitScroll(dx, dy, left/right/top/bottomScrollMargin, snap = false, scrollDiff)` →
  `limitDelta(current, delta, minValue, maxValue, snap)`, which hard-clamps `current + delta` to `[min, max]` (the
  margin-expanded bounds from `minAllowedStart` / `maxAllowedStart`).
- `smoothScrollBy` (fling, release correction) calls `limitScroll` with zero margins and `snap = true`; must not change.
- Margins: `leftScrollMargin`, `topScrollMargin`, `rightScrollMargin`, `bottomScrollMargin` (default `DEFAULT_SCROLL_MARGIN = 100f`),
  saved in `onSaveInstanceState` / restored in `onRestoreInstanceState` via `*_SCROLL_MARGIN_KEY` constants, and read from
  XML via `R.styleable.GestureBitmapView_*ScrollMargin` (attrs in `lib/src/main/res/values/attrs.xml`).
- Bound-reached listeners fire inside `limitScroll` (based on margin bounds); keep their semantics.
- Tests: JVM `lib/src/test/java/com/irurueta/android/gesturebitmap/GestureBitmapViewTest.kt` (Robolectric-style, uses reflection helpers);
  instrumented `lib/src/androidTest/.../test/GestureBitmapViewTest.kt` (real-gesture tests already `@RequiresRealDevice`).
- Docs (Antora): `docs/modules/ROOT/pages/gestures.adoc` ("Scroll limits and bounce"), `configuration.adoc` (properties table), `index.adoc`.
- Project: `android-library` (Kotlin, Gradle KTS, View-based, `.ui-quality.yml accessibility: enforce` but no new UI components added).

## Implementation steps

### Group 1 — Core tension behavior (Parallelizable: yes — single task)

- [x] Task 1. Add rubber-band tension to drag-driven overscroll _(android)_
  - [x] Task 1.1. In `GestureBitmapView.kt` add `var scrollTensionEnabled = true` (KDoc: when true, dragging past the un-margined
    bound is progressively resisted up to the margin; when false the bitmap follows the finger until the hard margin).
  - [x] Task 1.2. Extend `limitDelta` (or add a `tensionDelta` helper it calls) so that when `!snap`, tension is enabled and a margin
    `m > 0` exists: compute the overscroll `o` of `current` beyond the margin-less bound (`o = max(0, bound - current)` style per side),
    and if the delta moves further outward use `delta * (1 - o/m)` (clamped to `>= 0`, and to the margin bound); inward deltas are
    unchanged. This requires passing the margin-less bounds (or the margin) into `limitDelta` from `limitScroll`. Keep the existing
    out-of-range branches (scale/rotation changed mid-gesture) working.
    ```kotlin
    val resistance = (1.0f - overscroll / margin).coerceIn(0.0f, 1.0f)
    val allowed = delta * resistance
    ```
  - [x] Task 1.3. Add unit tests to `lib/src/test/.../GestureBitmapViewTest.kt`: default `scrollTensionEnabled` is `true`; inside limits
    movement is 1:1; successive equal outward drags move the bitmap by strictly decreasing amounts and never exceed the margin on all
    four sides; inward drag from an overscrolled position moves 1:1; with tension disabled the old hard-clamp result is obtained; margin `0`
    behaves as before; `smoothScrollBy`/fling results unchanged; bound-reached listeners still notified.
  - [x] Task 1.4. Run the touched tests via `iru-gate-runner` (`Skill iru-android-test` with selector `GestureBitmapViewTest`) and check coverage of
    the new code via `iru-android-coverage`; run `iru-android-code-quality`.
    - Done: `GestureBitmapView.kt` (`scrollTensionEnabled`, `limitDelta` soft bounds, `tensionDelta`); 7 tests added in `GestureBitmapViewTest` (+ existing `limitDelta` reflection test updated to new signature). 99.7% line / 87.5% branch coverage of `GestureBitmapView` (measured with a temporary Robolectric/JaCoCo `isIncludeNoLocationClasses` setting, reverted); Android Lint :lib 0 issues; full `./gradlew test` passes.

### Group 2 — XML attribute and state persistence (Parallelizable: yes — single task; depends on Group 1 property, edits same test file)

- [x] Task 2. Expose and persist `scrollTensionEnabled` _(android)_
  - [x] Task 2.1. Add `<attr name="scrollTensionEnabled" format="boolean"/>` to `lib/src/main/res/values/attrs.xml` and read it in the view's
    attribute-parsing code (default `true`).
  - [x] Task 2.2. Save/restore it in `onSaveInstanceState` / `onRestoreInstanceState` with a new `SCROLL_TENSION_ENABLED_KEY` constant.
  - [x] Task 2.3. Extend the existing attribute-parsing and save/restore tests in the JVM `GestureBitmapViewTest.kt`.
  - [x] Task 2.4. Run tests via `iru-gate-runner` (`iru-android-test`), then `iru-android-code-quality` and license/KDoc checks (`iru-check-license`).
  - Done: attrs.xml, GestureBitmapView.kt (attr parse, save/restore with `SCROLL_TENSION_ENABLED_KEY`), GestureBitmapViewTest (130 tests pass, full suite green); GestureBitmapView 99.7% unit-test line coverage (627/629); :lib lint 0 issues.

### Group 3 — Documentation (Parallelizable: yes — single task)

- [x] Task 3. Document tension overscroll
  - Docs-only: gestures.adoc, configuration.adoc, index.adoc, usage.adoc, CHANGELOG.md; Antora build OK, Pagefind OK, nav check OK (no code, tests or coverage involved).
  - [x] Task 3.1. Update `docs/modules/ROOT/pages/gestures.adoc` ("Scroll limits and bounce") to describe progressive tension and that margins are the
    maximum overshoot; update `configuration.adoc` table with `scrollTensionEnabled` (`Boolean`, `true`); mention in `index.adoc` bounce bullet;
    add an entry to `CHANGELOG.md` under Unreleased.
  - [x] Task 3.2. Run `/iru-update-docs` and verify the Antora build (`iru-build-docs`).

## Follow-up (requested during review)

- [x] Task 4. Make tension adjustable _(android)_
  - [x] Task 4.1. Add `scrollTension: Float` (default `DEFAULT_SCROLL_TENSION = 1.0f`, `IllegalArgumentException` if negative) to
    `GestureBitmapView`; `tensionDelta` now uses `(1 - overscroll / margin)^scrollTension`.
  - [x] Task 4.2. Add `app:scrollTension` attribute and save/restore with `SCROLL_TENSION_KEY`.
  - [x] Task 4.3. Unit tests: default/setter/negative value, exact resistance for tensions 0, 0.5, 1 and 2, higher tension overscrolls less,
    zero tension hard-clamps; attribute parsing and state save/restore extended.
  - [x] Task 4.4. Docs (`gestures.adoc`, `configuration.adoc`, `usage.adoc`), `CHANGELOG.md` and `README.md` updated.
