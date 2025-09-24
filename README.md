# irurueta-android-gesture-bitmap-view

A touch gesture capable Android view to display bitmaps and perform scaling, translation and
rotation on the bitmap

[![Build Status](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions/workflows/main.yml/badge.svg)](https://github.com/albertoirurueta/irurueta-android-gesture-bitmap-view/actions)

[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=code_smells)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=coverage)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)

[![Duplicated lines](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Lines of code](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=ncloc)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)

[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Quality gate](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=alert_status)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Reliability](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)

[![Security](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=security_rating)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Technical debt](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=sqale_index)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=albertoirurueta_irurueta-android-gesture-bitmap-view&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=albertoirurueta_irurueta-android-gesture-bitmap-view)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.irurueta/irurueta-android-gesture-bitmap-view/badge.svg)](https://search.maven.org/artifact/com.irurueta/irurueta-android-gesture-bitmap-view/1.0.0/aar)

[API Documentation](http://albertoirurueta.github.io/irurueta-android-gesture-bitmap-view)

## Overview

This library contains a view that can display a bitmap an handle touch gestures to translate, scale
and rotate displayed bitmap.

![Demo](docs/video.gif)

## Usage

Add the following dependency to your project:

```
implementation 'com.irurueta:irurueta-android-gesture-bitmap-view:1.2.5'
```

The view can be added to your xml layout as the example below:

```
    <com.irurueta.android.gesturebitmap.GestureBitmapView
        android:id="@+id/gesture_bitmap"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:src="@drawable/image"/>
```

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
- **animationDurationMillis**: duration of animations when doing smoooth scaling or translation and
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


## Additional details

This library is contained within the `lib` module in source code.
`app` module contains an example of an application using the view contained in the `lib` module.

### Gradle taks

To execute all unit tests, execute:

```
gradlew test
```

To execute all instrumentation tests, first make sure you have connected a device or emulator and
execute:

```
gradlew connectedAndroidTest
```

If you only want to run tests of a specific module, execute:

```
gradlew lib:test
```

or

```
gradlew lib:connectedAndroidTest
```

where "lib" is the name of the library module in the project

To generate documentation, run:

```
gradlew dokkaHtml
```

To execute sonarqube, run:

```
gradlew sonarqube
```

To convert exec files to xml and html reports:

```
java -jar jacoco-0.8.13/lib/jacococli.jar report lib/build/jacoco/testReleaseUnitTest.exec --classfiles lib/build/tmp/kotlin-classes/debug --sourcefiles lib/src/main/java --html lib/build/reports/coverage/test --xml lib/build/reports/coverage/test/report.xml
```

Github actions to release library in maven central has been configured following:
[https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/)

