package com.nickskelton.wifidelity.view.video.frame.processor

import android.graphics.Bitmap
import com.google.firebase.ml.common.FirebaseMLException
import com.nickskelton.wifidelity.common.FrameMetadata
import com.nickskelton.wifidelity.view.video.GraphicOverlayView
import java.nio.ByteBuffer

/** An inferface to process the images with different ML Kit detectors and custom image models.  */
interface VisionImageProcessor {
    /** Processes the images with the underlying machine learning models.  */
    @Throws(FirebaseMLException::class)
    fun process(data: ByteBuffer, frameMetadata: FrameMetadata, graphicOverlayView: GraphicOverlayView)

    /** Processes the bitmap images.  */
    fun process(bitmap: Bitmap, graphicOverlayView: GraphicOverlayView)

    /** Stops the underlying machine learning model and release resources.  */
    fun stop()

    val busy: Boolean
}

