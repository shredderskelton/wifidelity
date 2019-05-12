package com.nickskelton.wifidelity.view.video.frame.processor

import android.content.Context
import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.jakewharton.rxrelay2.BehaviorRelay
import com.nickskelton.wifidelity.common.FrameMetadata
import com.nickskelton.wifidelity.video.frame.graphic.BackgroundBlurGraphic
import com.nickskelton.wifidelity.view.video.GraphicOverlayView
import com.nickskelton.wifidelity.view.video.frame.graphic.DetectedTextImageGraphic
import com.nickskelton.wifidelity.view.video.frame.graphic.HighlightedTextBoxGraphic
import com.nickskelton.wifidelity.view.video.frame.graphic.TextBoxGraphic
import com.nickskelton.wifidelity.wifi.WifiFinder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class TextProcessor(
    private val context: Context,
    private val disposable: CompositeDisposable,
    private val listener: (FirebaseVisionText) -> Unit
) : VisionProcessorBase<FirebaseVisionText>(),
    KoinComponent {

    private val wifiFinder: WifiFinder by inject()

    private val availableWifiNetworks = BehaviorRelay.createDefault(emptyList<String>())

    init {
        disposable += wifiFinder.availableNetworks
            .subscribe(availableWifiNetworks)
    }

    private val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

    override fun detectInImage(image: FirebaseVisionImage): Task<FirebaseVisionText> = detector.processImage(image)

    override fun onSuccess(
        originalCameraImage: Bitmap?,
        results: FirebaseVisionText?,
        frameMetadata: FrameMetadata,
        graphicOverlayView: GraphicOverlayView
    ) {
        if (originalCameraImage == null) return

        graphicOverlayView.clear()

        graphicOverlayView.add(BackgroundBlurGraphic(context, graphicOverlayView, originalCameraImage))

        val rectangles = results?.let { visionText ->
            visionText.textBlocks
                .flatMap { textBlock ->
                    textBlock.lines
                }
        } ?: emptyList()

        rectangles.forEach { line ->
            graphicOverlayView.add(DetectedTextImageGraphic(graphicOverlayView, line, originalCameraImage))
//            if (shouldHighlight(line.text))
//                graphicOverlayView.add(HighlightedTextBoxGraphic(graphicOverlayView, line))
//            else
//                graphicOverlayView.add(TextBoxGraphic(graphicOverlayView, line))
        }

        graphicOverlayView.postInvalidate()
        results?.let(listener)
    }

    private fun shouldHighlight(line: String): Boolean {
        val availableNets = availableWifiNetworks.value ?: emptyList()
        return availableNets
            .map { FuzzySearch.ratio(line, it) }
            .max() ?: 0 > 80
    }

    override fun onFailure(e: Exception) {
        Timber.e(e)
    }
}