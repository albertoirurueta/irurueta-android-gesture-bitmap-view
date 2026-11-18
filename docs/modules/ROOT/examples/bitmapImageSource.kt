// the bitmap stays owned by the caller: the view never recycles it
view.setImageSource(BitmapImageSource(bitmap))

// draw on the bitmap, synchronizing on it, and refresh only the edited region
val edited = Rect(100, 100, 300, 300) // upright image pixels
synchronized(bitmap) {
    val paint = Paint().apply { color = Color.RED }
    Canvas(bitmap).drawRect(edited, paint)
}
view.invalidateImage(edited)
