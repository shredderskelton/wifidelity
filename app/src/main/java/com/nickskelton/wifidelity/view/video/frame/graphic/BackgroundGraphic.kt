package com.nickskelton.wifidelity.video.frame.graphic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.nickskelton.wifidelity.view.video.GraphicOverlayView

class BackgroundGraphic(
    overlayView: GraphicOverlayView,
    private val bitmap: Bitmap
) :
    GraphicOverlayView.Graphic(overlayView) {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(
            bitmap, null,
            Rect(0, 0, canvas.width, canvas.height), null
        )
    }
}