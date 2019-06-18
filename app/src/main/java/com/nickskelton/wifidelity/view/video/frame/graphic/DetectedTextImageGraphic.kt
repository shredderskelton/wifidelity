package com.nickskelton.wifidelity.view.video.frame.graphic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.nickskelton.wifidelity.view.video.GraphicOverlayView

open class DetectedTextImageGraphic(
    overlayView: GraphicOverlayView,
    private val line: FirebaseVisionText.Line,
    private val originalBitmap: Bitmap
) : GraphicOverlayView.Graphic(overlayView) {

    fun shouldHit(x: Float, y: Float): Boolean {
        val convertedRect = line.boundingBox?.let {
            translateRectF(RectF(it))
        } ?: return false
        return convertedRect.contains(x, y)
    }

    val text: String = line.text

    override fun draw(canvas: Canvas) {
        line.boundingBox?.let {
            canvas.drawBitmap(originalBitmap, it, translateRectF(RectF(it)), null)
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