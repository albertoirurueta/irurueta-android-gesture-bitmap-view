# Implementation plan — Support for preview and high res image swap

## Task summary

`GestureBitmapView` draws one full-size bitmap on every frame. The issue asks the view to show a low resolution
version when zoomed out and higher resolution when zoomed in, keeping position, rotation and zoom on screen, without
holding the whole image at full resolution in memory. The design agreed in the issue is:

- matrices defined in upright image pixels, independent of the bitmap being drawn, plus a constant EXIF orientation
  matrix;
- an `ImageSource` abstraction over `BitmapRegionDecoder`;
- a preview, a few cached whole-image levels within a memory budget, and one high resolution patch of the visible
  region, all decoded on a background thread and swapped in on the UI thread;
- hardware bitmaps by default with a software fallback;
- automatic use from `app:src` and new `setImage…` methods, with flags to turn it off;
- three ways to update image contents: mutable bitmap, `invalidateImage`, overlay drawing hook;
- documentation with a code example for every case.

Source: GitHub issue #3
Base branch: develop

Decisions taken with the user (recorded in the issue):

- EXIF orientation is applied; preview, levels and patches share one orientation matrix.
- `app:src` on a raster resource uses region decoding automatically. `regionDecodingEnabled` (default `true`) turns
  it off for every entry point.
- `bitmap`, `setBitmap` (including `resetMatrices = false`) and `setDrawable` keep today's behaviour.
- `Bitmap.Config.HARDWARE` is the default, `hardwareBitmapsEnabled` switches to `ARGB_8888` with `inBitmap` reuse.
- Cached whole-image levels are kept within `cachedLevelsMemoryBudget`.
- All three content-update options are kept, and all are documented with code examples.

Decisions taken on the user's behalf:

- The multi-resolution state (levels, patch, executor, generation counter) lives in a new internal class,
  `MultiResolutionImage`, not inside `GestureBitmapView.kt`. The view is already 2750 lines and the class can be unit
  tested without a view.
- No UI decomposition or screenshot matrix: the change adds no screen, component or visual state to the sample app.
  The optional draw-mode toggle in the sample app listed in the issue is left out; documentation examples are
  verified by a compiled test class instead (Task 14).
- Device measurements (issue steps 0 and 8: memory and frame timing on a physical device, hardware against software
  mode) cannot be run in this pipeline. They are recorded in the pull request as a manual follow-up (Task 13), as
  was done for issue #7. Hardware mode stays the default until those numbers exist.
- Values the issue marks "to tune" are implemented as named private constants with the issue's starting values:
  overscan 25% per side, patch budget 2× the view pixel count, default level budget 4× the view pixel count × 4 bytes.
- A fade-in of a new patch (optional in the issue) is not implemented.

Open clarifications: none

## Acceptance criteria coverage

| # | Criterion | Covered by tasks |
|---|---|---|
| 1 | "An image set through `app:src` (raster resource) or through the new source methods is handled automatically: zooming in on the sample image shows full-resolution detail of the visible region once the gesture settles, and position, rotation and zoom on screen do not change when the detail appears or is released." | Task 7, Task 8, Task 9, Task 12.4 |
| 2 | "Bitmap memory held by the view is bounded (cached levels within `cachedLevelsMemoryBudget`, plus one patch within its pixel budget) and does not grow with the image size; the full-resolution image is only held whole when it fits the budget." | Task 3, Task 7 |
| 3 | "Changing between scales covered by cached levels shows the sharper image immediately, with no decoding." | Task 3.3, Task 7.3 |
| 4 | "While a decode is pending the image stays visible at the best resolution already in memory, and gestures are not blocked." | Task 7, Task 8.4 |
| 5 | "EXIF orientation is applied, and preview and patches always show the same orientation." | Task 2, Task 4.2, Task 6, Task 12.2 |
| 6 | "Image contents can be updated keeping the size in three ways, and each one shows the change at every scale: a mutable bitmap held whole in memory, `invalidateImage` on a source, and the overlay drawing hook." | Task 5, Task 10, Task 11 |
| 7 | "`regionDecodingEnabled = false` gives a single full-resolution bitmap for every way of setting an image." | Task 8.2, Task 9 |
| 8 | "The documentation has a code example for each way of setting an image, each flag and each way of updating contents." | Task 14, Task 15 |
| 9 | "Images larger than the `Canvas` maximum bitmap size can be displayed through an `ImageSource`." | Task 3.2, Task 7.2 |
| 10 | "No decoding or texture upload of decoded images on the UI thread, and no allocations in `onDraw`." | Task 6.3, Task 7.1, Task 8.4 |
| 11 | "Drag, pinch and rotate frame timing with a source is no worse than the baseline of step 0." | Task 13 (manual follow-up on a physical device, recorded in the pull request) |
| 12 | "The existing `bitmap`, `setBitmap`, `setDrawable` and `app:src` API, the public matrices and the saved-state format keep working as today, including `setBitmap(bitmap, resetMatrices = false)`. The only differences are the two documented for `app:src` on raster resources, and `regionDecodingEnabled = false` restores the previous path." | Task 6.5, Task 8.6, Task 9.3 |

## Current code state

Android library, Kotlin, Gradle Kotlin DSL with wrapper. `lib/` is the library (`com.android.library`, minSdk 26,
compileSdk 36), `app/` is its View-based sample. The only library dependency is `material`.

- `lib/src/main/java/com/irurueta/android/gesturebitmap/GestureBitmapView.kt` (2750 lines), custom `View`:
  - `_bitmap`, `bitmap`, `setBitmap(bitmap, resetMatrices)` (lines 283 to 327): one bitmap, previous one recycled,
    `setHasMipMap(true)`.
  - `setDrawable` / `toBitmapWithoutUpscaling` (341 to 366).
  - `baseMatrix`, `paramsMatrix`, `displayMatrix`: all in pixels of the bitmap. `computeBaseMatrix(bitmap, …)` (1932)
    and `currentBitmapDisplayedRect` (1171) read `bitmap.width`/`height`. `updateDisplayMatrix()` (1202) is called by
    every gesture, animator and setter.
  - `onDraw` (728): `canvas.drawBitmap(bitmap, displayMatrix, bitmapPaint)`. `bitmapPaint` is a plain `Paint()` (179).
  - `onSizeChanged` (849), `onSaveInstanceState` / `onRestoreInstanceState` (744, 791, one bundle key per property).
  - `onTouchEvent` (658): gesture end at `ACTION_UP`/`ACTION_CANCEL` (710). Animator ends: `smoothTranslateTo` (1555),
    `smoothRotateBy` (1642), `startScaleAnimator` (1788). `isAnyAnimatorRunning()` (1849).
  - `init` (2077): reads `app:src` with `getDrawable` and calls `setDrawable`.
  - Listener interfaces nested in the view (2635 onwards).
- `lib/src/main/res/values/attrs.xml`: `GestureBitmapView` styleable.
- `MetricTransformationParameters.kt`, `RotationGestureDetector.kt`: not changed.
- No threading anywhere in the library.
- Unit tests (Robolectric, MockK): `lib/src/test/java/com/irurueta/android/gesturebitmap/GestureBitmapViewTest.kt`
  and one test class per source file. Instrumented tests:
  `lib/src/androidTest/java/com/irurueta/android/gesturebitmap/test/GestureBitmapViewTest.kt`. There is no
  `androidTest/assets` folder yet.
- Sample: `app/src/main/res/layout/activity_main.xml` uses `app:src="@drawable/image"`
  (`drawable-nodpi/image.jpeg`, 4032×3024).
- Docs: Antora under `docs/modules/ROOT` (`nav.adoc`, `pages/usage.adoc`, `configuration.adoc`, `reference.adoc`,
  `sample-app.adoc`). `usage.adoc` lines 76 to 81 hold the tip that tells callers to decode at display size.
- `.ui-quality.yml`: `accessibility: enforce`.

New files, all under `lib/src/main/java/com/irurueta/android/gesturebitmap/`:

| File | Visibility | Role |
|---|---|---|
| `ImageSource.kt` | public | Interface for a region-decodable image |
| `ImageOrientation.kt` | internal | EXIF orientation → upright size and orientation matrix |
| `ResolutionPlanner.kt` | internal | Pure maths: sample sizes, levels to cache, level to draw, patch region |
| `RegionDecoderImageSource.kt` | public | `BitmapRegionDecoder` implementation |
| `ImageSources.kt` | public | Factories (resource, asset, file, `Uri`, bytes) |
| `BitmapImageSource.kt` | public | Source backed by a caller-owned mutable bitmap |
| `MultiResolutionImage.kt` | internal | Levels, patch, background decoding, swap |

## Implementation steps

Every new file carries the repository license header and complete KDoc. Run tests by delegating to the
`iru-gate-runner` agent, for example `Agent({description: "Run tests for ResolutionPlannerTest", subagent_type:
"iru-gate-runner", prompt: "Invoke Skill({skill: \"iru-android-test\", args: \"ResolutionPlannerTest\"}) ..."})`,
never by running Gradle in the main conversation.

### Group 1 — Foundations (new files, no change to the view)

Parallelizable: yes

- [x] Task 1. Add the `ImageSource` interface _(android)_ — implemented (`ImageSource.kt`, `ImageSourceTest.kt`); tests in ImageSourceTest (100% unit-test line coverage), no Lint issues
  - [x] Task 1.1. Create `ImageSource.kt`: — done
        ```kotlin
        interface ImageSource : Closeable {
            val storedWidth: Int
            val storedHeight: Int
            /** One of the 8 ExifInterface.ORIENTATION_* values; ORIENTATION_NORMAL when unknown. */
            val orientation: Int
            /** Called on a worker thread. [region] is in stored pixels, [sampleSize] a power of two. */
            fun decodeRegion(region: Rect, sampleSize: Int, config: Bitmap.Config, reuse: Bitmap?): Bitmap?
            /** Called on a worker thread before decoding again after the contents changed. */
            fun contentChanged() {}
        }
        ```
  - [x] Task 1.2. KDoc states the contract: `decodeRegion` returns `null` on failure, may ignore `reuse`, must not
        be called concurrently, and the returned bitmap is owned by the caller
- [x] Task 2. Add `ImageOrientation` _(android)_ — implemented, tests added; tests in ImageOrientationTest (100% line, 100% branch coverage), no Lint issues.
  - [x] Task 2.1. (ImageOrientation.kt) Create `ImageOrientation.kt` with an internal object exposing `uprightWidth(orientation, storedW,
        storedH)`, `uprightHeight(…)` and `toMatrix(orientation, storedW, storedH, result: Matrix)`, where the matrix
        maps stored pixels to upright image pixels for the 8 EXIF orientations (normal, flip horizontal, rotate 180,
        flip vertical, transpose, rotate 90, transverse, rotate 270). Unknown values behave as normal
  - [x] Task 2.2. (ImageOrientationTest.kt) Add `ImageOrientationTest` (Robolectric): for each orientation, the four corners of the stored
        rectangle map inside `(0, 0, uprightWidth, uprightHeight)` and a known off-centre point maps to the expected
        upright position
- [x] Task 3. Add `ResolutionPlanner` _(android)_ — implemented in ResolutionPlanner.kt, tests in ResolutionPlannerTest.kt; tests in ResolutionPlannerTest (100% line, 86% branch coverage), no Lint issues.
  - [x] Task 3.1. Create `ResolutionPlanner.kt`, an internal object with pure functions and no Android state:
        `previewSampleSize(imageW, imageH, viewW, viewH)` (largest power of two keeping at least one bitmap pixel per
        view pixel at base scale) and `requiredSampleSize(displayScale)` (largest power of two `≤ 1 / scale`, at
        least 1)
  - [x] Task 3.2. `cachedLevelSampleSizes(imageW, imageH, previewSample, budgetBytes, maxBitmapW, maxBitmapH)`:
        returns the preview sample followed by each halved sample whose bitmap keeps the running total within the
        budget and its sides within the maximum bitmap size. Budget 0 returns the preview only. The preview sample
        itself is increased when the preview would exceed the maximum bitmap size
  - [x] Task 3.3. `levelToDraw(availableSamples, requiredSample)`: the coarsest available sample `≤ required`, or
        the finest available with `needsPatch = true` when none is fine enough
  - [x] Task 3.4. `patchRegion(inverseMatrix, viewW, viewH, storedW, storedH, sample, maxPixels, result: Rect):
        Int`: bounding box of the view rectangle through the inverse of display × orientation matrix, intersected
        with the image, expanded 25% per side, snapped outwards to multiples of the sample, clamped; when over
        `maxPixels`, reduce overscan first, then double the sample. Returns the sample actually used, and an empty
        rect when the image is off screen. `isPatchValid(patchRegion, patchSample, visibleRegion, requiredSample)`
  - [x] Task 3.5. Add `ResolutionPlannerTest` covering each function: powers of two at boundaries, scale above 1,
        budget 0 / default / large, maximum bitmap size, rotated view (45° and 90°), each EXIF orientation,
        pixel-budget reduction order, image fully off screen, coverage check

### Group 2 — Image sources

Parallelizable: yes

- [x] Task 4. Add `RegionDecoderImageSource` and the factories _(android)_ — tests in RegionDecoderImageSourceTest and ImageSourcesTest (RegionDecoderImageSource 95% / ImageSources 96% unit-test line coverage), no Lint issues (Detekt/Spotless/ktlint not wired).
  - [x] Task 4.1. Add `androidx.exifinterface:exifinterface` to `gradle/libs.versions.toml` (version and library
        entry, latest stable) and `implementation(libs.androidx.exifinterface)` to `lib/build.gradle.kts`
  - [x] Task 4.2. Create `RegionDecoderImageSource.kt`: constructed from an opener `() -> InputStream`. On creation
        and on `contentChanged()` it reads the EXIF orientation from one stream and opens a `BitmapRegionDecoder`
        from another (`newInstance(stream)` on API 31 and above, the deprecated `newInstance(stream, false)` below).
        `contentChanged()` closes the old decoder and throws `IllegalStateException` when the new size differs.
        `decodeRegion` sets `inSampleSize`, `inPreferredConfig`, and `inBitmap` only when `reuse` is mutable and the
        config is not `HARDWARE`; it catches `IllegalArgumentException`/`IOException`/`OutOfMemoryError` and returns
        `null`. Creation throws `IOException` with a clear message when the format is not supported
  - [x] Task 4.3. Create `ImageSources.kt` with `fromResource(resources, resId)`, `fromAsset(assets, path)`,
        `fromFile(file)`, `fromUri(contentResolver, uri)`, `fromBytes(bytes)`, each returning an `ImageSource`, and
        an internal `isRegionDecodable(resources, resId): Boolean` (false for XML drawables and nine-patches by
        resource file name from `Resources.getValue`, otherwise a bounds-only decode that must report a JPEG, PNG or
        WebP mime type) used later by `app:src`
  - [x] Task 4.4. Add `RegionDecoderImageSourceTest` and `ImageSourcesTest` (Robolectric) for what the shadows
        support: opener invoked again on `contentChanged`, size mismatch, close, `isRegionDecodable` for an XML
        drawable and a raster resource placed under `lib/src/test/res`. Real decoding is covered by Task 12
- [x] Task 5. Add `BitmapImageSource` _(android)_ — tests in BitmapImageSourceTest (88% unit-test line coverage; HARDWARE copy path not unit-testable under Robolectric), no Lint issues.
  - [x] Task 5.1. Create `BitmapImageSource.kt`: wraps a caller-owned bitmap; `orientation` is normal;
        `decodeRegion` draws the region scaled by `1 / sampleSize` into `reuse` when usable or into a new
        `ARGB_8888` bitmap, and returns a `HARDWARE` copy of it when that config is requested; `close()` does not
        recycle the wrapped bitmap. Reads of the wrapped bitmap are synchronised on it, and the KDoc tells callers
        to synchronise on the same bitmap while drawing on it
  - [x] Task 5.2. Add `BitmapImageSourceTest`: size, region and sample of the result, reuse, edits to the wrapped
        bitmap visible in the next decode, wrapped bitmap not recycled after `close()`

### Group 3 — Image coordinate space and the multi-resolution engine

Parallelizable: yes

- [x] Task 6. Decouple the view's coordinate space from the bitmap being drawn _(android)_ — implemented in `GestureBitmapView.kt`, tests added to `GestureBitmapViewTest.kt` (GestureBitmapView 99.6% unit-test line coverage; one existing test, `onDraw_whenNonRecycledBitmapWithCanvas_drawsBitmap`, had to be adapted because Task 6.3 mandates `save`/`concat`/`restore` and the 4-argument `drawBitmap`, which its strict mock could not answer). Lint: one new `UseKtx` warning on `canvas.save()` left in place as unavoidable, since Task 6.3 specifies `save`/`concat`/`restore` explicitly.
  - [x] Task 6.1. In `GestureBitmapView.kt` add private `imageWidth`, `imageHeight` (upright size, 0 when there is
        no image), `orientationMatrix` and preallocated `RectF` destination fields. `setBitmap` sets the image size
        from the bitmap and resets `orientationMatrix` on every call, with or without `resetMatrices`, so today's
        behaviour is unchanged
  - [x] Task 6.2. `computeBaseMatrix` and `currentBitmapDisplayedRect` take the image size instead of a `Bitmap`;
        update their callers (`setBitmap`, `displayType` setter, `onSizeChanged`, `displayRect`)
  - [x] Task 6.3. `onDraw`: `save`, `concat(displayMatrix)`, `concat(orientationMatrix)`, `drawBitmap(bitmap, null,
        destinationRect, bitmapPaint)`, `restore`. No allocations
  - [x] Task 6.4. `bitmapPaint.isFilterBitmap = true`. Update the KDoc of `displayTransformationMatrix`,
        `baseTransformationMatrix`, `transformationMatrix`, `displayRect` and their parameter variants to say
        "image pixels"
  - [x] Task 6.5. In `GestureBitmapViewTest` (unit) add tests proving nothing changed for the bitmap API: base
        matrix and `displayRect` per display type, `setBitmap(other, resetMatrices = false)` with a bitmap of a
        different size keeps `paramsMatrix` and `baseMatrix` and uses the new size for `displayRect`, and saved
        state from before this change restores. Existing tests must pass unmodified
- [x] Task 7. Add `MultiResolutionImage` _(android)_ — implemented (MultiResolutionImage.kt, MultiResolutionImageTest.kt); 31 tests in MultiResolutionImageTest, 92.5% unit-test line coverage (69.7% branch), no new Lint issues.
  - [x] Task 7.1. Create `MultiResolutionImage.kt`, internal, constructed with an `ImageSource`, an `Executor` for
        decoding (default: a single-thread executor it owns), a `(Runnable) -> Unit` to post to the UI thread, and
        callbacks `onChanged()`, `onPreviewLoaded()`, `onDetailLoaded()`, `onFailed(Throwable?)`. It never decodes on
        the calling thread. Every request carries a generation number; results of an older generation are recycled
        and dropped
  - [x] Task 7.2. Levels: `configure(viewW, viewH, budgetBytes, maxBitmapW, maxBitmapH, hardware: Boolean,
        regionDecoding: Boolean)` computes the level samples with `ResolutionPlanner`, decodes the preview first and
        then the remaining levels coarse to fine, each as a whole-image `decodeRegion`. With `regionDecoding =
        false` it decodes one level at sample 1 and reports failure when it exceeds the maximum bitmap size.
        Reconfiguring with a finer preview sample decodes again and keeps the old bitmaps until replaced
  - [x] Task 7.3. `update(inverseDisplayTimesOrientation: Matrix, displayScale, viewW, viewH, scaling: Boolean)`:
        picks the level to draw with `levelToDraw` (no decode when a cached level is enough, and the patch is
        released), otherwise plans the patch and requests it when the current one is not valid. One decode in
        flight, always for the latest state; while `scaling` is true a patch with a different sample is deferred,
        coverage misses are requested at once; patch requests go ahead of pending level prefetches
  - [x] Task 7.4. Software mode: front and back `ARGB_8888` patch bitmaps passed as `reuse`. Hardware mode:
        `Bitmap.Config.HARDWARE`, no reuse, previous bitmap recycled after the swap
  - [x] Task 7.5. Drawing state read by the view on the UI thread only: `levelBitmap`, `levelSample`, `patchBitmap`,
        `patchRegion` (stored pixels), `hasAlpha`. `invalidate(region: Rect?)` (stored pixels): merges pending
        regions, calls `source.contentChanged()` on the worker, then decodes again every level and the patch that
        intersect the region; in software mode it decodes only the region and copies it into the level on the UI
        thread, in hardware mode it decodes the level whole; old bitmaps stay until replaced; an exception from
        `contentChanged()` is reported through `onFailed`
  - [x] Task 7.6. `trimLevels()` (drops every level above the preview and the patch), `release()` (cancels,
        recycles everything it decoded, shuts down its own executor; does not close the source) and `close()`
        (`release()` plus closing the source)
  - [x] Task 7.7. Add `MultiResolutionImageTest` with a fake `ImageSource` recording its calls and a direct
        executor: preview then levels in order; budget 0; level swap with no decode request; patch only beyond the
        finest level; stale generation dropped and recycled; deferral while scaling; coverage miss; patch released
        when zooming out; `invalidate` whole and region with merged calls in both modes; size change reported;
        `trimLevels` then decode again on demand; `regionDecoding = false`; nothing decoded when the executor does
        not run; `release` and `close`

### Group 4 — View integration

Parallelizable: no — Task 8, Task 9, Task 10 and Task 11 all edit `GestureBitmapView.kt` and `GestureBitmapViewTest.kt`

- [x] Task 8. Use `MultiResolutionImage` from `GestureBitmapView` _(android)_ — GestureBitmapView.kt, attrs.xml; tests in GestureBitmapViewImageSourceTest (new, NATIVE graphics mode) and GestureBitmapViewTest (updated); GestureBitmapView 98% line / 86% branch unit coverage; Lint 0 issues (UseKtx fixed with withSave). Hardware-to-software copy branch not unit-testable on the JVM.
  - [x] Task 8.1. Public API: `var imageSource: ImageSource?`, `setImageSource(source, resetMatrices = true)`,
        `setImageResource(resId, resetMatrices = true)`, `setImageUri(uri, resetMatrices = true)`,
        `setImageFile(file, resetMatrices = true)`. Setting a source recycles and clears `_bitmap`, sets the image
        size and `orientationMatrix` from the source through `ImageOrientation`, computes the base matrix, and
        creates a `MultiResolutionImage`. Setting `bitmap` closes and clears the source. The `bitmap` getter returns
        `null` while a source is set. A source the view created itself is closed when replaced; one passed to
        `setImageSource` is closed too, as documented in its KDoc
  - [x] Task 8.2. Properties `regionDecodingEnabled` (default `true`), `hardwareBitmapsEnabled` (default `true`) and
        `cachedLevelsMemoryBudget` (bytes; default sentinel meaning 4 × view pixels × 4; negative values rejected
        with `IllegalArgumentException`). Changing one reconfigures the current image. Hardware mode is used only
        when the flag is true and `isHardwareAccelerated` is true
  - [x] Task 8.3. Call `configure` on the first valid size (`onSizeChanged`), on `onAttachedToWindow`, and when a
        source is set on a laid-out view, passing `Canvas.maximumBitmapWidth`/`Height` captured in `onDraw` (a
        conservative constant before the first draw)
  - [x] Task 8.4. `updateDisplayMatrix()` computes the inverse of display × orientation into a preallocated matrix
        and calls `update(…)`, with `scaling = true` while a scale gesture is in progress or `scaleAnimator` is
        running. Call `update` again with `scaling = false` at gesture end (`ACTION_UP`/`ACTION_CANCEL`) and at the
        end of each animator. `onChanged` calls `invalidate()`
  - [x] Task 8.5. `onDraw` with a source: inside `concat(displayMatrix)` and `concat(orientationMatrix)`, when the
        image has alpha and there is a patch `clipOutRect(patchRegion)` inside a nested `save`/`restore`, draw the
        level into the whole stored rectangle, then the patch into its region. When the canvas is not hardware
        accelerated and a bitmap is `HARDWARE`, draw a software copy made for that draw
  - [x] Task 8.6. Lifecycle and state: `onDetachedFromWindow` calls `release()`, `onAttachedToWindow` configures
        again; register a `ComponentCallbacks2` on the context while attached and call `trimLevels()` from
        `onTrimMemory`. Save and restore the three new properties with new bundle keys read with their defaults, so
        an older saved state still restores. The source is not saved
  - [x] Task 8.7. `OnImageLoadListener` nested interface (`onPreviewLoaded(view)`, `onDetailLoaded(view)`,
        `onImageLoadFailed(view, error: Throwable?)`) and `imageLoadListener` property, in the style of the existing
        listeners
  - [x] Task 8.8. Unit tests in `GestureBitmapViewTest` with a fake source and a direct executor (the executor is
        replaceable through an internal property): each setter; `bitmap` and source mutually exclusive; matrices
        unchanged when a patch appears and when it is released; no patch requested between cached levels; flags and
        budget; saved state with and without the new keys; detach and attach; trim memory; listener calls; alpha
        clip; software canvas in hardware mode does not throw
- [x] Task 9. XML attributes and `app:src` _(android)_ — tests in GestureBitmapViewImageSourceTest/GestureBitmapViewTest; test_vector.xml given an explicit size.
  - [x] Task 9.1. `lib/src/main/res/values/attrs.xml`: add `regionDecodingEnabled` (boolean),
        `hardwareBitmapsEnabled` (boolean) and `cachedLevelsMemoryBudget` (integer)
  - [x] Task 9.2. `init`: read the three attributes before `src`. For `src`, read the resource id with
        `getResourceId`; when `regionDecodingEnabled` is true and `ImageSources.isRegionDecodable` is true call
        `setImageResource`, otherwise keep `getDrawable` + `setDrawable`. The automatic path must not decode the
        whole bitmap
  - [x] Task 9.3. Unit tests: raster `src` uses a source; XML drawable `src` and `regionDecodingEnabled="false"`
        use the drawable path with the same resulting bitmap size as before; attribute values reach the properties
- [x] Task 10. Content-changed method _(android)_ — tests in GestureBitmapViewImageSourceTest.
  - [x] Task 10.1. `fun invalidateImage(region: Rect? = null)`: with a source, maps the upright region to stored
        pixels through the inverse of `orientationMatrix` (preallocated) and calls `MultiResolutionImage.invalidate`;
        with a plain bitmap it calls `invalidate()`; with no image it does nothing
  - [x] Task 10.2. Unit tests: whole image and region for several EXIF orientations, repeated calls, bitmap path
        (edit a mutable bitmap, call `invalidateImage()`, the next draw uses the edited bitmap), failure reported
        through `onImageLoadFailed` when the size changed
- [x] Task 11. Overlay drawing hook and touch mapping _(android)_ — tests in GestureBitmapViewImageSourceTest.
  - [x] Task 11.1. Nested interface `ImageOverlayDrawer { fun onDrawOverImage(view: GestureBitmapView, canvas:
        Canvas) }` and `imageOverlayDrawer` property. In `onDraw`, after the image and after undoing
        `orientationMatrix` (so the canvas is in upright image pixels), `clipRect(0, 0, imageWidth, imageHeight)` and
        call it. It is called for the bitmap path and the source path, and not when there is no image
  - [x] Task 11.2. `fun mapViewToImage(x: Float, y: Float, result: PointF): Boolean` using a preallocated inverse of
        `displayMatrix`; returns `false` when there is no image or the matrix cannot be inverted
  - [x] Task 11.3. Unit tests: drawer called once per draw with a canvas whose matrix equals `displayMatrix` and
        whose clip is the image rectangle, for several scales, rotations and EXIF orientations, on both paths;
        `mapViewToImage` round trip with `displayTransformationMatrix`

### Group 5 — Instrumented tests and sample check

Parallelizable: no — Task 13 records the outcome of Task 12

- [x] Task 12. Instrumented tests with the real decoder _(android)_ — 38/38 instrumented tests pass on Pixel_9_Pro (API 36), 0 skipped; no new files in `main`, so unit coverage unchanged.
  - [x] Task 12.1. Add test images under `lib/src/androidTest/assets`: a JPEG of about 2000×1500 with a
        recognisable pattern (a different colour per quadrant), copies of it tagged with EXIF orientations 3, 6 and
        8, a PNG with alpha, and a text file with an image extension. Generate them with a small script or at test
        set-up time; keep the total under 1 MB — assets in `lib/src/androidTest/assets` (quadrants.jpg, orientation 3/6/8 copies, alpha.png, not_an_image.jpg; about 380 KB)

  - [x] Task 12.2. New `RegionDecoderImageSourceTest` under
        `lib/src/androidTest/java/com/irurueta/android/gesturebitmap/test/`: stored size and orientation; size and
        content of a decoded region for sample sizes 1, 2 and 4; `inBitmap` reuse returns the same instance;
        `HARDWARE` config returns a hardware bitmap (if it does not on the running API level, the test documents
        the level and `GestureBitmapView` forces software mode below a private constant API level); unsupported
        format throws; for each EXIF asset, the quadrant colours of the preview and of a patch drawn by the view
        match the upright image — `RegionDecoderImageSourceTest`, helpers `ImageLoadRecorder` and `GestureBitmapViewTestHelper`

  - [x] Task 12.3. Extend the instrumented `GestureBitmapViewTest`: set a source, wait for `onPreviewLoaded`, set
        `transformationParameters` to a high scale, wait for `onDetailLoaded`, check `displayTransformationMatrix`
        is unchanged; draw the view on a software canvas in hardware mode without an exception; `invalidateImage`
        after editing a `BitmapImageSource` bitmap changes the drawn pixels at low and high scale — three tests added to `GestureBitmapViewTest`; the two layout-xml tests now expect `imageSource` instead of `bitmap` because `app:src` takes the automatic path

  - [x] Task 12.4. Run the instrumented tests on the `Pixel_9_Pro` emulator by delegating to the `iru-gate-runner`
        agent (`iru-android-test` with the device option). Then install the sample app on the emulator and confirm
        from the `OnImageLoadListener` log output, or a screenshot at maximum zoom, that `app:src` used the
        automatic path. If the emulator cannot be started, report both as not run instead of marking the task done — emulator ran the suite; sample app check done with temporary logging (reverted): `imageSource=RegionDecoderImageSource bitmap=null`, then `onPreviewLoaded`, `onDetailLoaded` at scale 10

- [x] Task 13. Record the device measurements as a manual follow-up
  - [x] Task 13.1. Write, for the pull request description, the exact procedure of issue steps 0 and 8 (`adb shell
        dumpsys meminfo`, `dumpsys gfxinfo framestats`; drag, pinch and rotate at minimum and maximum scale; with
        `hardwareBitmapsEnabled` on and off and with `regionDecodingEnabled` off) and state that it has not been
        run. Include whether `HARDWARE` region decoding worked on the emulator API level (Task 12.2)

  #### Manual device measurements (copy into the pull request description)

  **Not run in this pipeline.** Issue #3 steps 0 and 8 need a physical device and were not executed. The Task 12
  instrumented suite did run on the `Pixel_9_Pro` emulator (API 36): 38 passed, 0 failed, 0 skipped.
  `BitmapRegionDecoder` honoured `Bitmap.Config.HARDWARE` there (logcat tag `RegionDecoderTest`: "HARDWARE region
  decoding worked=true on API 36"), so `GestureBitmapView` needs no software-mode fallback on that level; other API
  levels were not checked.

  Procedure (sample app `com.irurueta.android.gesturebitmap.app`, 4032x3024 JPEG via `app:src`):
  1. `adb shell am force-stop com.irurueta.android.gesturebitmap.app`, then start the activity and wait 5 s.
  2. Memory: `adb shell dumpsys meminfo com.irurueta.android.gesturebitmap.app` at minimum scale, at maximum scale
     and after a rotation; note Java Heap, Native Heap, Graphics and TOTAL PSS.
  3. Frames: `adb shell dumpsys gfxinfo com.irurueta.android.gesturebitmap.app reset`, perform the gesture, then
     `adb shell dumpsys gfxinfo com.irurueta.android.gesturebitmap.app framestats`; note janky frame percentage and
     the 90th/95th/99th percentile frame times.
  4. Gestures, each at minimum scale and at maximum scale: drag, pinch and rotate (use a real device; `adb input`
     cannot pinch or rotate).
  5. Repeat steps 2 to 4 for three configurations: default (`hardwareBitmapsEnabled` on, `regionDecodingEnabled` on),
     `hardwareBitmapsEnabled = false`, and `regionDecodingEnabled = false` (set from code, for example in
     `MainActivity.onCreate`, without committing the change).
  6. Compare the three configurations, and the numbers against the same procedure on `main`, in a table in the PR.

### Group 6 — Documentation

Parallelizable: no — Task 15 uses the examples verified by Task 14

- [x] Task 14. Compiled documentation examples _(android)_
  - Note: DocumentationExamplesTest 14/14 pass; test-only file, no main coverage change.
  - [x] Task 14.1. Add `lib/src/test/java/com/irurueta/android/gesturebitmap/DocumentationExamplesTest.kt`
        (Robolectric) with one test per documented case, each containing the exact snippet the docs will show, marked
        with `// tag::name[]` and `// end::name[]` comments: `setImageResource`, `setImageUri`, `setImageFile`,
        `setImageSource` with a factory, a custom `ImageSource`, `regionDecodingEnabled = false`, the bitmap path,
        `cachedLevelsMemoryBudget` values, `hardwareBitmapsEnabled`, `OnImageLoadListener`, a mutable bitmap with
        `invalidateImage()`, `BitmapImageSource` with `invalidateImage(region)`, a rewritten file with
        `invalidateImage()`, and a complete draw mode (gestures disabled, `OnTouchListener`, `mapViewToImage`,
        strokes as paths in the overlay drawer, commit with `invalidateImage(region)`)
- [x] Task 15. Write the documentation
  - Note: the docs component cannot reach the test file (outside the Antora start_path), so the snippets are copies of the tagged regions of DocumentationExamplesTest.kt, stored in docs/modules/ROOT/examples/*.kt and included with `include::example$...[]`. Antora build clean.
  - [x] Task 15.1. `docs/modules/ROOT/pages/usage.adoc`: replace the tip at lines 76 to 81 with a "Large images"
        section explaining preview, cached levels and patch, with the XML example (`app:src` and the three new
        attributes), every code example from Task 14 that sets an image or a flag, EXIF behaviour, and the two
        `app:src` differences (EXIF applied; image size is the file's pixel size, visible only with
        `DisplayType.NONE` and `FIT_IF_BIGGER`)
  - [x] Task 15.2. New `docs/modules/ROOT/pages/editing.adoc` ("Updating image contents"), added to
        `docs/modules/ROOT/nav.adoc` after Usage: a table to choose between the three ways (memory, cost of an
        update, when to use) and the four content-update examples from Task 14, including the complete draw mode
  - [x] Task 15.3. `configuration.adoc`, `reference.adoc`, `sample-app.adoc`: every new method, property, XML
        attribute, interface and listener
  - [x] Task 15.4. `lib/module.md`, `README.md` (short example of the automatic path and links to both pages) and
        `CHANGELOG.md` (Added: multi-resolution display, new API; Changed: `app:src` on raster resources)
  - [x] Task 15.5. Run `/iru-update-docs` and build the site with `/iru-build-docs`; fix any broken xref or include
