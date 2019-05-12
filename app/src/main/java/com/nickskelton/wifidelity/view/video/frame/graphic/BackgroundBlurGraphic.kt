package com.nickskelton.wifidelity.video.frame.graphic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Script
import android.renderscript.ScriptIntrinsicBlur
import com.nickskelton.wifidelity.view.video.GraphicOverlayView

private const val BLUR_RADIUS: Float = 25f

class BackgroundBlurGraphic(
    private val context: Context,
    overlayView: GraphicOverlayView,
    private val bitmap: Bitmap
) :
    GraphicOverlayView.Graphic(overlayView) {
    override fun draw(canvas: Canvas) {
        canvas.drawBitmap(
            blur(bitmap), null,
            Rect(0, 0, canvas.width, canvas.height), null
        )
    }

    private fun blur(image: Bitmap): Bitmap {
        val outputBitmap = Bitmap.createBitmap(image)
        val renderScript = RenderScript.create(context)
        val tmpIn = Allocation.createFromBitmap(renderScript, image)
        val tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap)
        val theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut, launchOptions)
        tmpOut.copyTo(outputBitmap)
        return outputBitmap
    }

    private val launchOptions: Script.LaunchOptions
        get() {
            val limit = Rect(0, 0, bitmap.width, bitmap.height)
            val bounds = with(limit) {
                Rect(left, top, right, bottom)
            }
            return Script.LaunchOptions().apply {
                setX(bounds.left, bounds.right)
                setY(bounds.top, bounds.bottom)
            }
        }
}