package com.nickskelton.wifidelity.view.video.frame.graphic

import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.nickskelton.wifidelity.view.video.GraphicOverlayView

class HighlightedTextBoxGraphic(
    overlayView: GraphicOverlayView,
    line: FirebaseVisionText.Line
) : TextBoxGraphic(overlayView, line) {
    override val paint get() = redPaint

    companion object {
        private val redPaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 15f
        }
    }

}