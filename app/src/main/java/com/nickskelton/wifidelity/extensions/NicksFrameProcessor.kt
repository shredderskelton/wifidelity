package com.nickskelton.wifidelity.extensions

import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import io.fotoapparat.preview.Frame
import io.fotoapparat.util.FrameProcessor
import timber.log.Timber
import java.lang.StringBuilder

class NicksFrameProcessor(private val listener: (Frame, FirebaseVisionText) -> Unit) : FrameProcessor {
//    private val performanceLogger = PerformanceLogger("ImageProcessing")

    private val detector: FirebaseVisionTextRecognizer by lazy { FirebaseVision.getInstance().onDeviceTextRecognizer }

    private var busy = false

    override fun invoke(frame: Frame) {
        //Timber.v("frame: ${frame.image.size}, ${frame.size}")
        if (busy) {
            //Timber.d("Dropped frame")
            return
        }

        busy = true

        val meta = FirebaseVisionImageMetadata.Builder()
        meta.setRotation(FirebaseVisionImageMetadata.ROTATION_90)
        meta.setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
        meta.setWidth(frame.size.width)
        meta.setHeight(frame.size.height)

        val image = FirebaseVisionImage.fromByteArray(frame.image, meta.build())
        detector.processImage(image)
            .addOnSuccessListener { result ->
                //                Timber.i("Success: ${it.text}")

                val sb = StringBuilder()

                result.textBlocks.forEach { block ->
                    block.lines.forEach {
                        sb.append(it.text)
                    }
                }

                listener.invoke(sb.toString())
                Timber.d(sb.toString())
//                performanceLogger.addSplit("Success")

            }
            .addOnFailureListener {
                //Timber.i("Failed: ${it.localizedMessage}")
//                performanceLogger.addSplit("Failed")
            }
            .addOnCompleteListener {
                //Timber.i("Completed")
                busy = false
//                performanceLogger.addSplit("Completed")
//                performanceLogger.dumpToLog()
            }
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
//        yuvImage.compressToJpeg(
//            Rect(0, 0, frame.size.width, frame.size.height),
//            100,
//            output
//        )
//
//        val bitmap: Bitmap = BitmapFactory.decodeByteArray(output.toByteArray(), 0, output.size())
//        bitmap.process(
//            bitmap.rotate(90f).flip(
//                false, activeCamera == Camera.Front
//            )
//        )
//        bitmap.recycle()
    }
}