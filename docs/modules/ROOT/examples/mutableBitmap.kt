val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
view.bitmap = bitmap

// edit the bitmap, then tell the view that its contents changed
Canvas(bitmap).drawColor(Color.RED)
view.invalidateImage()
