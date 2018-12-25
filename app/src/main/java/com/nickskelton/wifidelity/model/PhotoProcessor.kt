package com.nickskelton.wifidelity.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.nickskelton.wifidelity.utils.PerformanceLogger
import io.fotoapparat.result.Photo
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.BehaviorSubject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber
import android.graphics.BitmapFactory
import android.graphics.ImageFormat.NV21
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata.IMAGE_FORMAT_YV12

class PhotoProcessor(private val photo: Photo) : KoinComponent {

    val context: Context by inject()

    private var bitmap = BitmapFactory.decodeByteArray(photo.encodedImage, 0, photo.encodedImage.size)

    private val performanceLogger = PerformanceLogger("ByteArrayProcessing")

    private val detector: FirebaseVisionTextRecognizer by lazy { FirebaseVision.getInstance().onDeviceTextRecognizer }

    val results: BehaviorSubject<DetectionResult> = BehaviorSubject.create<DetectionResult>()

    private val resultsObservable: Observable<DetectionResult> = Observable.create<DetectionResult> { emitter ->
        performanceLogger.reset()
        performanceLogger.addSplit("Started")
        emitter.onNext(DetectionResult.Loading())
        Timber.i("Loading")
        val meta = FirebaseVisionImageMetadata.Builder()
//        meta.setHeight(photo.height)
//        meta.setWidth(photo.width)
        meta.setRotation(convertToFirebaseRotation(photo.rotationDegrees))
        meta.setFormat(IMAGE_FORMAT_YV12)
        val image = FirebaseVisionImage.fromByteArray(photo.encodedImage, meta.build())
        detector.processImage(image)
            .addOnSuccessListener {
                Timber.i("Success")
                performanceLogger.addSplit("Success")
                onSuccess(emitter, it)
            }
            .addOnFailureListener {
                Timber.i("Failed")
                performanceLogger.addSplit("Failed")
                onFailure(emitter, it)
            }
            .addOnCompleteListener {
                Timber.i("Completed")
                performanceLogger.addSplit("Completed")
                performanceLogger.dumpToLog()
                emitter.onComplete()
            }
    }

    init {
        resultsObservable.subscribe(results)
    }

    private fun onFailure(resultsEmitter: ObservableEmitter<DetectionResult>, exception: Exception) {
        Timber.e("Failed to detect text $exception")
        resultsEmitter.onNext(DetectionResult.Failed(exception = exception))
        resultsEmitter.onComplete()
    }

    private fun onSuccess(
        resultsEmitter: ObservableEmitter<DetectionResult>,
        it: FirebaseVisionText
    ) {
        val newBlocks = mutableListOf<Pair<BitmapDrawable, String>>()
        for (block in it.textBlocks) {
            for (line in block.lines) {
                val thumbnail = line.toThumbnail(bitmap)
                val thumbnailDrawable = BitmapDrawable(context.resources, thumbnail)
                val text = line.text
                newBlocks.add(Pair(thumbnailDrawable, text))
            }
        }
        Timber.e("Found results: ${newBlocks.size}")
        performanceLogger.addSplit("Processing")

        val result = if (newBlocks.size == 0)
            DetectionResult.NothingFound()
        else
            DetectionResult.Success(newBlocks)
        resultsEmitter.onNext(result)

        resultsEmitter.onComplete()
    }
}

private fun convertToFirebaseRotation(rotation: Int): Int =
    when (rotation) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        else -> FirebaseVisionImageMetadata.ROTATION_270
    }