package com.nickskelton.wifidelity.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.wifi.WifiFinder
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.BehaviorSubject
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ImageProcessor(private val bitmap: Bitmap) : KoinComponent {

    val context: Context by inject()

    private val detector: FirebaseVisionTextRecognizer by lazy { FirebaseVision.getInstance().onDeviceTextRecognizer }

    val results: BehaviorSubject<DetectionResult> = BehaviorSubject.create<DetectionResult>()

    private val resultsObservable: Observable<DetectionResult> = Observable.create<DetectionResult> { emitter ->
        emitter.onNext(DetectionResult.Loading())
        Timber.i("Loading")
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.processImage(image)
            .addOnSuccessListener {
                Timber.i("Success")
                onSuccess(emitter, it)
            }
            .addOnFailureListener {
                Timber.i("Failed")
                onFailure(emitter, it)
            }
            .addOnCompleteListener {
                Timber.i("Completed")
                emitter.onComplete()
            }
    }

    init {
        resultsObservable.subscribe(results)
    }

    private fun onFailure(resultsEmitter: ObservableEmitter<DetectionResult>, exception: Exception) {
        Timber.e("Failed to detect text $exception")
        resultsEmitter.onNext(DetectionResult.Failed(exception = exception))
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