// the image to draw on: the view displays it and the caller keeps ownership of the bitmap
view.setImageSource(BitmapImageSource(bitmap))

// gestures must not compete with drawing
view.scrollEnabled = false
view.scaleEnabled = false
view.rotationEnabled = false
view.doubleTapEnabled = false

val paint = Paint().apply {
    color = Color.RED
    style = Paint.Style.STROKE
    strokeWidth = 8.0f
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    isAntiAlias = true
}
var stroke: Path? = null // stroke being drawn, in upright image pixels
val point = PointF()

// the stroke in progress is drawn above the image, in image pixels
view.imageOverlayDrawer = object : GestureBitmapView.ImageOverlayDrawer {
    override fun onDrawOverImage(view: GestureBitmapView, canvas: Canvas) {
        stroke?.let { canvas.drawPath(it, paint) }
    }
}

view.setOnTouchListener { v, event ->
    // convert the touch position from view pixels to image pixels
    if (!view.mapViewToImage(event.x, event.y, point)) {
        return@setOnTouchListener false
    }
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            stroke = Path().apply { moveTo(point.x, point.y) }
        }
        MotionEvent.ACTION_MOVE -> {
            stroke?.lineTo(point.x, point.y)
            v.invalidate()
        }
        MotionEvent.ACTION_UP -> {
            stroke?.let { finished ->
                finished.lineTo(point.x, point.y)

                // commit the stroke to the bitmap
                synchronized(bitmap) {
                    Canvas(bitmap).drawPath(finished, paint)
                }

                // refresh only the region touched by the stroke
                val bounds = RectF()
                finished.computeBounds(bounds, true)
                bounds.inset(-paint.strokeWidth, -paint.strokeWidth)
                val region = Rect()
                bounds.roundOut(region)
                view.invalidateImage(region)
            }
            stroke = null
            v.invalidate()
        }
    }
    true
}
