package com.nickskelton.wifidelity.view.video.frame.graphic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.nickskelton.wifidelity.view.video.GraphicOverlayView

open class TextBoxGraphic(
    overlayView: GraphicOverlayView,
    private val line: FirebaseVisionText.Line
) : GraphicOverlayView.Graphic(overlayView) {

    protected open val paint: Paint get() = whitePaint

    private val detectionBox = line.boundingBox?.let {
        translateRectF(RectF(it))
    }

    override fun draw(canvas: Canvas) {
        detectionBox?.let {
            canvas.drawRect(it, paint)
        }
    }

    companion object {
        private val whitePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }
}