package com.nickskelton.wifidelity.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.nickskelton.wifidelity.utils.PerformanceLogger
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.BehaviorSubject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import se.gustavkarlsson.koptional.Optional
import timber.log.Timber

class ImageProcessor(private val bitmapOptional: Optional<Bitmap>) : KoinComponent {

    val context: Context by inject()

    private val performanceLogger = PerformanceLogger("ImageProcessing")

    private val detector: FirebaseVisionTextRecognizer by lazy { FirebaseVision.getInstance().onDeviceTextRecognizer }

    val results: BehaviorSubject<DetectionResult> = BehaviorSubject.create<DetectionResult>()

    private val resultsObservable: Observable<DetectionResult> = Observable.create<DetectionResult> { emitter ->
        performanceLogger.reset()
        performanceLogger.addSplit("Started")
        emitter.onNext(DetectionResult.Loading())
        Timber.i("Loading")
        if (bitmapOptional.isAbsent) {
            emitter.onNext(DetectionResult.NothingFound())
            emitter.onComplete()
        } else {
            val image = FirebaseVisionImage.fromBitmap(bitmapOptional.valueUnsafe)
            detector.processImage(image)
                .addOnSuccessListener {
                    success(emitter, it)
                }
                .addOnFailureListener {
                    failure(emitter, it)
                }
                .addOnCompleteListener {
                    completed(emitter)
                }
        }
    }

    private fun completed(emitter: ObservableEmitter<DetectionResult>) {
        Timber.i("Completed")
        performanceLogger.addSplit("Completed")
        performanceLogger.dumpToLog()
        emitter.onComplete()
    }

    private fun failure(
        emitter: ObservableEmitter<DetectionResult>,
        it: java.lang.Exception
    ) {
        Timber.i("Failed")
        performanceLogger.addSplit("Failed")
        onFailure(emitter, it)
    }

    private fun success(
        emitter: ObservableEmitter<DetectionResult>,
        it: FirebaseVisionText
    ) {
        Timber.i("Success")
        performanceLogger.addSplit("Success")
        onSuccess(emitter, it)
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
                val thumbnail = line.toThumbnail(bitmapOptional.valueUnsafe)
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

sealed class DetectionResult {
    data class Success(val blocks: List<Pair<BitmapDrawable, String>>) : DetectionResult()
    data class Failed(val message: String = "", val exception: Exception = Exception(message)) : DetectionResult()
    class NothingFound : DetectionResult()
    class Loading : DetectionResult()
}

fun FirebaseVisionText.TextBlock.toThumbnail(sourceBitmap: Bitmap): Bitmap {
    boundingBox?.let {
        val paddedRect = it.pad(10)
        return Bitmap.createBitmap(
            sourceBitmap,
            paddedRect.left,
            paddedRect.top,
            paddedRect.right - paddedRect.left,
            paddedRect.bottom - paddedRect.top
        )
    }
    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

fun FirebaseVisionText.Line.toThumbnail(sourceBitmap: Bitmap?): Bitmap {
    val source = sourceBitmap ?: return defaultBitmap
    val box = boundingBox ?: return defaultBitmap

    val paddedRect = box.pad(10)

    val x = paddedRect.left
    val y = paddedRect.top
    val width = Math.min(source.width - x, paddedRect.right - paddedRect.left)
    val height = Math.min(source.height - y, paddedRect.bottom - paddedRect.top)

    return Bitmap.createBitmap(
        sourceBitmap,
        x,
        y,
        width,
        height
    )
}

val defaultBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

fun FirebaseVisionText.TextBlock.toText(): String {
    val builder = StringBuilder()
    for (line in lines) {
        builder.append(line.text)
        builder.append("\n")
    }
    return builder.toString()
}

fun Rect.pad(padding: Int): Rect {
    return Rect(Math.max(0, left - padding), Math.max(0, top - padding), right + padding, bottom + padding)
}