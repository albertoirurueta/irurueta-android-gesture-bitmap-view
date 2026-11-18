// Generates a 20000 x 20000 pixels image procedurally. It is never held whole in memory.
class CheckerboardImageSource : ImageSource {
    override val storedWidth = 20000
    override val storedHeight = 20000
    override val orientation = ExifInterface.ORIENTATION_NORMAL

    // called on a worker thread, never concurrently with itself
    override fun decodeRegion(
        region: Rect,
        sampleSize: Int,
        config: Bitmap.Config,
        reuse: Bitmap?
    ): Bitmap? {
        val width = (region.width() + sampleSize - 1) / sampleSize
        val height = (region.height() + sampleSize - 1) / sampleSize
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val column = (region.left + x * sampleSize) / 1000
                val row = (region.top + y * sampleSize) / 1000
                val color = if ((column + row) % 2 == 0) Color.DKGRAY else Color.LTGRAY
                result.setPixel(x, y, color)
            }
        }
        return result
    }

    override fun close() {
        // release anything held by the source
    }
}

view.setImageSource(CheckerboardImageSource())
