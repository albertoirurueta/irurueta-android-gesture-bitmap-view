# Module irurueta-android-gesture-bitmap-view

A touch gesture capable Android view to display bitmaps and perform scaling, translation and
rotation on the bitmap. The module provides the gesture view, its gesture detectors and the
transformation parameters used to report scaling and rotation changes.

# Package com.irurueta.android.gesturebitmap

Contains the touch gesture capable bitmap view and the gesture detectors used to recognize
rotation gestures performed with two fingers, reporting angle variations, pivot points and
angular velocity to registered listeners.

It also defines `ImageSource`, the provider of large images that are decoded one region at a time
at a reduced resolution, so that they can be displayed without loading them fully into memory.
`ImageSources` creates such sources from drawable resources, assets, files, content URIs or encoded
bytes, while `BitmapImageSource` wraps a bitmap that is already held in memory.

`GestureBitmapView` decodes such images in several resolutions (a preview, cached levels and a detail patch for the
visible region) and exposes `setImageSource`, `setImageResource`, `setImageUri`, `setImageFile`,
`regionDecodingEnabled`, `hardwareBitmapsEnabled`, `cachedLevelsMemoryBudget` and `imageLoadListener` to control it.
`invalidateImage(region)` refreshes the image after its contents change, `imageOverlayDrawer` draws on top of it in
image pixels and `mapViewToImage` converts view coordinates into image pixels.
