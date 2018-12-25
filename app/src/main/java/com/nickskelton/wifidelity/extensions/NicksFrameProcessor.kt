package com.nickskelton.wifidelity.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.nickskelton.wifidelity.view.DetectionOverlay
import io.fotoapparat.preview.Frame
import io.fotoapparat.util.FrameProcessor
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.StringBuilder

class NicksFrameProcessor(val overlay: DetectionOverlay) : FrameProcessor {
    private val detector: FirebaseVisionTextRecognizer by lazy { FirebaseVision.getInstance().onDeviceTextRecognizer }
    private var busy = false
    private var droppedFrameRate = 0
    private var lastFrame: Frame? = null

    var lastResults : List<String> = emptyList()

    override fun invoke(frame: Frame) {
        lastFrame = frame

        if (busy) {
            droppedFrameRate++
            return
        }
        busy = true
        Timber.d("Dropped Frames: $droppedFrameRate")
        droppedFrameRate = 0

        val meta = FirebaseVisionImageMetadata.Builder()
        meta.setRotation(FirebaseVisionImageMetadata.ROTATION_90)
        meta.setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        meta.setWidth(frame.size.width)
        meta.setHeight(frame.size.height)

        Timber.d("Frame size: ${frame.size}, Control size: w=${overlay.width}, h=${overlay.height}")

        val widthScale: Float = overlay.width.toFloat() / frame.size.height.toFloat()
        val heightScale: Float = overlay.height.toFloat() / frame.size.width.toFloat()

        Timber.d("Scale: $widthScale, $heightScale")

        val image = FirebaseVisionImage.fromByteArray(frame.image, meta.build())
        detector.processImage(image)
            .addOnSuccessListener { result ->
                val sb = StringBuilder()
                result.textBlocks.forEach { block ->
                    block.lines.forEach {
                        sb.append("${it.text} \n")
                    }
                }

                lastResults = result.textBlocks
                    .flatMap {
                        it.lines
                    }.map {
                        it.text
                    }


                overlay.rectangles = result.textBlocks
                    .flatMap {
                        it.lines
                    }
                    .map { line ->
                        Timber.d("Line: ${line.text}= ${line.boundingBox}")
                        line.boundingBox?.let {
                            RectF(
                                it.left.toFloat() * widthScale,
                                it.top.toFloat() * heightScale,
                                it.right.toFloat() * widthScale,
                                it.bottom.toFloat() * heightScale
                            )
                        }
                    }
                    .toTypedArray()

            }
            .addOnFailureListener {
                Timber.e("Failed: ${it.localizedMessage}")
            }
            .addOnCompleteListener {
                busy = false
            }
    }
//
//    fun takePicture(): Bitmap? {
//
//        val frame = lastFrame ?: return null
//
//        val yuvImage = YuvImage(
//            frame.image,
//            ImageFormat.NV21,
//            frame.size.width,
//            frame.size.height,
//            null
//        )
//
//        val output = ByteArrayOutputStream()
//
//            .compressToJpeg(
//                Rect(0, 0, frame.size.width, frame.size.height),
//                100,
//                output
//            )
//
//        return BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
//    }
//
//    private fun YuvImage.rotate90(): ByteArray {
//
//        val yuv = ByteArray(width * height * 3 / 2)
//        // Rotate the Y luma
//        var i = 0
//        for (x in 0 until width) {
//            for (y in height - 1 downTo 0) {
//                yuv[i] = yuvData[y * width + x]
//                i++
//            }
//        }
//        // Rotate the U and V color components
//        i = width * height * 3 / 2 - 1
//        var x = width - 1
//        while (x > 0) {
//            for (y in 0 until height / 2) {
//                yuv[i] = yuvData[width * height + y * width + x]
//                i--
//                yuv[i] = yuvData[width * height + y * width + (x - 1)]
//                i--
//            }
//            x -= 2
//        }
//        return yuv
//    }
}