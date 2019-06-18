package com.nickskelton.wifidelity.view.video

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.common.CameraDirection
import com.nickskelton.wifidelity.extensions.addBottomMargin
import com.nickskelton.wifidelity.view.ChooseNetworkActivity
import com.nickskelton.wifidelity.view.video.frame.processor.TextProcessor
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_camera.*
import org.koin.standalone.KoinComponent
import timber.log.Timber
import java.io.IOException

class LiveCameraActivity : AppCompatActivity(), KoinComponent {
    private lateinit var rxPermissions: RxPermissions
    private var disposables = CompositeDisposable()
    private var cameraSource: CameraSource = CameraSourceNoop
    private val requiredPermissions =
        arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        rxPermissions = RxPermissions(this)
        requestPermissions()
        cameraButton.setOnApplyWindowInsetsListener { view, windowInsets ->
            val bottomInset = windowInsets.systemWindowInsetBottom
            cameraButton.addBottomMargin(bottomInset)
            windowInsets.consumeSystemWindowInsets()
        }
    }

    override fun onResume() {
        super.onResume()
        if (rxPermissions.hasCameraPermissions) startCamera()
    }

    override fun onPause() {
        super.onPause()
        if (rxPermissions.hasCameraPermissions) cameraSourcePreviewView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.release()
        disposables.dispose()
    }

    private fun requestPermissions() {
        disposables.dispose()
        disposables += rxPermissions.request(*requiredPermissions)
            .subscribe { granted -> if (!granted) finish() }
    }

    private val RxPermissions.hasCameraPermissions get() = isGranted(Manifest.permission.CAMERA)

    private fun startCamera() {
        cameraButton.setOnClickListener {
            ChooseNetworkActivity.start(
                this, ChooseNetworkActivity.Args(
                    text?.textBlocks?.flatMap { block ->
                        block.lines.map { line ->
                            line.text
                        }
                    } ?: emptyList()
                )
            )
        }

        cameraSource = CameraSourceImpl(this, graphicOverlayView)

        cameraSource.setFrameProcessor(TextProcessor(this, disposables) {
            text = it
        })

        cameraSource.requestCameraDirection(CameraDirection.BACK)
        startCameraSource()
        graphicOverlayView.onTextSelected = {
            Toast.makeText(this, "Chose: $it", Toast.LENGTH_LONG).show()
        }
    }

    private var text: FirebaseVisionText? = null

    private fun onCameraSwitched() {
        val direction =
            if (graphicOverlayView.cameraDirection == CameraDirection.BACK) CameraDirection.FRONT
            else CameraDirection.BACK

        cameraSource.requestCameraDirection(direction)
        cameraSourcePreviewView.stop()
        startCameraSource()
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() = try {
        cameraSourcePreviewView.start(cameraSource, graphicOverlayView)
    } catch (e: IOException) {
        Timber.e("Unable to start camera source. $e")
        cameraSource.release()
    }
}

