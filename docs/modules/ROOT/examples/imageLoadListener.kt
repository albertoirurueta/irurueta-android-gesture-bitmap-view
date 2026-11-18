view.imageLoadListener = object : GestureBitmapView.OnImageLoadListener {
    override fun onPreviewLoaded(view: GestureBitmapView) {
        // the whole image is visible, at low resolution
    }

    override fun onDetailLoaded(view: GestureBitmapView) {
        // a sharper level or a patch has been decoded
    }

    override fun onImageLoadFailed(view: GestureBitmapView, error: Throwable?) {
        Log.e(TAG, "Image could not be decoded", error)
    }
}
