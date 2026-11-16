# irurueta-android-gesture-bitmap-view

A touch gesture capable Android view to display bitmaps and perform scaling, translation and
rotation on the bitmap.

[![Release](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/main.yml/badge.svg)](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/main.yml)
[![Develop](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/develop.yml/badge.svg?branch=develop)](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/develop.yml)
[![Security](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/security.yml/badge.svg)](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/security.yml)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=bugs)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=coverage)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view)

[![Maven Central](https://img.shields.io/maven-central/v/com.irurueta/irurueta-android-gesture-bitmap-view)](https://central.sonatype.com/artifact/com.irurueta/irurueta-android-gesture-bitmap-view)

## Project Status

| Item | Value |
| --- | --- |
| Language | Kotlin 2.3.10 (JVM target 17) |
| Build tool | Gradle 9.4.0 (Kotlin DSL) |
| Latest release | `1.2.7` |
| Platform | Android |
| Min / compile SDK | `minSdk` 26 (Android 8.0) / `compileSdk` 36 |
| Kotlin / AGP | Kotlin 2.3.10 / Android Gradle Plugin 9.0.1 |
| License | Apache License 2.0 |
| CI | GitHub Actions: `develop` builds (tests, Lint, SonarCloud, docs, snapshot publish), releases (Maven Central publish and GitHub Pages docs) and weekly security scans |
| Quality | SonarCloud, JaCoCo coverage, Android Lint |
| Distribution | Maven Central (`com.irurueta:irurueta-android-gesture-bitmap-view`) |

## Documentation

- [Documentation site](https://albertoirurueta.github.io/irurueta-android-gesture-bitmap-view) (Antora): installation,
  usage, configuration reference, gestures, sample app and building instructions.
- [API documentation](https://albertoirurueta.github.io/irurueta-android-gesture-bitmap-view/api/) (Dokka).
- [SonarCloud dashboard](https://sonarcloud.io/summary/new_code?id=albertoirurueta_irurueta-android-gesture-bitmap-view).
- [Changelog](CHANGELOG.md).

## Overview

This library contains a view that can display a bitmap and handle touch gestures to translate, scale
and rotate the displayed bitmap.

![Demo](docs/video.gif)

## Installation

The library is published to Maven Central. Make sure `mavenCentral()` is among your repositories and add the
dependency to your module's `build.gradle.kts`:

Latest release:

```kotlin
dependencies {
    implementation("com.irurueta:irurueta-android-gesture-bitmap-view:1.2.7")
}
```

The Installation page of the [documentation site](https://albertoirurueta.github.io/irurueta-android-gesture-bitmap-view)
also covers the Groovy DSL and version catalog forms.

## Usage

The view can be added to your xml layout as the example below:

```xml
<com.irurueta.android.gesturebitmap.GestureBitmapView
    android:id="@+id/gesture_bitmap"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:src="@drawable/image"/>
```

Or configured from code:

```kotlin
val view = findViewById<GestureBitmapView>(R.id.gesture_bitmap)
view.displayType = GestureBitmapView.DisplayType.CENTER_CROP
view.rotationEnabled = false
view.maxScale = 5.0f

// resets scale, rotation and translation for the new bitmap
view.bitmap = bitmap
```

Note that setting a new bitmap recycles the bitmap previously held by the view.

The following properties are supported in xml layouts:

- **src**: drawable to be displayed
- **displayType**: enum to indicate the way the bitmap is initially displayed. The following options
  are supported:
    - **none**: bitmap is displayed at original scale, without additional rotation or translation.
    - **fit_if_bigger**: bitmap is initially scaled only if bigger than the limits of the view.
    - **fit_x_top**: bitmap is initially scaled to fit horizontally while keeping aspect ratio.
      Bitmap is aligned on top border of the view.
    - **fit_x_bottom**: bitmap is initially scaled to fit horizontally while keeping aspect ratio.
      Bitmap is aligned on bottom border of the view.
    - **fit_x_center**: bitmap is initially scaled to fit horizontally while keeping aspect ratio.
      Bitmap is vertically centered.
    - **fit_y_left**: bitmap is initially scaled to fit vertically while keeping aspect ratio.
      Bitmap is aligned to the left border of the view.
    - **fit_y_right**: bitmap is initially scaled to fit vertically while keeping aspect ratio.
      Bitmap is aligned to the right border of the view.
    - **fit_y_center**: bitmap is initially scaled to fit vertically while keeping aspect ratio.
      Bitmap is horizontally centered.
    - **center_crop**: bitmap is initially scaled to fill the whole view and centered on the view
      while preserving aspect ratio.
- **animationDurationMillis**: duration of animations when doing smooth scaling or translation and
  expressed in milliseconds.
- **rotationEnabled**: indicates whether the view handles rotation gestures with two fingers. If
  false, bitmap rotations are not allowed.
- **scaleEnabled**: indicates whether the view handles pinch gestures to zoom bitmap. If false,
  bitmap scaling is not allowed.
- **scrollEnabled**: indicates whether the view handles scroll gestures to translate bitmap. If
  false, scrolling is not allowed.
- **twoFingerScrollEnabled**: indicates whether scrolling can be done with two fingers. If enabled,
  scrolling can be done while doing scaling or rotation (if those are enabled as well), otherwise
  scrolling can only be done with one finger (if scrolling is also enabled).
- **exclusiveTwoFingerScrollEnabled**: indicates whether scroll using two fingers is exclusive. When
  this is true, and both scrollEnabled and twoFingerScrollEnabled are also true, then scroll is
  disabled with one finger and can only be done with two fingers. If false and both scrollEnabled
  and twoFingerScrollEnabled, then scroll can be made with both one or two fingers.
- **doubleTapEnabled**: indicates whether double tap can be used to increase the scale at tapped
  location by a certain jump factor.
- **minScale**: minimum allowed scale to display the bitmap. This is only taken into account if
  scale is enabled. When double tap is enabled and maximum scale is reached, then the next double
  tap resets scale to this value.
- **maxScale**: maximum allowed scale to display the bitmap. This is only taken into account if
  scale is enabled. When double tap is enabled and this scale is reached, then the next double tap
  resets scale to the minimum value.
- **scaleFactorJump**: scale factor to increase the scale when double tap is done.
- **minScaleMargin**: amount of margin given when minimum scale is exceeded. A positive value can be
  used to make a bouncing effect, so that bitmap bounces back to minimum scale when such scale is
  exceeded.
- **maxScaleMargin**: amount of margin given when maximum scale is exceeded. A positive value can be
  used to make a bouncing effect, so that bitmap bounces back to maximum scale when such scale is
  exceeded.
- **leftScrollMargin**: amount of left margin given when left boundary of bitmap is reached while
  scrolling. A positive value can be used to make a bouncing effect, so that bitmap bounces back to
  the left side when left boundary is exceeded.
- **topScrollMargin**: amount of top margin given when top boundary of bitmap is reached while
  scrolling. A positive value can be used to make a bouncing effect, so that bitmap bounces back to
  the top side when top boundary is exceeded.
- **rightScrollMargin**: amount of right margin given when right boundary of bitmap is reached while
  scrolling. A positive value can be used to make a bouncing effect, so that bitmap bounces back to
  the right side when right boundary is exceeded.
- **bottomScrollMargin**: amount of bottom margin given when bottom boundary of bitmap is reached
  while scrolling. A positive value can be used to make a bouncing effect, so that bitmap bounces
  back to the bottom side when bottom boundary is reached.

See the Configuration page of the [documentation site](https://albertoirurueta.github.io/irurueta-android-gesture-bitmap-view)
for default values and the corresponding Kotlin properties and listeners.

## Additional details

This library is contained within the `lib` module in source code.
`app` module contains an example of an application using the view contained in the `lib` module.

### Gradle tasks

To execute all unit tests, execute:

```
./gradlew test
```

To execute all instrumentation tests, first make sure you have connected a device or emulator and
execute:

```
./gradlew connectedAndroidTest
```

If you only want to run tests of a specific module, execute:

```
./gradlew lib:test
```

or

```
./gradlew lib:connectedAndroidTest
```

where "lib" is the name of the library module in the project

To generate API documentation (Dokka), run:

```
./gradlew lib:dokkaGenerate
```

To build the documentation site, run:

```
cd docs && npx antora antora-playbook.yml
```

To execute SonarCloud analysis, run:

```
./gradlew :lib:sonar
```

To convert exec files to xml and html reports:

```
java -jar jacoco-0.8.13/lib/jacococli.jar report lib/build/jacoco/testReleaseUnitTest.exec --classfiles lib/build/tmp/kotlin-classes/debug --sourcefiles lib/src/main/java --html lib/build/reports/coverage/test --xml lib/build/reports/coverage/test/report.xml
```

Github actions to release library in maven central has been configured following:
[https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/)

## License

This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
