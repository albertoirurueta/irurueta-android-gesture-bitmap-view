# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Redraws are requested with `invalidate()` instead of `postInvalidate()`, so that changes made by gestures and
  animations are drawn without an additional frame of delay, and `onDraw` no longer allocates.
- Mipmaps are enabled on the bitmap set on the view, to make drawing at reduced sizes cheaper.
- Scroll limits are computed from the area where the bitmap is currently displayed instead of the one at the start of
  the gesture. While dragging, a bitmap left beyond its limits by a pinch or a rotation is no longer snapped to them
  in a single step; the excess is corrected by the release animation.
- Fling animation starts at the velocity of the finger (it previously started at twice that velocity), hence fling
  distance is now half of what it used to be.
- Scale animations decelerate instead of accelerating. When returning to the minimum scale, rotation and translation
  are restored within the same animation instead of in a second one.
- No animation is started when a gesture ends with the bitmap already within its limits.
- `RotationGestureDetector` ignores fingers beyond the second one, and restarts tracking with the remaining fingers
  when a tracked finger is lifted. `ACTION_UP` now clears both tracked fingers.
- The view asks its parent not to intercept touch events once a second finger touches the screen.
- Sample app image moved to `drawable-nodpi` so that it is not scaled up by screen density.

### Fixed

- Bitmap not returning within its scroll limits when released after the first drag.
- Wrong coordinate used to decide whether the bitmap fits horizontally when limiting scroll.
- Scale and translation limits not being restored when a gesture is cancelled.
- Possible `IllegalArgumentException` and rotation jumps in `RotationGestureDetector` when the first finger is lifted
  before the second one or when more than two fingers are used.

## [1.2.7] - 2026-03-07

### Changed

- Build upgraded to Android Gradle Plugin 9.0.1, Gradle 9.4.0 and Kotlin 2.3.10.
- Updated dependencies: `irurueta-geometry` 1.5.0, Compose BOM 2026.02.01 and test/publishing tooling. No library
  source changes.

## [1.2.6] - 2025-12-28

### Changed

- Minimum supported SDK raised from 24 to 26.
- Updated dependencies: `irurueta-geometry` 1.4.0, Kotlin 2.3.0, Android Gradle Plugin 8.13.2, Compose BOM
  2025.12.01, Dokka 2.1.0 and test/publishing tooling.

## [1.2.5] - 2025-09-24

### Changed

- Updated dependencies: `irurueta-geometry` 1.3.2, Kotlin 2.2.20, Android Gradle Plugin 8.13.0, Material 1.13.0,
  Compose BOM 2025.09.00 and test tooling. No library source changes.

## [1.2.4] - 2025-07-29

### Changed

- Updated test-only dependency `irurueta-android-test-utils` to 1.3.0. No library source changes.

## [1.2.3] - 2025-07-27

### Removed

- Unused test launcher icons (`ic_launcher_test`, `ic_launcher_test_round`) are no longer packaged in the library's
  resources.

### Changed

- Internal test helpers replaced by the shared `irurueta-android-test-utils` test dependency; MockK updated to
  1.14.5.

## [1.2.2] - 2025-07-22

### Changed

- Test suite improvements only. No library source or dependency changes.

## [1.2.1] - 2025-07-20

### Changed

- Updated to Kotlin 2.2.0, Android Gradle Plugin 8.11.1, Gradle 8.14.3 and Compose BOM 2025.07.00.
- Maven Central publishing migrated to the Central Portal (vanniktech publish plugin 0.34.0).
- Minor source cleanups in the library manifest and `InvalidMatrixException`; no behavior change.

## [1.2.0] - 2025-07-03

### Added

- `GestureBitmapView.isAntialiasingEnabled` property to enable or disable bitmap antialiasing.

### Changed

- Further performance improvements: the display matrix is now updated on every rotation step instead of
  conditionally invalidating the view.
- Android Gradle Plugin updated to 8.11.0.

## [1.1.0] - 2025-06-29

### Changed

- Improved performance of touch event handling and animations.
- Updated to compile SDK 36 and Java/JVM target 17.
- Build migrated to Gradle Kotlin DSL with a version catalog; sample app rebuilt with Jetpack Compose.
- Overridden callbacks (`onDraw`, animator, scale and gesture listeners) updated to the non-null signatures of the
  current Android SDK.

## [1.0.1] - 2023-03-05

### Changed

- Updated to compile and target SDK 31 and Kotlin 1.7.21.
- Now depends on the released `irurueta-geometry` 1.1.0 instead of a snapshot.
- Improved test coverage and README documentation (including a demo video). No library source changes.

## [1.0.0] - 2021-10-07

### Added

- Initial release of `GestureBitmapView`, an Android view that displays a bitmap or drawable and supports
  pinch-to-zoom, scrolling (one- or two-finger), rotation and double-tap zoom, each individually toggleable.
- Configurable display types, minimum/maximum scale, scale jump factor, and scale/scroll margins.
- Animated scale, scroll, and rotate-and-translate transitions with configurable duration.
- Listeners for reaching bounds (top/bottom/left/right), minimum/maximum scale, animation completion and double tap.
- Access to transformation parameters and matrices through `MetricTransformationParameters`, with
  `InvalidMatrixException` for non-metric matrices.
- `RotationGestureDetector` for two-finger rotation gestures.

[Unreleased]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.7...HEAD
[1.2.7]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.6...1.2.7
[1.2.6]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.5...1.2.6
[1.2.5]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.4...1.2.5
[1.2.4]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.3...1.2.4
[1.2.3]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.2...1.2.3
[1.2.2]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.1...1.2.2
[1.2.1]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/releases/tag/1.0.0
